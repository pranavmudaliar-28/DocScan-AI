import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useRouter } from 'next/navigation';
import SignupPage from '@/app/(auth)/signup/page';
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
const mockSetAuth = vi.fn();
const mockPush    = vi.fn();

function setupMocks() {
  vi.mocked(useAuthStore).mockImplementation((selector: any) =>
    selector({ user: null, token: null, setAuth: mockSetAuth, logout: vi.fn() })
  );
  vi.mocked(useRouter).mockReturnValue({
    push: mockPush, replace: vi.fn(), back: vi.fn(), prefetch: vi.fn(),
  } as any);
}

const VALID_USER_RESPONSE = {
  data: { user: { id: '1', email: 'new@test.com', profile: {}, tier: 'free' }, access_token: 'tok' },
};

describe('SignupPage', () => {
  beforeEach(() => {
    setupMocks();
    vi.mocked(api.post).mockResolvedValue(VALID_USER_RESPONSE);
  });

  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders the email input', () => {
    render(<SignupPage />);
    expect(screen.getByPlaceholderText('you@company.com')).toBeInTheDocument();
  });

  it('renders the password input', () => {
    render(<SignupPage />);
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
  });

  it('renders the confirm password input', () => {
    render(<SignupPage />);
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
  });

  it('renders the Create account submit button', () => {
    render(<SignupPage />);
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
  });

  it('renders the Continue as Guest button', () => {
    render(<SignupPage />);
    expect(screen.getByRole('button', { name: /continue as guest/i })).toBeInTheDocument();
  });

  it('renders a link to the login page', () => {
    render(<SignupPage />);
    // Page has two "Sign in" links (desktop top-right + mobile bottom) — just assert at least one exists
    const links = screen.getAllByRole('link', { name: /sign in/i });
    expect(links.length).toBeGreaterThanOrEqual(1);
    expect(links[0]).toHaveAttribute('href', '/login');
  });

  // ── Validation ───────────────────────────────────────────────────────────────

  it('shows error for invalid email', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'bad-email');
    await user.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => {
      expect(screen.getByText(/valid email/i)).toBeInTheDocument();
    });
  });

  it('shows error when password is less than 8 characters', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'a@b.com');
    await user.type(screen.getByLabelText(/^password$/i), 'short');
    await user.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => {
      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
    });
  });

  it('shows error when passwords do not match', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'a@b.com');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.type(screen.getByLabelText(/confirm password/i), 'different123');
    await user.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => {
      expect(screen.getByText(/passwords don't match/i)).toBeInTheDocument();
    });
  });

  // ── Password strength indicator ───────────────────────────────────────────────

  it('shows Weak strength for a short password', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByLabelText(/^password$/i), 'abc');
    expect(screen.getByText(/weak/i)).toBeInTheDocument();
  });

  it('shows Good strength for an 8-char password with a digit', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByLabelText(/^password$/i), 'password1');
    expect(screen.getByText(/good/i)).toBeInTheDocument();
  });

  it('shows Strong strength for a 12+ char password with digit and symbol', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByLabelText(/^password$/i), 'Str0ng!Pass#word');
    expect(screen.getByText(/strong/i)).toBeInTheDocument();
  });

  it('does not render strength indicator before typing', () => {
    render(<SignupPage />);
    expect(screen.queryByText(/weak|good|strong/i)).not.toBeInTheDocument();
  });

  // ── Password visibility toggles ───────────────────────────────────────────────

  it('toggles password field between text and password type', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    const passInput = screen.getByLabelText(/^password$/i);
    const toggleBtns = screen.getAllByRole('button', { name: /show password/i });
    expect(passInput).toHaveAttribute('type', 'password');
    await user.click(toggleBtns[0]);
    expect(passInput).toHaveAttribute('type', 'text');
  });

  it('toggles confirm password field independently', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    const confirmInput = screen.getByLabelText(/confirm password/i);
    const toggleBtns = screen.getAllByRole('button', { name: /show password/i });
    // Second toggle belongs to confirm password
    await user.click(toggleBtns[1]);
    expect(confirmInput).toHaveAttribute('type', 'text');
  });

  // ── Successful registration ───────────────────────────────────────────────────

  it('sends only email and password to POST /auth/register (no confirmPassword)', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'new@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.type(screen.getByLabelText(/confirm password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/auth/register', {
        email: 'new@test.com',
        password: 'password123',
      });
      // confirmPassword must NOT be sent
      const callArg = vi.mocked(api.post).mock.calls[0][1] as Record<string, unknown>;
      expect(callArg).not.toHaveProperty('confirmPassword');
    });
  });

  it('calls setAuth and redirects to / on success', async () => {
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'new@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.type(screen.getByLabelText(/confirm password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => {
      expect(mockSetAuth).toHaveBeenCalled();
      expect(mockPush).toHaveBeenCalledWith('/');
    });
  });

  // ── Failed registration ───────────────────────────────────────────────────────

  it('shows error banner when registration fails', async () => {
    vi.mocked(api.post).mockRejectedValueOnce({
      response: { data: { message: 'Email already in use' } },
    });
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'existing@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.type(screen.getByLabelText(/confirm password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => {
      expect(screen.getByText(/email already in use/i)).toBeInTheDocument();
    });
  });

  // ── Loading state ────────────────────────────────────────────────────────────

  it('shows Creating account text and disables button while submitting', async () => {
    vi.mocked(api.post).mockImplementationOnce(
      () => new Promise((resolve) => setTimeout(resolve, 500))
    );
    const user = userEvent.setup();
    render(<SignupPage />);
    await user.type(screen.getByPlaceholderText('you@company.com'), 'new@test.com');
    await user.type(screen.getByLabelText(/^password$/i), 'password123');
    await user.type(screen.getByLabelText(/confirm password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(screen.getByText(/creating account/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /creating account/i })).toBeDisabled();
  });
});
