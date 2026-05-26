import { Controller, Post, Get, Body, UseGuards, Request } from '@nestjs/common';
import { DocumentService } from './document.service';
import { UploadIntentDto, ConfirmUploadDto } from './dto/document.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('documents')
@UseGuards(AuthGuard('jwt'))
export class DocumentController {
  constructor(private documentService: DocumentService) {}

  @Post('upload-intent')
  createUploadIntent(@Request() req: any, @Body() uploadDto: UploadIntentDto) {
    return this.documentService.createUploadIntent(req.user.id, uploadDto);
  }

  @Post('confirm-upload')
  confirmUpload(@Request() req: any, @Body() confirmDto: ConfirmUploadDto) {
    return this.documentService.confirmUpload(req.user.id, confirmDto.documentId);
  }

  @Get()
  getUserDocuments(@Request() req: any) {
    return this.documentService.getUserDocuments(req.user.id);
  }
}
