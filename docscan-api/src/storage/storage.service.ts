import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { S3Client, PutObjectCommand, GetObjectCommand, DeleteObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

@Injectable()
export class StorageService {
  private s3Client: S3Client;
  private bucketName: string;

  constructor() {
    this.bucketName = process.env.S3_BUCKET_NAME || 'docscan-local';
    
    // For local dev, we might use MinIO or mock credentials if not set
    this.s3Client = new S3Client({
      region: process.env.AWS_REGION || 'us-east-1',
      credentials: {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID || 'mock-access-key',
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY || 'mock-secret-key',
      },
      endpoint: process.env.S3_ENDPOINT, // e.g. http://localhost:9000 for MinIO
      forcePathStyle: true, // Needed for local MinIO
    });
  }

  async generatePresignedUrl(key: string, mimeType: string): Promise<string> {
    try {
      const command = new PutObjectCommand({
        Bucket: this.bucketName,
        Key: key,
        ContentType: mimeType,
      });

      // URL expires in 15 minutes
      return await getSignedUrl(this.s3Client, command, { expiresIn: 900 });
    } catch (error) {
      throw new InternalServerErrorException('Failed to generate pre-signed upload URL');
    }
  }

  async generateDownloadUrl(key: string, originalFilename?: string): Promise<string> {
    try {
      const command = new GetObjectCommand({
        Bucket: this.bucketName,
        Key: key,
        ResponseContentDisposition: originalFilename ? `attachment; filename="${originalFilename}"` : undefined
      });

      // URL expires in 15 minutes
      return await getSignedUrl(this.s3Client, command, { expiresIn: 900 });
    } catch (error) {
      throw new InternalServerErrorException('Failed to generate pre-signed download URL');
    }
  }

  async deleteFile(key: string): Promise<void> {
    try {
      const command = new DeleteObjectCommand({
        Bucket: this.bucketName,
        Key: key,
      });
      await this.s3Client.send(command);
    } catch (error) {
      console.error('Failed to delete file from S3:', error);
      // We log but don't strictly throw if S3 delete fails, as the DB record will be deleted anyway
    }
  }
}
