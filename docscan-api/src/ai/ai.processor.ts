import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Job } from 'bullmq';
import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Document, DocumentDoc, DocumentStatus } from '../document/schemas/document.schema';
import * as Tesseract from 'tesseract.js';
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
      // 1. Mark as processing
      document.status = DocumentStatus.PROCESSING;
      await document.save();

      // 2. Fetch image buffer from S3
      this.logger.log(`Downloading ${s3Key} from S3...`);
      let imageBuffer: Buffer | null = null;
      try {
        const command = new GetObjectCommand({
          Bucket: process.env.S3_BUCKET_NAME || 'docscan-local',
          Key: s3Key,
        });
        const response = await this.s3Client.send(command);
        if (response.Body) {
          imageBuffer = Buffer.from(await response.Body.transformToByteArray());
        }
      } catch (err) {
        this.logger.warn(`Could not fetch from S3 (likely mock). Using dummy OCR text.`);
        imageBuffer = null;
      }

      // 3. OCR (Tesseract) or PDF Parse
      let ocrText = "DUMMY INVOICE\nTotal: $150.00\nDate: 2023-10-25";
      if (imageBuffer) {
        if (s3Key.toLowerCase().endsWith('.pdf')) {
          this.logger.log(`Parsing PDF document ${documentId}...`);
          try {
            const pdfParse = require('pdf-parse');
            const pdfData = await pdfParse(imageBuffer);
            ocrText = pdfData.text;
          } catch (e) {
            this.logger.error(`PDF Parse failed: ${e.message}`);
          }
        } else {
          this.logger.log(`Running Tesseract OCR on document ${documentId}...`);
          const { data: { text } } = await Tesseract.recognize(imageBuffer, 'eng');
          ocrText = text;
        }
      }

      // 4. Mock LLM Extraction
      this.logger.log(`Extracting structured data via LLM...`);
      // Simulate LLM latency
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      const aiMetadata = this.mockLlmExtraction(ocrText);

      // 5. Complete
      document.aiMetadata = aiMetadata;
      document.status = DocumentStatus.COMPLETED;
      await document.save();

      // Emit Real-Time WebSocket Event
      this.notificationsGateway.notifyDocumentProcessed(ownerId, {
        documentId: document._id,
        status: DocumentStatus.COMPLETED,
        filename: document.filename,
        aiMetadata: aiMetadata
      });

      this.logger.log(`Document ${documentId} processing completed successfully.`);
      
    } catch (error) {
      this.logger.error(`Failed to process document ${documentId}`, error.stack);
      document.status = DocumentStatus.FAILED;
      await document.save();
      throw error;
    }
  }

  private mockLlmExtraction(ocrText: string) {
    // In a real implementation, we would send ocrText to OpenAI or Gemini
    // and instruct it to return JSON matching our schema.
    return {
      documentType: "Invoice",
      confidence: 0.95,
      extractedData: {
        totalAmount: 150.00,
        currency: "USD",
        date: "2023-10-25",
        vendorName: "Mock Vendor LLC"
      },
      rawOcrPreview: ocrText.substring(0, 100)
    };
  }
}
