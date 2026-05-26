import { describe, it, expect, afterEach } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import { api } from '@/lib/axios';

const mock = new MockAdapter(api);

describe('api axios instance', () => {
  afterEach(() => {
    mock.reset();
  });

  // ── baseURL ──────────────────────────────────────────────────────────────────

  it('defaults baseURL to localhost:3001 when env var is not set', () => {
    // NEXT_PUBLIC_API_URL is not set in the test environment
    expect(api.defaults.baseURL).toBe('http://localhost:3001');
  });

  // ── Auth header injection ────────────────────────────────────────────────────

  it('injects Authorization Bearer header when token is in localStorage', async () => {
    localStorage.setItem('access_token', 'jwt-test-token');
    let capturedHeaders: Record<string, string> = {};

    mock.onGet('/test').reply((config) => {
      capturedHeaders = (config.headers ?? {}) as Record<string, string>;
      return [200, {}];
    });

    await api.get('/test');
    expect(capturedHeaders['Authorization']).toBe('Bearer jwt-test-token');
  });

  it('does not add Authorization header when localStorage has no token', async () => {
    localStorage.removeItem('access_token');
    let capturedHeaders: Record<string, string> = {};

    mock.onGet('/test').reply((config) => {
      capturedHeaders = (config.headers ?? {}) as Record<string, string>;
      return [200, {}];
    });

    await api.get('/test');
    expect(capturedHeaders['Authorization']).toBeUndefined();
  });

  it('uses the token present at request time, not at import time', async () => {
    // Set token after the module was imported
    localStorage.setItem('access_token', 'late-token');
    let capturedHeader = '';

    mock.onPost('/auth/login').reply((config) => {
      capturedHeader = (config.headers?.['Authorization'] as string) ?? '';
      return [200, { access_token: 'response-token' }];
    });

    await api.post('/auth/login', { email: 'a@b.com', password: 'pass' });
    expect(capturedHeader).toBe('Bearer late-token');
  });

  it('sends the updated token after it is rotated in localStorage', async () => {
    localStorage.setItem('access_token', 'old-token');
    const headers: string[] = [];

    mock.onGet('/documents').reply((config) => {
      headers.push((config.headers?.['Authorization'] as string) ?? '');
      return [200, []];
    });

    await api.get('/documents');
    localStorage.setItem('access_token', 'new-token');
    await api.get('/documents');

    expect(headers[0]).toBe('Bearer old-token');
    expect(headers[1]).toBe('Bearer new-token');
  });

  // ── HTTP methods ─────────────────────────────────────────────────────────────

  it('performs GET requests successfully', async () => {
    mock.onGet('/documents').reply(200, [{ id: '1' }]);
    const res = await api.get('/documents');
    expect(res.status).toBe(200);
    expect(res.data).toEqual([{ id: '1' }]);
  });

  it('performs POST requests and sends the body', async () => {
    mock.onPost('/auth/register').reply(201, { user: {}, access_token: 'tok' });
    const res = await api.post('/auth/register', { email: 'x@y.com', password: 'pass123' });
    expect(res.status).toBe(201);
  });

  it('propagates 401 errors to the caller', async () => {
    mock.onGet('/documents').reply(401, { message: 'Unauthorized' });
    await expect(api.get('/documents')).rejects.toMatchObject({
      response: { status: 401 },
    });
  });

  it('propagates 500 errors to the caller', async () => {
    mock.onPost('/documents/upload-intent').reply(500, { message: 'Server error' });
    await expect(api.post('/documents/upload-intent', {})).rejects.toMatchObject({
      response: { status: 500 },
    });
  });
});
