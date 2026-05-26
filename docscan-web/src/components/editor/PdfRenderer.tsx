'use client';

import { useEffect, useRef, useState, useCallback } from 'react';

export interface PdfPageInfo {
  pageNum: number;
  displayWidth: number;
  displayHeight: number;
  naturalWidth: number;
  naturalHeight: number;
}

interface PdfRendererProps {
  url: string;
  pageNum: number;
  scale: number;
  onReady?: (info: PdfPageInfo) => void;
  onPageCount?: (count: number) => void;
  onError?: (msg: string) => void;
}

let pdfjsLibCache: any = null;

async function getPdfJs() {
  if (pdfjsLibCache) return pdfjsLibCache;
  const lib = await import('pdfjs-dist');
  lib.GlobalWorkerOptions.workerSrc =
    `https://unpkg.com/pdfjs-dist@${lib.version}/build/pdf.worker.min.mjs`;
  pdfjsLibCache = lib;
  return lib;
}

export function PdfRenderer({ url, pageNum, scale, onReady, onPageCount, onError }: PdfRendererProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const renderTaskRef = useRef<any>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [errMsg, setErrMsg] = useState('');

  const render = useCallback(async () => {
    if (!canvasRef.current) return;
    setStatus('loading');

    try {
      const pdfjsLib = await getPdfJs();
      const pdf = await pdfjsLib.getDocument({ url, cMapPacked: true }).promise;
      onPageCount?.(pdf.numPages);

      const page = await pdf.getPage(pageNum);
      const dpr = window.devicePixelRatio || 1;
      const viewport = page.getViewport({ scale: scale * dpr });

      const canvas = canvasRef.current;
      canvas.width = viewport.width;
      canvas.height = viewport.height;
      canvas.style.width = `${viewport.width / dpr}px`;
      canvas.style.height = `${viewport.height / dpr}px`;

      if (renderTaskRef.current) {
        renderTaskRef.current.cancel();
      }

      const ctx = canvas.getContext('2d')!;
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      const renderTask = page.render({ canvasContext: ctx, viewport });
      renderTaskRef.current = renderTask;

      await renderTask.promise;

      const displayW = viewport.width / dpr;
      const displayH = viewport.height / dpr;
      const naturalVp = page.getViewport({ scale: 1 });

      onReady?.({
        pageNum,
        displayWidth: displayW,
        displayHeight: displayH,
        naturalWidth: naturalVp.width,
        naturalHeight: naturalVp.height,
      });

      setStatus('ready');
    } catch (err: any) {
      if (err?.name === 'RenderingCancelledException') return;
      const msg = err?.message || 'Failed to render PDF';
      setErrMsg(msg);
      setStatus('error');
      onError?.(msg);
    }
  }, [url, pageNum, scale, onReady, onPageCount, onError]);

  useEffect(() => {
    render();
    return () => { renderTaskRef.current?.cancel(); };
  }, [render]);

  return (
    <div style={{ position: 'relative', display: 'inline-block', lineHeight: 0 }}>
      {status === 'loading' && (
        <div style={{
          position: 'absolute', inset: 0,
          minWidth: '680px', minHeight: '880px',
          display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center',
          background: '#fafafa', gap: '12px',
        }}>
          <div style={{ width: '36px', height: '36px', border: '3px solid rgba(99,102,241,0.2)', borderTopColor: '#6366f1', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
          <p style={{ fontSize: '12px', color: '#94a3b8', fontWeight: 500 }}>Rendering page {pageNum}…</p>
          <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
        </div>
      )}
      {status === 'error' && (
        <div style={{
          padding: '32px', background: '#fef2f2',
          border: '1px solid #fee2e2', borderRadius: '8px',
          color: '#ef4444', fontSize: '13px', maxWidth: '680px',
          minHeight: '200px', display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', gap: '8px',
        }}>
          <span style={{ fontSize: '32px' }}>⚠️</span>
          <strong>PDF rendering failed</strong>
          <span style={{ color: '#94a3b8', fontSize: '12px' }}>{errMsg}</span>
          <button onClick={render} style={{ marginTop: '8px', padding: '6px 16px', background: '#6366f1', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '12px', fontWeight: 600 }}>
            Retry
          </button>
        </div>
      )}
      <canvas
        ref={canvasRef}
        style={{ display: 'block', background: '#fff' }}
      />
    </div>
  );
}
