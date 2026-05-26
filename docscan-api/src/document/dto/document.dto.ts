import { IsString, IsNumber, IsOptional } from 'class-validator';

export class UploadIntentDto {
  @IsString()
  filename: string;

  @IsString()
  mimeType: string;

  @IsNumber()
  sizeBytes: number;
}

export class ConfirmUploadDto {
  @IsString()
  documentId: string;
}
