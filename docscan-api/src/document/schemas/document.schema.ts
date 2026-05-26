import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document as MongooseDocument, Types } from 'mongoose';

export type DocumentDoc = Document & MongooseDocument;

export enum DocumentStatus {
  PENDING = 'PENDING',
  UPLOADED = 'UPLOADED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

export enum DocumentCategory {
  IMAGE         = 'image',          // Pure photo — no significant text
  SCANNED_IMAGE = 'scanned_image',  // Scanned receipt/invoice/doc as image
  SCANNED_PDF   = 'scanned_pdf',    // PDF built from scanned pages
  DIGITAL_PDF   = 'digital_pdf',    // Native text-based PDF
  DOCX          = 'docx',           // Microsoft Word document
  TXT           = 'txt',            // Plain text file
  UNKNOWN       = 'unknown',
}

@Schema({ timestamps: true })
export class Document {
  @Prop({ type: Types.ObjectId, required: true, ref: 'User', index: true })
  ownerId: Types.ObjectId;

  @Prop({ required: true })
  filename: string;

  @Prop({ required: true, unique: true })
  s3Key: string;

  @Prop()
  mimeType: string;

  @Prop()
  sizeBytes: number;

  @Prop({ type: String, enum: DocumentStatus, default: DocumentStatus.PENDING })
  status: DocumentStatus;

  @Prop({ type: String, enum: DocumentCategory, default: DocumentCategory.UNKNOWN })
  documentCategory: DocumentCategory;

  @Prop({ type: [String], default: [] })
  tags: string[];

  @Prop({ type: Object })
  aiMetadata: Record<string, any>;

  @Prop({ type: [Object], default: [] })
  ocrBlocks: Record<string, any>[];

  @Prop({ type: Object, default: {} })
  originalDimensions: { width: number; height: number };
}

export const DocumentSchema = SchemaFactory.createForClass(Document);
