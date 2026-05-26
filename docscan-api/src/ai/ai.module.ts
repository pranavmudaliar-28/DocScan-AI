import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AiProcessor } from './ai.processor';
import { Document, DocumentSchema } from '../document/schemas/document.schema';

import { NotificationsModule } from '../notifications/notifications.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Document.name, schema: DocumentSchema }]),
    NotificationsModule,
  ],
  providers: [AiProcessor],
})
export class AiModule {}
