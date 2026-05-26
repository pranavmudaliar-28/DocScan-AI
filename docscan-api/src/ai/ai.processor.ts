import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Job } from 'bullmq';
import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Document, DocumentDoc, DocumentStatus, DocumentCategory } from '../document/schemas/document.schema';
import Tesseract from 'tesseract.js';
import { S3Client, GetObjectCommand } from '@aws-sdk/client-s3';
import { NotificationsGateway } from '../notifications/notifications.gateway';

@Processor('document-processing')
@Injectable()
export class AiProcessor extends WorkerHost {
  private readonly logger = new Logger(AiProcessor.name);
  private s3Client: S3Client;

  constructor(
    @InjectModel(Document.name) private documentModel: Model<DocumentDoc>,
    private notificationsGateway: NotificationsGateway,
  ) {
    super();
    this.s3Client = new S3Client({
      region: process.env.AWS_REGION || 'us-east-1',
      credentials: {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID || 'mock',
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY || 'mock',
      },
      endpoint: process.env.S3_ENDPOINT,
      forcePathStyle: true,
    });
  }

  async process(job: Job<any, any, string>): Promise<any> {
    const { documentId, s3Key, ownerId } = job.data;
    this.logger.log(`Starting processing for document ${documentId}`);

    const document = await this.documentModel.findById(documentId);
    if (!document) {
      this.logger.error(`Document ${documentId} not found`);
      return;
    }

    try {
      document.status = DocumentStatus.PROCESSING;
      await document.save();

      // Detect file type from extension + mimeType
      const ext = (s3Key.split('.').pop() || '').toLowerCase();
      const mime = (document.mimeType || '').toLowerCase();
      const isPdf  = ext === 'pdf'  || mime === 'application/pdf';
      const isDocx = ext === 'docx' || mime === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
      const isTxt  = ext === 'txt'  || mime === 'text/plain';
      const isImage = !isPdf && !isDocx && !isTxt;

      // Download from S3
      this.logger.log(`Downloading ${s3Key} from S3...`);
      let fileBuffer: Buffer | null = null;
      try {
        const command = new GetObjectCommand({
          Bucket: process.env.S3_BUCKET_NAME || 'docscan-local',
          Key: s3Key,
        });
        const response = await this.s3Client.send(command);
        if (response.Body) {
          fileBuffer = Buffer.from(await response.Body.transformToByteArray());
        }
      } catch (err) {
        this.logger.warn(`Could not fetch from S3 (likely mock). Using dummy OCR data.`);
      }

      let ocrText = 'DUMMY INVOICE\nVendor: Mock Corp\nTotal: $150.00\nDate: 2024-01-15';
      let ocrBlocks: any[] = [];
      let originalDimensions = { width: 0, height: 0 };
      let documentCategory: DocumentCategory = DocumentCategory.UNKNOWN;

      if (!fileBuffer) {
        // Mock mode — return realistic invoice demo data
        ocrBlocks = this.buildMockOcrBlocks();
        originalDimensions = { width: 800, height: 1100 };
        documentCategory = DocumentCategory.SCANNED_IMAGE;
      } else if (isPdf) {
        const result = await this.processPdfFull(fileBuffer);
        ocrText = result.text;
        ocrBlocks = result.blocks;
        documentCategory = result.category;
      } else if (isDocx) {
        const result = await this.processDocx(fileBuffer, documentId);
        ocrText = result.text;
        ocrBlocks = result.blocks;
        documentCategory = DocumentCategory.DOCX;
      } else if (isTxt) {
        ocrText = fileBuffer.toString('utf-8');
        ocrBlocks = this.buildBlocksFromText(ocrText);
        documentCategory = DocumentCategory.TXT;
      } else if (isImage) {
        const result = await this.processImage(fileBuffer, document.mimeType, documentId);
        ocrText = result.text;
        ocrBlocks = result.blocks;
        originalDimensions = result.dimensions;
        documentCategory = result.category;
      }

      // AI metadata extraction
      this.logger.log(`Running AI extraction on document ${documentId}...`);
      await new Promise(resolve => setTimeout(resolve, 500));
      const aiMetadata = this.mockLlmExtraction(ocrText);

      document.aiMetadata = aiMetadata;
      document.ocrBlocks = ocrBlocks;
      document.originalDimensions = originalDimensions;
      document.documentCategory = documentCategory;
      document.status = DocumentStatus.COMPLETED;
      await document.save();

      this.notificationsGateway.notifyDocumentProcessed(ownerId, {
        documentId: document._id,
        status: DocumentStatus.COMPLETED,
        filename: document.filename,
        aiMetadata,
        ocrBlockCount: ocrBlocks.length,
        documentCategory,
      });

      this.logger.log(`Document ${documentId} done. Category: ${documentCategory}. Blocks: ${ocrBlocks.length}`);
    } catch (error) {
      this.logger.error(`Failed to process document ${documentId}`, error.stack);
      document.status = DocumentStatus.FAILED;
      await document.save();
      throw error;
    }
  }

