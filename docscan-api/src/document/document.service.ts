import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { v4 as uuidv4 } from 'uuid';
import { Document, DocumentDoc, DocumentStatus } from './schemas/document.schema';
import { StorageService } from '../storage/storage.service';
import { UploadIntentDto } from './dto/document.dto';
import { InjectQueue } from '@nestjs/bullmq';
import { Queue } from 'bullmq';

@Injectable()
export class DocumentService {
  constructor(
    @InjectModel(Document.name) private documentModel: Model<DocumentDoc>,
    private storageService: StorageService,
    @InjectQueue('document-processing') private documentQueue: Queue,
  ) {}

  async createUploadIntent(userId: string, uploadDto: UploadIntentDto) {
    const s3Key = `users/${userId}/${uuidv4()}-${uploadDto.filename}`;

    const presignedUrl = await this.storageService.generatePresignedUrl(
      s3Key,
      uploadDto.mimeType,
    );

    const document = new this.documentModel({
      ownerId: userId,
      filename: uploadDto.filename,
      s3Key: s3Key,
      mimeType: uploadDto.mimeType,
      sizeBytes: uploadDto.sizeBytes,
      status: DocumentStatus.PENDING,
    });

    await document.save();

    return {
      documentId: document._id,
      uploadUrl: presignedUrl,
      s3Key: s3Key,
    };
  }

  async confirmUpload(userId: string, documentId: string) {
    const document = await this.documentModel.findOne({ _id: documentId, ownerId: userId });

    if (!document) {
      throw new NotFoundException('Document not found');
    }

    document.status = DocumentStatus.UPLOADED;
    await document.save();

    await this.documentQueue.add('process-document', {
      documentId: document._id,
      s3Key: document.s3Key,
      ownerId: userId,
    });

    return document;
  }

  async getUserDocuments(userId: string) {
    return this.documentModel.find({ ownerId: userId }).sort({ createdAt: -1 }).exec();
  }

  async getDocument(userId: string, documentId: string) {
    const document = await this.documentModel.findOne({ _id: documentId, ownerId: userId });
    if (!document) throw new NotFoundException('Document not found');
    return document;
  }

  async updateDocument(userId: string, documentId: string, updates: Partial<DocumentDoc>) {
    const document = await this.documentModel.findOneAndUpdate(
      { _id: documentId, ownerId: userId },
      { $set: updates },
      { new: true },
    );

    if (!document) {
      throw new NotFoundException('Document not found');
    }

    return document;
  }

  async deleteDocument(userId: string, documentId: string) {
    const document = await this.documentModel.findOne({ _id: documentId, ownerId: userId });
    if (!document) {
      throw new NotFoundException('Document not found');
    }

    await this.storageService.deleteFile(document.s3Key);
    await this.documentModel.deleteOne({ _id: documentId });

    return { success: true };
  }

  async getDownloadUrl(userId: string, documentId: string) {
    const document = await this.documentModel.findOne({ _id: documentId, ownerId: userId });
    if (!document) {
      throw new NotFoundException('Document not found');
    }

    const downloadUrl = await this.storageService.generateDownloadUrl(document.s3Key, document.filename);
    return { downloadUrl };
  }

  async getOcrBlocks(userId: string, documentId: string) {
    const document = await this.documentModel.findOne({ _id: documentId, ownerId: userId });
    if (!document) throw new NotFoundException('Document not found');

    return {
      ocrBlocks: document.ocrBlocks || [],
      originalDimensions: document.originalDimensions || { width: 0, height: 0 },
      filename: document.filename,
      mimeType: document.mimeType,
      status: document.status,
      documentCategory: document.documentCategory || 'unknown',
    };
  }

  async saveOcrBlocks(userId: string, documentId: string, blocks: any[]) {
    const document = await this.documentModel.findOneAndUpdate(
      { _id: documentId, ownerId: userId },
      { $set: { ocrBlocks: blocks } },
      { new: true },
    );

    if (!document) throw new NotFoundException('Document not found');

    return { success: true, count: blocks.length };
  }

  async reprocessDocument(userId: string, documentId: string) {
    const document = await this.documentModel.findOne({ _id: documentId, ownerId: userId });
    if (!document) throw new NotFoundException('Document not found');

    document.status = DocumentStatus.PENDING;
    document.ocrBlocks = [];
    document.originalDimensions = { width: 0, height: 0 };
    await document.save();

    await this.documentQueue.add('process-document', {
      documentId: document._id,
      s3Key: document.s3Key,
      ownerId: userId,
    });

    return { success: true, status: DocumentStatus.PENDING };
  }

  async getDocumentContent(userId: string, documentId: string) {
    const document = await this.documentModel.findOne({ _id: documentId, ownerId: userId });
    if (!document) throw new NotFoundException('Document not found');

    const buffer = await this.storageService.getFileBuffer(document.s3Key);
    if (!buffer) {
      return { type: 'error', message: 'Could not load file from storage (mock mode or S3 unavailable)' };
    }

    const filename = document.filename.toLowerCase();

    if (filename.endsWith('.txt')) {
      return { type: 'txt', text: buffer.toString('utf-8') };
    }

    if (filename.endsWith('.docx')) {
      try {
        // eslint-disable-next-line @typescript-eslint/no-var-requires
        const mammoth = require('mammoth');
        const [htmlResult, textResult] = await Promise.all([
          mammoth.convertToHtml({ buffer }),
          mammoth.extractRawText({ buffer }),
        ]);
        return { type: 'docx', html: htmlResult.value, text: textResult.value };
      } catch (e) {
        return { type: 'error', message: `DOCX conversion failed: ${e.message}` };
      }
    }

    if (filename.endsWith('.pdf')) {
      try {
        // eslint-disable-next-line @typescript-eslint/no-var-requires
        const pdfParse = require('pdf-parse');
        const data = await pdfParse(buffer);
        return { type: 'digital_pdf', text: data.text, numPages: data.numpages };
      } catch (e) {
        return { type: 'error', message: `PDF text extraction failed: ${e.message}` };
      }
    }

    return { type: 'unsupported', message: 'Content extraction not supported for this file type' };
  }
}
