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

  @Prop({ type: [String], default: [] })
  tags: string[];

  @Prop({ type: Object })
  aiMetadata: Record<string, any>;
}

export const DocumentSchema = SchemaFactory.createForClass(Document);
