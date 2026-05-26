'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/lib/axios';
import { toast } from 'sonner';
import { AlertTriangle, Sparkles } from 'lucide-react';

import { OcrEditor }     from '@/components/editor/OcrEditor';
import { ImageEditor }   from '@/components/editor/ImageEditor';
import { PdfTextEditor } from '@/components/editor/PdfTextEditor';
import { DocxEditor }    from '@/components/editor/DocxEditor';
import { TxtEditor }     from '@/components/editor/TxtEditor';

// ─── Types ─────────────────────────────────────────────────────────────────────

type DocumentCategory =
  | 'image'           // Pure photo — CSS filter editor
  | 'scanned_image'   // Image with OCR text — OCR overlay editor
  | 'scanned_pdf'     // Scanned PDF — OCR overlay editor with PDF.js
  | 'digital_pdf'     // Native text PDF — text extraction editor
  | 'docx'            // Word document — rich text / mammoth editor
  | 'txt'             // Plain text — textarea editor
  | 'unknown';        // Fallback — detect from mimeType

interface DocInfo {
  filename: string;
  mimeType: string;
  status: string;
  documentCategory: DocumentCategory;
  ocrBlocks: any[];
  originalDimensions: { width: number; height: number };
}

// ─── Editor router ──────────────────────────────────────────────────────────────

export default function EditorPage() {
  const params  = useParams();
  const router  = useRouter();
  const { token } = useAuthStore();
  const id = params.id as string;

  const [docInfo,     setDocInfo]     = useState<DocInfo | null>(null);
  const [downloadUrl, setDownloadUrl] = useState<string | null>(null);
  const [isLoading,   setIsLoading]   = useState(true);
  const [loadError,   setLoadError]   = useState<string | null>(null);

  useEffect(() => { if (!token) router.push('/login'); }, [token, router]);

  useEffect(() => {
    if (!id || !token) return;
    (async () => {
      try {
        setIsLoading(true);
        setLoadError(null);

        // Fetch routing info and download URL in parallel
        const [blocksRes, dlRes] = await Promise.allSettled([
          api.get(`/documents/${id}/ocr-blocks`),
          api.get(`/documents/${id}/download`),
        ]);

        if (blocksRes.status === 'fulfilled') {
          setDocInfo(blocksRes.value.data);
        } else {
          throw new Error('Could not load document');
        }

        if (dlRes.status === 'fulfilled') {
          setDownloadUrl(dlRes.value.data.downloadUrl);
        }
      } catch (err: any) {
        setLoadError(err.message || 'Failed to load document');
        toast.error('Failed to load document');
      } finally {
        setIsLoading(false);
      }
    })();
  }, [id, token]);

  // ── Loading ─────────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div style={{ position: 'fixed', inset: 0, background: '#0a0a12', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
        <div style={{ width: '44px', height: '44px', border: '3px solid rgba(99,102,241,0.2)', borderTopColor: '#6366f1', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
        <p style={{ color: '#64748b', fontSize: '14px', fontWeight: 500 }}>Detecting file type and loading editor…</p>
        <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
      </div>
    );
  }

  // ── Error ───────────────────────────────────────────────────────────────────
  if (loadError || !docInfo) {
    return (
      <div style={{ position: 'fixed', inset: 0, background: '#0a0a12', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '12px' }}>
        <AlertTriangle size={36} color="#ef4444" strokeWidth={1.5} />
        <p style={{ color: '#ef4444', fontSize: '14px', fontWeight: 600 }}>{loadError || 'Document not found'}</p>
        <button onClick={() => router.push('/')}
          style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', borderRadius: '7px', border: 'none', cursor: 'pointer', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', color: '#fff', fontSize: '13px', fontWeight: 700 }}>
          Back to Dashboard
        </button>
      </div>
    );
  }

  // ── Processing / pending state ───────────────────────────────────────────────
  if (docInfo.status === 'PROCESSING' || docInfo.status === 'PENDING' || docInfo.status === 'UPLOADED') {
    return (
      <div style={{ position: 'fixed', inset: 0, background: '#0a0a12', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
        <div style={{ width: '44px', height: '44px', border: '3px solid rgba(245,158,11,0.2)', borderTopColor: '#f59e0b', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
        <p style={{ color: '#f59e0b', fontSize: '14px', fontWeight: 600 }}>Document is still processing…</p>
        <p style={{ color: '#475569', fontSize: '12px' }}>This usually takes a few seconds. Come back shortly.</p>
        <button onClick={() => router.push('/')}
          style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '7px 14px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.1)', background: 'transparent', color: '#94a3b8', fontSize: '12px', cursor: 'pointer' }}>
          Back to Dashboard
        </button>
        <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
      </div>
    );
  }

  // ── Resolve category (fall back to mimeType detection if still unknown) ──────
  const fn  = docInfo.filename.toLowerCase();
  let category: DocumentCategory = docInfo.documentCategory;

  if (category === 'unknown') {
    const mime = (docInfo.mimeType || '').toLowerCase();
    if (mime === 'application/pdf' || fn.endsWith('.pdf'))  category = 'digital_pdf';
    else if (mime.startsWith('image/') || /\.(jpg|jpeg|png|webp|gif|bmp|tiff|heic)$/.test(fn)) category = 'scanned_image';
    else if (fn.endsWith('.docx')) category = 'docx';
    else if (fn.endsWith('.txt'))  category = 'txt';
    else category = 'scanned_image';
  }

  // ── Route to correct editor ───────────────────────────────────────────────────

  switch (category) {

    case 'image':
      return (
        <ImageEditor
          documentId={id}
          filename={docInfo.filename}
          mimeType={docInfo.mimeType}
          downloadUrl={downloadUrl}
        />
      );

    case 'scanned_image':
    case 'scanned_pdf':
      return (
        <OcrEditor
          documentId={id}
          filename={docInfo.filename}
          mimeType={docInfo.mimeType}
          initialBlocks={docInfo.ocrBlocks || []}
          initialDimensions={docInfo.originalDimensions || { width: 0, height: 0 }}
          isPdf={category === 'scanned_pdf'}
          downloadUrl={downloadUrl}
        />
      );

    case 'digital_pdf':
      return (
        <PdfTextEditor
          documentId={id}
          filename={docInfo.filename}
          initialBlocks={docInfo.ocrBlocks || []}
          downloadUrl={downloadUrl}
        />
      );

    case 'docx':
      return (
        <DocxEditor
          documentId={id}
          filename={docInfo.filename}
        />
      );

    case 'txt':
      return (
        <TxtEditor
          documentId={id}
          filename={docInfo.filename}
        />
      );

    default:
      return (
        <OcrEditor
          documentId={id}
          filename={docInfo.filename}
          mimeType={docInfo.mimeType}
          initialBlocks={docInfo.ocrBlocks || []}
          initialDimensions={docInfo.originalDimensions || { width: 0, height: 0 }}
          isPdf={(docInfo.mimeType === 'application/pdf') || fn.endsWith('.pdf')}
          downloadUrl={downloadUrl}
        />
      );
  }
}
