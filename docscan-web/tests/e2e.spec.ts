import { test, expect } from '@playwright/test';

test.describe('DocScan Web Application E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.clear();
    });

    // Mock Backend APIs to ensure frontend tests pass even if MongoDB isn't running
    await page.route('**/auth/guest', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: { id: 'guest-123', email: 'guest@example.com', tier: 'free' },
          access_token: 'fake-jwt-token'
        })
      });
    });

    await page.route('**/documents', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      });
    });
  });

  test('Landing Page renders correctly for unauthenticated users', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('span', { hasText: 'DocScan' }).first()).toBeVisible();
    await expect(page.locator('button', { hasText: 'Log in' })).toBeVisible();

    await expect(page.locator('h1', { hasText: /Turn unstructured documents into/i })).toBeVisible();
    await expect(page.locator('button', { hasText: 'Start Building for Free' })).toBeVisible();
    
    // Check minimal dashboard mockup
    await expect(page.locator('div', { hasText: 'DocScan AI Platform is now live' }).first()).toBeVisible();
  });

  test('Login Page renders correctly and has guest login', async ({ page }) => {
    await page.goto('/login');

    await expect(page.locator('text=Welcome back')).toBeVisible();

    await expect(page.locator('input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();

    await expect(page.locator('button', { hasText: 'Sign in' }).first()).toBeVisible();
    await expect(page.locator('button', { hasText: 'Continue as Guest' })).toBeVisible();
    
    // Desktop layout has "Create account" first, Mobile has it last
    const isMobile = page.viewportSize()?.width && page.viewportSize()!.width < 768;
    if (isMobile) {
      await expect(page.locator('a', { hasText: 'Sign up' }).last()).toBeVisible();
    } else {
      await expect(page.locator('a', { hasText: 'Create account' }).first()).toBeVisible();
    }
  });

  test('Signup Page renders correctly and has guest login', async ({ page }) => {
    await page.goto('/signup');

    await expect(page.locator('text=Create your account')).toBeVisible();

    await expect(page.locator('input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();

    await expect(page.locator('button', { hasText: 'Create account' }).first()).toBeVisible();
    await expect(page.locator('button', { hasText: 'Continue as Guest' })).toBeVisible();
    
    // Desktop layout has "Sign in" first, Mobile has it last
    const isMobile = page.viewportSize()?.width && page.viewportSize()!.width < 768;
    if (isMobile) {
      await expect(page.locator('a', { hasText: 'Sign in' }).last()).toBeVisible();
    } else {
      await expect(page.locator('a', { hasText: 'Sign in' }).first()).toBeVisible();
    }
  });

  test('Guest Login flow successfully redirects to Dashboard', async ({ page }) => {
    await page.goto('/');

    await page.locator('button', { hasText: 'Start Building for Free' }).click();

    // Wait until the 'Documents' heading appears (which indicates we are on the dashboard)
    await expect(page.locator('h2', { hasText: 'Documents' })).toBeVisible({ timeout: 10000 });

    await expect(page.locator('div', { hasText: 'Pro Tier' }).first()).toBeVisible();

    const uploadBtn = page.locator('button', { hasText: 'Upload' }).first();
    await expect(uploadBtn).toBeVisible();

    await uploadBtn.click();
    await expect(page.locator('text=Upload Document').first()).toBeVisible();
    await expect(page.locator('p', { hasText: 'Click to upload' })).toBeVisible();

    await page.locator('button', { hasText: 'Cancel' }).click();

    await page.locator('button', { hasText: 'Logout' }).click();
    
    await expect(page.locator('h1', { hasText: /Turn unstructured documents into/i })).toBeVisible();
  });

  test('FE-001: Responsive Landing Page works on mobile viewports', async ({ page }) => {
    // Set viewport to mobile size (iPhone 12/13)
    await page.setViewportSize({ width: 390, height: 844 });
    
    await page.goto('/');

    await expect(page.locator('span', { hasText: 'DocScan' }).first()).toBeVisible();
    await expect(page.locator('h1', { hasText: /Turn unstructured documents into/i })).toBeVisible();
    await expect(page.locator('button', { hasText: 'Start Building for Free' })).toBeVisible();

    // Verify layout hasn't broken by ensuring elements remain visible
    const getStartedBtn = page.locator('button', { hasText: 'Start Building for Free' });
    const boundingBox = await getStartedBtn.boundingBox();
    expect(boundingBox?.width).toBeLessThan(390); // Should fit within screen
  });

  test('ERR-001: 500 Error Handling triggers toast notification', async ({ page }) => {
    await page.route('**/documents', async (route) => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Internal Server Error' })
      });
    });

    await page.goto('/');
    
    // Trigger guest login which will subsequently call GET /documents
    await page.locator('button', { hasText: 'Start Building for Free' }).click();

    // The API error should trigger a toast notification (e.g., 'Failed to load documents')
    await expect(page.locator('text=Failed to load documents')).toBeVisible();
  });

  test('SEC-002: Unauthorized 401 Token Redirection cleanly logs user out', async ({ page }) => {
    // We override the default mock to return 401 for /documents to simulate expired token
    await page.route('**/documents', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Unauthorized' })
      });
    });

    await page.goto('/');

    // Trigger login
    await page.locator('button', { hasText: 'Start Building for Free' }).click();

    // The interceptor in axios.ts should catch the 401 and redirect to /login
    await page.waitForURL('**/login');
    
    // Verify we are back on the login page
    await expect(page.locator('text=Welcome back')).toBeVisible();
  });

});