  // ─── Image processing ───────────────────────────────────────────────────────

  private async processImage(
    buffer: Buffer,
    mimeType: string,
    documentId: string,
  ): Promise<{ text: string; blocks: any[]; dimensions: { width: number; height: number }; category: DocumentCategory }> {
    this.logger.log(`Preprocessing image for ${documentId}...`);
    const processed = await this.preprocessImage(buffer);

    this.logger.log(`Running Tesseract OCR on image ${documentId}...`);
    let text = '';
    let blocks: any[] = [];
    let dimensions = { width: 0, height: 0 };

    try {
      const { data } = await Tesseract.recognize(processed, 'eng');
      text = data.text;

      const lines: any[] = (data as any).lines || [];
      blocks = lines
        .filter((l: any) => l.text?.trim().length > 0 && l.confidence > 15)
        .map((l: any, i: number) => ({
          id: `line-${i}`,
          text: l.text.trim(),
          editedText: l.text.trim(),
          x: l.bbox?.x0 ?? 0,
          y: l.bbox?.y0 ?? 0,
          width:  Math.max(1, (l.bbox?.x1 ?? 0) - (l.bbox?.x0 ?? 0)),
          height: Math.max(1, (l.bbox?.y1 ?? 0) - (l.bbox?.y0 ?? 0)),
          confidence: Math.round(l.confidence ?? 0),
          fontSize: Math.max(10, Math.round(((l.bbox?.y1 ?? 14) - (l.bbox?.y0 ?? 0)) * 0.65)),
          fontFamily: 'sans-serif',
          bold: false, italic: false,
          color: '#1a1a1a', pageNum: 1,
        }));

      const anyData = data as any;
      if (anyData.imageWidth && anyData.imageHeight) {
        dimensions = { width: anyData.imageWidth, height: anyData.imageHeight };
      } else {
        dimensions = this.getImageDimensionsFromBuffer(buffer, mimeType);
      }
    } catch (e) {
      this.logger.error(`Tesseract OCR failed: ${e.message}`);
    }

    // Classify: is this a document scan or a regular photo?
    const category = (blocks.length >= 5 || text.trim().length > 150)
      ? DocumentCategory.SCANNED_IMAGE
      : DocumentCategory.IMAGE;

    return { text, blocks, dimensions, category };
  }

  // ─── PDF processing ─────────────────────────────────────────────────────────

  private async processPdfFull(
    buffer: Buffer,
  ): Promise<{ text: string; blocks: any[]; category: DocumentCategory }> {
    let text = '';
    let blocks: any[] = [];
    let category = DocumentCategory.SCANNED_PDF;

    try {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const pdfParse = require('pdf-parse');
      const data = await pdfParse(buffer);
      text = data.text || '';

      if (text.trim().length > 100) {
        // Digital PDF — has native selectable text
        category = DocumentCategory.DIGITAL_PDF;
        blocks = this.buildBlocksFromText(text);
      } else {
        // Scanned PDF — no native text, needs OCR (server-side PDF rendering required)
        category = DocumentCategory.SCANNED_PDF;
        blocks = [];
      }
    } catch (e) {
      this.logger.error(`PDF parse failed: ${e.message}`);
    }

    return { text, blocks, category };
  }

  // ─── DOCX processing ────────────────────────────────────────────────────────

