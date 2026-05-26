import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useRouter } from 'next/navigation';
import LoginPage from '@/app/(auth)/login/page';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/lib/axios';

// ── Module mocks ──────────────────────────────────────────────────────────────
vi.mock('@/lib/axios', () => ({
  api: { post: vi.fn() },
}));

vi.mock('@/store/authStore', () => ({
  useAuthStore: vi.fn(),
}));

// ── Helpers ───────────────────────────────────────────────────────────────────
const mockSetAuth   = vi.fn();
const mockPush      = vi.fn();

function setupMocks() {
  vi.mocked(useAuthStore).mockImplementation((selector: any) =>
    selector({ user: null, token: null, setAuth: mockSetAuth, logout: vi.fn() })
  );
  vi.mocked(useRouter).mockReturnValue({
    push: mockPush, replace: vi.fn(), back: vi.fn(), prefetch: vi.fn(),
  } as any);
}

describe('LoginPage', () => {
  beforeEach(() => {
    setupMocks();
    vi.mocked(api.post).mockResolvedValue({
      data: { user: { id: '1', email: 'test@test.com', profile: {}, tier: 'free' }, access_token: 'tok' },
    });
  });

  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders the email input', () => {
    render(<LoginPage />);
    expect(screen.getByPlaceholderText('you@company.com')).toBeInTheDocument();
  });

  it('renders the password input', () => {
    render(<LoginPage />);
    const passwordInput = screen.getByLabelText(/^password$/i);
    expect(passwordInput).toBeInTheDocument();
    expect(passwordInput).toHaveAttribute('type', 'password');
  });

  it('renders a Forgot password link', () => {
    render(<LoginPage />);
    expect(screen.getByText(/forgot password/i)).toBeInTheDocument();
  });

  it('renders the Sign in button', () => {
    render(<LoginPage />);
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('renders the Continue as Guest button', () => {
    render(<LoginPage />);
    expect(screen.getByRole('button', { name: /continue as guest/i })).toBeInTheDocument();
  });

  it('renders the sign-up link', () => {
    render(<LoginPage />);
    expect(screen.getByRole('link', { name: /sign up/i })).toBeInTheDocument();
  });

  // ── Validation ───────────────────────────────────────────────────────────────

  it('shows validation error when submitted with empty email', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/valid email/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when submitted with invalid email', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'not-an-email');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/valid email/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when password is empty', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'a@b.com');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/password is required/i)).toBeInTheDocument();
    });
  });

  // ── Password visibility toggle ───────────────────────────────────────────────

  it('toggles password field to text when eye icon is clicked', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    const passwordInput = screen.getByLabelText(/^password$/i);
    expect(passwordInput).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: /show password/i }));
    expect(passwordInput).toHaveAttribute('type', 'text');
  });

  it('toggles password back to hidden when clicked again', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    const toggle = screen.getByRole('button', { name: /show password/i });
    await user.click(toggle);
    await user.click(screen.getByRole('button', { name: /hide password/i }));
    expect(screen.getByLabelText(/^password$/i)).toHaveAttribute('type', 'password');
  });

  // ── Successful login ─────────────────────────────────────────────────────────

  it('calls POST /auth/login with email and password on valid submit', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'user@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'secret123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        email: 'user@test.com',
        password: 'secret123',
      });
    });
  });

  it('calls setAuth with user and token on success', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'user@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'secret123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(mockSetAuth).toHaveBeenCalledWith(
        expect.objectContaining({ email: 'test@test.com' }),
        'tok'
      );
    });
  });

  it('redirects to / after successful login', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'user@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'secret123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/');
    });
  });

  // ── Failed login ─────────────────────────────────────────────────────────────

  it('displays error banner on API failure', async () => {
    vi.mocked(api.post).mockRejectedValueOnce({
      response: { data: { message: 'Invalid credentials' } },
    });
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'user@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'wrongpass');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
    });
  });

  it('shows a fallback error message when API provides no message', async () => {
    vi.mocked(api.post).mockRejectedValueOnce(new Error('Network Error'));
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'user@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'pass');
    await user.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/incorrect email or password/i)).toBeInTheDocument();
    });
  });

  // ── Loading state ────────────────────────────────────────────────────────────

  it('shows Signing in text and disables button while submitting', async () => {
    vi.mocked(api.post).mockImplementationOnce(
      () => new Promise((resolve) => setTimeout(resolve, 500))
    );
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'user@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'pass123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(screen.getByText(/signing in/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled();
  });

  // ── Guest login ──────────────────────────────────────────────────────────────

  it('calls POST /auth/guest when Continue as Guest is clicked', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      data: { user: { id: 'g1', email: 'guest', profile: {}, tier: 'guest' }, access_token: 'guest-tok' },
    });
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.click(screen.getByRole('button', { name: /continue as guest/i }));
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/auth/guest');
    });
  });

  it('redirects to / after guest login', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      data: { user: { id: 'g1', email: 'guest', profile: {}, tier: 'guest' }, access_token: 'guest-tok' },
    });
    const user = userEvent.setup();
    render(<LoginPage />);
    await user.click(screen.getByRole('button', { name: /continue as guest/i }));
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/');
    });
  });
});
