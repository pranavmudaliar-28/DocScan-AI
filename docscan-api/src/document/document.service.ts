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
    
    // Generate pre-signed URL
    const presignedUrl = await this.storageService.generatePresignedUrl(
      s3Key,
      uploadDto.mimeType,
    );

    // Save pending document
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

    // Emit event to OCR worker queue
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
}
