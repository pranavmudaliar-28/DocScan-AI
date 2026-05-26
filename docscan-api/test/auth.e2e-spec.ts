import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
const request = require('supertest');
import { AppModule } from './../src/app.module';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { getQueueToken } from '@nestjs/bullmq';

describe('QA Testing Suite: Auth & Security (e2e)', () => {
  let app: INestApplication;
  let mongoServer: MongoMemoryServer;

  beforeAll(async () => {
    // 1. Setup isolated in-memory database to prevent test pollution
    mongoServer = await MongoMemoryServer.create();
    const uri = mongoServer.getUri();
    process.env.MONGODB_URI = uri;

    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    })
    // Mock the document queue to prevent Redis connection requirements during test
    .overrideProvider(getQueueToken('document-queue'))
    .useValue({
      add: jest.fn(),
      process: jest.fn(),
    })
    .compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    if (app) {
      await app.close();
    }
    if (mongoServer) {
      await mongoServer.stop();
    }
  });

  describe('API-001: Guest Login Authentication', () => {
    it('POST /auth/guest should return a 200 OK and a valid JWT token', async () => {
      const response = await request(app.getHttpServer())
        .post('/auth/guest')
        .expect(200);

      expect(response.body).toHaveProperty('access_token');
      expect(response.body).toHaveProperty('user');
      expect(response.body.user).toHaveProperty('tier', 'FREE');
    });
  });

  describe('SEC-001: Protected Route Security', () => {
    it('GET /documents should return 401 Unauthorized without a token', async () => {
      const response = await request(app.getHttpServer())
        .get('/documents')
        .expect(401);

      expect(response.body.message).toEqual('Unauthorized');
    });

    it('GET /documents should return 200 OK with a valid token', async () => {
      // Setup: Generate guest token
      const authRes = await request(app.getHttpServer()).post('/auth/guest').expect(200);
      const token = authRes.body.access_token;

      // Test: Use token
      const docRes = await request(app.getHttpServer())
        .get('/documents')
        .set('Authorization', `Bearer ${token}`);

      if (docRes.status !== 200) {
        console.error('docRes.body:', docRes.body);
      }
      expect(docRes.status).toBe(200);

      expect(Array.isArray(docRes.body)).toBeTruthy();
      expect(docRes.body.length).toBe(0); // Brand new guest has 0 docs
    });
  });
});
