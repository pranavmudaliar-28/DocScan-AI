import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UploadModal } from '@/components/UploadModal';
import { api } from '@/lib/axios';
import axios from 'axios';

// ── Module mocks ──────────────────────────────────────────────────────────────
vi.mock('@/lib/axios', () => ({
  api: { post: vi.fn() },
}));

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>();
  return { ...actual, default: { ...actual.default, put: vi.fn() } };
});

// ── Helpers ───────────────────────────────────────────────────────────────────
const defaultProps = {
  isOpen:           true,
  onClose:          vi.fn(),
  onUploadComplete: vi.fn(),
};

function makeFile(name = 'invoice.pdf', type = 'application/pdf', sizeBytes = 500_000) {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

async function dropFile(file: File) {
  const input = document.querySelector('input[type="file"]') as HTMLInputElement;
  if (!input) throw new Error('File input not found');
  await userEvent.upload(input, file);
}

describe('UploadModal', () => {
  beforeEach(() => {
    vi.mocked(api.post).mockResolvedValue({
      data: { uploadUrl: 'https://s3.example.com/upload', documentId: 'doc-1' },
    });
    vi.mocked(axios.put).mockResolvedValue({ status: 200 });
  });

  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders the upload modal title', () => {
    render(<UploadModal {...defaultProps} />);
    expect(screen.getByText(/upload document/i)).toBeInTheDocument();
  });

  it('renders the dropzone area when no file is selected', () => {
    render(<UploadModal {...defaultProps} />);
    expect(screen.getByText(/click to upload/i)).toBeInTheDocument();
    expect(screen.getByText(/PDF, PNG, JPG/i)).toBeInTheDocument();
  });

  it('renders the Upload File button', () => {
    render(<UploadModal {...defaultProps} />);
    expect(screen.getByRole('button', { name: /upload file/i })).toBeInTheDocument();
  });

  it('Upload File button is disabled when no file is selected', () => {
    render(<UploadModal {...defaultProps} />);
    expect(screen.getByRole('button', { name: /upload file/i })).toBeDisabled();
  });

  it('renders the Cancel button', () => {
    render(<UploadModal {...defaultProps} />);
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  it('does not render when isOpen is false', () => {
    render(<UploadModal {...defaultProps} isOpen={false} />);
    expect(screen.queryByText(/upload document/i)).not.toBeInTheDocument();
  });

  // ── File selection ────────────────────────────────────────────────────────────

  it('shows file name after a file is dropped', async () => {
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile('invoice.pdf'));
    await waitFor(() => {
      expect(screen.getByText('invoice.pdf')).toBeInTheDocument();
    });
  });

  it('shows file size after a file is dropped', async () => {
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile('invoice.pdf', 'application/pdf', 500_000));
    await waitFor(() => {
      expect(screen.getByText(/0\.48 MB/i)).toBeInTheDocument();
    });
  });

  it('enables Upload File button after a file is selected', async () => {
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile());
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /upload file/i })).not.toBeDisabled();
    });
  });

  it('allows removing the selected file with the X button', async () => {
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile('invoice.pdf'));
    await waitFor(() => screen.getByText('invoice.pdf'));

    const removeBtn = screen.getByRole('button', { name: /remove file/i });
    await user.click(removeBtn);
    await waitFor(() => {
      expect(screen.queryByText('invoice.pdf')).not.toBeInTheDocument();
    });
  });

  // ── Upload flow ───────────────────────────────────────────────────────────────

  it('calls POST /documents/upload-intent with correct payload', async () => {
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    const file = makeFile('receipt.png', 'image/png', 200_000);
    await dropFile(file);
    await waitFor(() => screen.getByRole('button', { name: /upload file/i }));
    await user.click(screen.getByRole('button', { name: /upload file/i }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/documents/upload-intent', {
        filename: 'receipt.png',
        mimeType: 'image/png',
        sizeBytes: expect.any(Number),
      });
    });
  });

  it('uploads the file to S3 using the presigned URL', async () => {
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile());
    await waitFor(() => screen.getByRole('button', { name: /upload file/i }));
    await user.click(screen.getByRole('button', { name: /upload file/i }));

    await waitFor(() => {
      expect(axios.put).toHaveBeenCalledWith(
        'https://s3.example.com/upload',
        expect.any(File),
        expect.objectContaining({ headers: { 'Content-Type': expect.any(String) } })
      );
    });
  });

  it('calls POST /documents/confirm-upload after S3 upload', async () => {
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile());
    await waitFor(() => screen.getByRole('button', { name: /upload file/i }));
    await user.click(screen.getByRole('button', { name: /upload file/i }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/documents/confirm-upload', { documentId: 'doc-1' });
    });
  });

  it('calls onUploadComplete after successful upload', async () => {
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile());
    await waitFor(() => screen.getByRole('button', { name: /upload file/i }));
    await user.click(screen.getByRole('button', { name: /upload file/i }));

    await waitFor(() => {
      expect(defaultProps.onUploadComplete).toHaveBeenCalled();
    });
  });

  it('shows Processing text on the button while uploading', async () => {
    vi.mocked(api.post).mockImplementationOnce(
      () => new Promise((resolve) => setTimeout(resolve, 500))
    );
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile());
    await waitFor(() => screen.getByRole('button', { name: /upload file/i }));
    await user.click(screen.getByRole('button', { name: /upload file/i }));

    expect(screen.getByRole('button', { name: /processing/i })).toBeDisabled();
  });

  // ── Error handling ────────────────────────────────────────────────────────────

  it('shows error message when upload-intent request fails', async () => {
    vi.mocked(api.post).mockRejectedValueOnce(new Error('Server unavailable'));
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile());
    await waitFor(() => screen.getByRole('button', { name: /upload file/i }));
    await user.click(screen.getByRole('button', { name: /upload file/i }));

    await waitFor(() => {
      expect(screen.getByText(/server unavailable/i)).toBeInTheDocument();
    });
  });

  it('shows a fallback error message when the error has no message', async () => {
    vi.mocked(api.post).mockRejectedValueOnce({});
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile());
    await waitFor(() => screen.getByRole('button', { name: /upload file/i }));
    await user.click(screen.getByRole('button', { name: /upload file/i }));

    await waitFor(() => {
      expect(screen.getByText(/upload failed/i)).toBeInTheDocument();
    });
  });

  // ── Cancel ───────────────────────────────────────────────────────────────────

  it('calls onClose when Cancel is clicked', async () => {
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await user.click(screen.getByRole('button', { name: /cancel/i }));
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  it('resets state (removes file) after Cancel', async () => {
    const user = userEvent.setup();
    render(<UploadModal {...defaultProps} />);
    await dropFile(makeFile('invoice.pdf'));
    await waitFor(() => screen.getByText('invoice.pdf'));
    await user.click(screen.getByRole('button', { name: /cancel/i }));
    // After close the modal resets; next open should show dropzone
    expect(screen.queryByText('invoice.pdf')).not.toBeInTheDocument();
  });
});