  private async processDocx(
    buffer: Buffer,
    documentId: string,
  ): Promise<{ text: string; blocks: any[] }> {
    this.logger.log(`Processing DOCX ${documentId} with Mammoth...`);
    let text = '';
    let blocks: any[] = [];

    try {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const mammoth = require('mammoth');
      const result = await mammoth.extractRawText({ buffer });
      text = result.value || '';
      blocks = this.buildBlocksFromText(text);
    } catch (e) {
      this.logger.error(`DOCX parse failed: ${e.message}`);
    }

    return { text, blocks };
  }

  // ─── Image preprocessing ────────────────────────────────────────────────────

  private async preprocessImage(buffer: Buffer): Promise<Buffer> {
    try {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const sharp = require('sharp');
      return await sharp(buffer)
        .grayscale()
        .normalize()
        .sharpen({ sigma: 1.5 })
        .toBuffer();
    } catch (err) {
      this.logger.warn(`Sharp preprocessing failed, using original: ${err?.message}`);
      return buffer;
    }
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  private getImageDimensionsFromBuffer(buffer: Buffer, mimeType: string): { width: number; height: number } {
    try {
      if (mimeType === 'image/png') {
        return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) };
      }
      if (mimeType === 'image/jpeg' || mimeType === 'image/jpg') {
        let i = 2;
        while (i < buffer.length - 8) {
          const marker = buffer.readUInt16BE(i);
          if (marker >= 0xffc0 && marker <= 0xffc3) {
            return { height: buffer.readUInt16BE(i + 5), width: buffer.readUInt16BE(i + 7) };
          }
          i += 2 + buffer.readUInt16BE(i + 2);
        }
      }
    } catch {}
    return { width: 0, height: 0 };
  }

  private buildBlocksFromText(text: string): any[] {
    return text
      .split('\n')
      .filter(line => line.trim().length > 0)
      .map((line, i) => ({
        id: `line-${i}`,
        text: line.trim(),
        editedText: line.trim(),
        x: 60,
        y: 80 + i * 24,
        width: Math.min(680, line.length * 7),
        height: 20,
        confidence: 95,
        fontSize: 13,
        fontFamily: 'sans-serif',
        bold: false, italic: false,
        color: '#1a1a1a', pageNum: 1,
      }));
  }

  private buildMockOcrBlocks(): any[] {
    const lines = [
      { text: 'INVOICE', y: 60, fontSize: 24, bold: true, confidence: 98 },
      { text: 'Invoice #: INV-2024-0042', y: 110, fontSize: 13, confidence: 96 },
      { text: 'Date: January 15, 2024', y: 135, fontSize: 13, confidence: 97 },
      { text: 'Bill To:', y: 185, fontSize: 13, bold: true, confidence: 95 },
      { text: 'Acme Corporation', y: 207, fontSize: 13, confidence: 93 },
      { text: '123 Business Avenue', y: 229, fontSize: 12, confidence: 91 },
      { text: 'New York, NY 10001', y: 251, fontSize: 12, confidence: 89 },
      { text: 'Description', y: 320, fontSize: 13, bold: true, confidence: 97 },
      { text: 'Professional Services - Q1 2024', y: 350, fontSize: 12, confidence: 88 },
      { text: 'Software License (Annual)', y: 374, fontSize: 12, confidence: 92 },
      { text: 'Support & Maintenance', y: 398, fontSize: 12, confidence: 85 },
      { text: 'Subtotal: $135.00', y: 460, fontSize: 13, confidence: 96 },
      { text: 'Tax (11.1%): $15.00', y: 484, fontSize: 12, confidence: 87 },
      { text: 'TOTAL: $150.00', y: 510, fontSize: 16, bold: true, confidence: 98 },
    ];
    return lines.map((l, i) => ({
      id: `mock-${i}`,
      text: l.text, editedText: l.text,
      x: 60, y: l.y,
      width: Math.min(680, l.text.length * (l.fontSize * 0.55)),
      height: l.fontSize + 6,
      confidence: l.confidence,
      fontSize: l.fontSize,
      fontFamily: 'sans-serif',
      bold: l.bold || false, italic: false,
      color: '#1a1a1a', pageNum: 1,
    }));
  }

  private mockLlmExtraction(ocrText: string) {
    return {
      documentType: 'Invoice',
      confidence: 0.95,
      extractedData: {
        totalAmount: 150.0,
        currency: 'USD',
        date: '2024-01-15',
        vendorName: 'Mock Vendor LLC',
      },
      rawOcrPreview: ocrText.substring(0, 200),
    };
  }
}
