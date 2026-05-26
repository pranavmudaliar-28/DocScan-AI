import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type UserDocument = User & Document;

export enum UserTier {
  FREE = 'FREE',
  PRO = 'PRO',
  ENTERPRISE = 'ENTERPRISE',
}

@Schema({ timestamps: true })
export class User {
  @Prop({ required: true, unique: true, index: true })
  email: string;

  @Prop({ required: true })
  passwordHash: string;

  @Prop({
    type: {
      name: { type: String },
      avatar: { type: String },
      phone: { type: String },
    },
    default: {},
  })
  profile: Record<string, any>;

  @Prop({
    type: {
      theme: { type: String, default: 'system' },
      notifications: { type: Boolean, default: true },
    },
    default: { theme: 'system', notifications: true },
  })
  preferences: Record<string, any>;

  @Prop({
    type: {
      usedBytes: { type: Number, default: 0 },
      limitBytes: { type: Number, default: 5368709120 }, // 5GB default
    },
    default: { usedBytes: 0, limitBytes: 5368709120 },
  })
  storage: Record<string, any>;

  @Prop({ type: String, enum: UserTier, default: UserTier.FREE })
  tier: UserTier;
}

export const UserSchema = SchemaFactory.createForClass(User);
