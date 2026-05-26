import { 
  WebSocketGateway, 
  WebSocketServer, 
  OnGatewayConnection,
  OnGatewayDisconnect,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import * as jwt from 'jsonwebtoken';
import { Logger } from '@nestjs/common';

@WebSocketGateway({
  cors: {
    origin: '*', // In production, restrict to frontend domain
  },
})
export class NotificationsGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private logger = new Logger(NotificationsGateway.name);

  handleConnection(client: Socket) {
    try {
      // Expect token from auth handshake
      const token = client.handshake.auth?.token;
      
      if (!token) {
        this.logger.warn(`Client disconnected (no token): ${client.id}`);
        client.disconnect();
        return;
      }

      // Verify token
      const secret = process.env.JWT_SECRET || 'super_secret_enterprise_key_123!';
      const decoded = jwt.verify(token, secret) as any;
      
      const userId = decoded.sub || decoded.id;
      if (!userId) {
        throw new Error('Invalid token payload');
      }

      // Join a private room for this user
      client.join(`user_${userId}`);
      this.logger.log(`Client connected: ${client.id} joined room user_${userId}`);
    } catch (err) {
      this.logger.error(`Client connection failed: ${err.message}`);
      client.disconnect();
    }
  }

  handleDisconnect(client: Socket) {
    this.logger.log(`Client disconnected: ${client.id}`);
  }

  // Called by AiProcessor
  notifyDocumentProcessed(userId: string, documentData: any) {
    this.logger.log(`Emitting document.processed to user_${userId}`);
    this.server.to(`user_${userId}`).emit('document.processed', documentData);
  }
}
