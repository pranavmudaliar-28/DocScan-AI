'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/axios';
import { toast } from 'sonner';
import {
  ArrowLeft, Download, Sparkles, Save, Check, Search, X,
  Bold, Italic, AlignLeft, AlignCenter, AlignRight, FileText, RefreshCw,
} from 'lucide-react';

export interface DocxEditorProps {
  documentId: string;
  filename: string;
}

type Align = 'left' | 'center' | 'right';

function SLabel({ children }: { children: React.ReactNode }) {
  return <p style={{ fontSize: '10px', color: '#374151', fontWeight: 700, letterSpacing: '0.09em', textTransform: 'uppercase', marginBottom: '8px' }}>{children}</p>;
}

export function DocxEditor({ documentId, filename }: DocxEditorProps) {
  const router = useRouter();
  const [html,       setHtml]       = useState<string>('');
  const [rawText,    setRawText]    = useState<string>('');
  const [isLoading,  setIsLoading]  = useState(true);
  const [loadError,  setLoadError]  = useState<string | null>(null);
  const [mode,       setMode]       = useState<'rendered' | 'raw'>('rendered');
  const [isDirty,    setIsDirty]    = useState(false);
  const [isSaving,   setIsSaving]   = useState(false);
  const [lastSaved,  setLastSaved]  = useState<Date | null>(null);
  const [showSearch, setShowSearch] = useState(false);
  const [searchQuery,setSearchQuery]= useState('');
  const [replaceWith,setReplaceWith]= useState('');
  const [align,      setAlign]      = useState<Align>('left');
  const [fontSize,   setFontSize]   = useState(14);
  const editorRef   = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const res = await api.get(`/documents/${documentId}/content`);
        if (res.data.type === 'docx') {
          setHtml(res.data.html || '');
          setRawText(res.data.text || '');
        } else if (res.data.type === 'error') {
          setLoadError(res.data.message || 'Failed to load document content');
        }
      } catch (err: any) {
        setLoadError(err.message || 'Failed to load DOCX content');
        toast.error('Could not load document content');
      } finally {
        setIsLoading(false);
      }
    })();
  }, [documentId]);

  // Auto-save raw text edits
  useEffect(() => {
    if (!isDirty) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(saveContent, 3000);
    return () => { if (saveTimerRef.current) clearTimeout(saveTimerRef.current); };
  }, [isDirty, rawText]); // eslint-disable-line react-hooks/exhaustive-deps

  const saveContent = useCallback(async () => {
    if (isSaving) return;
    try {
      setIsSaving(true);
      const lines = rawText.split('\n').filter(l => l.trim());
      const blocks = lines.map((line, i) => ({
        id: `docx-${i}`, text: line, editedText: line, pageNum: 1,
        x: 60, y: 80 + i * 24, width: Math.min(680, line.length * 7), height: 20,
        confidence: 99, fontSize: 13, fontFamily: 'sans-serif',
        bold: false, italic: false, color: '#1a1a1a',
      }));
      await api.patch(`/documents/${documentId}/ocr-blocks`, { blocks });
      setIsDirty(false);
      setLastSaved(new Date());
    } catch { toast.error('Save failed'); }
    finally { setIsSaving(false); }
  }, [documentId, isSaving, rawText]);

  useEffect(() => {
    const handler = (e: globalThis.KeyboardEvent) => {
      const ctrl = e.ctrlKey || e.metaKey;
      if (ctrl && e.key === 's') { e.preventDefault(); saveContent(); }
      if (ctrl && e.key === 'f') { e.preventDefault(); setShowSearch(v => !v); }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [saveContent]);

  const handleReplaceAll = () => {
    if (!searchQuery) return;
    const regex = new RegExp(searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');
    const count = (rawText.match(regex) || []).length;
    setRawText(t => t.replace(regex, replaceWith));
    setHtml(h => h.replace(regex, replaceWith));
    setIsDirty(true);
    toast.success(`Replaced ${count} occurrence${count !== 1 ? 's' : ''}`);
  };

  const handleExportTxt = () => {
    const text = mode === 'raw' ? rawText : (editorRef.current?.innerText || rawText);
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename.replace(/\.docx$/i, '')}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Exported as TXT');
  };

  const wordCount = rawText.trim().split(/\s+/).filter(Boolean).length;
  const charCount = rawText.length;

  if (isLoading) {
    return (
      <div style={{ position: 'fixed', inset: 0, background: '#0a0a12', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
        <div style={{ width: '44px', height: '44px', border: '3px solid rgba(99,102,241,0.2)', borderTopColor: '#6366f1', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
        <p style={{ color: '#64748b', fontSize: '14px' }}>Loading DOCX…</p>
        <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
      </div>
    );
  }

  return (
    <div style={{ position: 'fixed', inset: 0, display: 'flex', flexDirection: 'column', background: '#0a0a12', color: '#e2e8f0', fontFamily: 'system-ui,-apple-system,sans-serif', overflow: 'hidden' }}>
      {/* TOP BAR */}
      <header style={S.header}>
        <button onClick={() => router.push('/')} style={S.backBtn}
          onMouseEnter={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.06)')}
          onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
          <ArrowLeft size={14} /> Dashboard
        </button>
        <div style={S.sep} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}>
          <div style={S.logoIcon}><Sparkles size={13} color="#fff" /></div>
          <span style={{ fontSize: '13px', fontWeight: 800 }}>DocScan AI</span>
        </div>
        <div style={S.sep} />
        <span style={S.filenameText}>{filename}</span>
        <span style={{ ...S.badge, background: 'rgba(59,130,246,0.12)', border: '1px solid rgba(59,130,246,0.2)', color: '#60a5fa' }}>
          Word Document
        </span>
        {isDirty && <span style={{ ...S.badge, background: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.2)', color: '#f59e0b' }}>Unsaved</span>}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '8px' }}>
          {isSaving ? <span style={{ fontSize: '11.5px', color: '#6366f1' }}>● Saving…</span>
            : lastSaved ? <span style={{ fontSize: '11.5px', color: '#22c55e', display: 'flex', alignItems: 'center', gap: '4px' }}><Check size={11} /> Saved</span>
            : null}
          <button onClick={saveContent} disabled={!isDirty} style={{ ...S.btnSave, ...(isDirty ? {} : S.btnSaveDisabled) }}>
            <Save size={12} /> Save
          </button>
          <button onClick={handleExportTxt} style={S.btnOutline}
            onMouseEnter={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.08)')}
            onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
            <Download size={12} /> Export TXT
          </button>
        </div>
      </header>

      {/* TOOLBAR */}
      <div style={S.toolbar}>
        {/* View mode toggle */}
        <div style={{ display: 'flex', background: 'rgba(255,255,255,0.05)', borderRadius: '6px', padding: '2px', gap: '2px' }}>
          <button onClick={() => setMode('rendered')}
            style={{ ...S.toolBtn, fontSize: '11px', fontWeight: 600, background: mode === 'rendered' ? 'rgba(99,102,241,0.25)' : 'transparent', color: mode === 'rendered' ? '#818cf8' : '#475569' }}>
            Rendered
          </button>
          <button onClick={() => setMode('raw')}
            style={{ ...S.toolBtn, fontSize: '11px', fontWeight: 600, background: mode === 'raw' ? 'rgba(99,102,241,0.25)' : 'transparent', color: mode === 'raw' ? '#818cf8' : '#475569' }}>
            Raw Text
          </button>
        </div>
        <div style={S.sep} />
        {/* Typography controls */}
        <span style={{ fontSize: '11px', color: '#475569' }}>Size</span>
        <input type="number" value={fontSize} onChange={e => setFontSize(+e.target.value)} min={10} max={32}
          style={{ width: '44px', padding: '3px 6px', borderRadius: '5px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', color: '#e2e8f0', fontSize: '12px', textAlign: 'center' }} />
        <div style={S.sep} />
        <button onClick={() => setAlign('left')}   title="Left"   style={{ ...S.toolBtn, background: align === 'left'   ? 'rgba(99,102,241,0.18)' : undefined, color: align === 'left'   ? '#818cf8' : undefined }}><AlignLeft size={13} /></button>
        <button onClick={() => setAlign('center')} title="Center" style={{ ...S.toolBtn, background: align === 'center' ? 'rgba(99,102,241,0.18)' : undefined, color: align === 'center' ? '#818cf8' : undefined }}><AlignCenter size={13} /></button>
        <button onClick={() => setAlign('right')}  title="Right"  style={{ ...S.toolBtn, background: align === 'right'  ? 'rgba(99,102,241,0.18)' : undefined, color: align === 'right'  ? '#818cf8' : undefined }}><AlignRight size={13} /></button>
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <button onClick={() => setShowSearch(v => !v)} title="Search (Ctrl+F)"
            style={{ ...S.toolBtn, background: showSearch ? 'rgba(99,102,241,0.18)' : undefined, color: showSearch ? '#818cf8' : undefined }}>
            <Search size={13} />
          </button>
        </div>
      </div>

      {/* SEARCH PANEL */}
      {showSearch && (
        <div style={S.searchPanel}>
          <Search size={13} color="#64748b" />
          <input placeholder="Find text…" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
            style={S.searchInput} autoFocus onKeyDown={e => { if (e.key === 'Escape') setShowSearch(false); }} />
          <span style={{ fontSize: '11px', color: '#475569' }}>→</span>
          <input placeholder="Replace with…" value={replaceWith} onChange={e => setReplaceWith(e.target.value)} style={S.searchInput} />
          <button onClick={handleReplaceAll} disabled={!searchQuery} style={{ ...S.toolBtn, fontSize: '11px', fontWeight: 600, whiteSpace: 'nowrap' }}>Replace all</button>
          <button onClick={() => setShowSearch(false)} style={S.toolBtn}><X size={13} /></button>
        </div>
      )}

      {/* BODY */}
      <div style={{ flex: 1, overflow: 'auto', display: 'flex', background: '#12121c' }}>
        <div style={{ margin: '0 auto', padding: '48px 32px', width: '100%', maxWidth: '860px' }}>
          {loadError ? (
            <div style={{ padding: '24px', background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: '10px', textAlign: 'center' }}>
              <p style={{ color: '#ef4444', fontSize: '13px', marginBottom: '8px' }}>{loadError}</p>
              <p style={{ color: '#64748b', fontSize: '11px' }}>The document may require S3 access to load. In mock mode, content is unavailable.</p>
            </div>
          ) : mode === 'rendered' ? (
            <div
              ref={editorRef}
              contentEditable
              suppressContentEditableWarning
              onInput={() => {
                setRawText(editorRef.current?.innerText || '');
                setIsDirty(true);
              }}
              style={{
                outline: 'none', fontSize: `${fontSize}px`, textAlign: align,
                lineHeight: 1.8, color: '#e2e8f0', fontFamily: 'Georgia, serif',
                background: 'rgba(255,255,255,0.03)', borderRadius: '10px',
                padding: '32px 40px', border: '1px solid rgba(255,255,255,0.06)',
                minHeight: '600px',
              }}
              dangerouslySetInnerHTML={html ? { __html: html } : undefined}
            />
          ) : (
            <textarea
              ref={textareaRef}
              value={rawText}
              onChange={e => { setRawText(e.target.value); setIsDirty(true); }}
              style={{
                width: '100%', minHeight: '600px', padding: '32px 40px',
                fontSize: `${fontSize}px`, textAlign: align,
                lineHeight: 1.8, fontFamily: 'system-ui,-apple-system,sans-serif',
                background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)',
                borderRadius: '10px', color: '#e2e8f0', outline: 'none', resize: 'none',
                boxSizing: 'border-box',
              }}
            />
          )}
        </div>
      </div>

      {/* STATUS BAR */}
      <footer style={S.statusBar}>
        <span><b style={{ color: '#4a5568' }}>{wordCount}</b> words</span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span><b style={{ color: '#4a5568' }}>{charCount}</b> characters</span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span>Font <b style={{ color: '#4a5568' }}>{fontSize}px</b></span>
        <span style={{ marginLeft: 'auto' }}>
          {isSaving ? <span style={{ color: '#6366f1' }}>● Saving…</span>
            : isDirty ? <span style={{ color: '#f59e0b' }}>● Unsaved</span>
            : lastSaved ? <span style={{ color: '#22c55e' }}>✓ Saved · {lastSaved.toLocaleTimeString()}</span>
            : <span>DOCX Editor — Ctrl+S save · Ctrl+F search</span>}
        </span>
      </footer>
    </div>
  );
}

const S = {
  header:   { height: '52px', flexShrink: 0 as const, background: 'rgba(10,10,18,0.98)', borderBottom: '1px solid rgba(255,255,255,0.07)', backdropFilter: 'blur(16px)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '10px', zIndex: 50 },
  toolbar:  { height: '44px', flexShrink: 0 as const, background: 'rgba(13,13,20,0.97)', borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 12px', gap: '4px', backdropFilter: 'blur(8px)', zIndex: 40 },
  searchPanel: { height: '40px', flexShrink: 0 as const, background: 'rgba(15,15,22,0.98)', borderBottom: '1px solid rgba(255,255,255,0.06)', display: 'flex', alignItems: 'center', padding: '0 12px', gap: '8px', zIndex: 35 },
  logoIcon: { width: '26px', height: '26px', borderRadius: '7px', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 as const },
  sep:      { width: '1px', height: '18px', background: 'rgba(255,255,255,0.08)', flexShrink: 0 as const },
  badge:    { fontSize: '10px', color: '#64748b', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '4px', padding: '2px 7px', fontWeight: 600 as const },
  filenameText: { fontSize: '14px', fontWeight: 500 as const, maxWidth: '260px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' as const },
  backBtn:  { display: 'flex', alignItems: 'center', gap: '5px', color: '#94a3b8', fontSize: '13px', fontWeight: 500 as const, padding: '6px 8px', borderRadius: '6px', border: 'none', background: 'transparent', cursor: 'pointer' },
  btnSave:  { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 12px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: '#6366f1', color: '#fff', fontSize: '12px', fontWeight: 700 as const },
  btnSaveDisabled: { background: 'rgba(255,255,255,0.05)', color: '#475569', cursor: 'default' as const },
  btnOutline: { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 10px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.08)', background: 'transparent', cursor: 'pointer', color: '#94a3b8', fontSize: '12px', fontWeight: 600 as const },
  toolBtn:  { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', height: '30px', minWidth: '30px', padding: '0 8px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: 'transparent', color: '#64748b', fontSize: '13px' },
  searchInput: { flex: 1, minWidth: 0, padding: '4px 8px', borderRadius: '5px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', color: '#e2e8f0', fontSize: '12px', outline: 'none' },
  statusBar:{ height: '26px', flexShrink: 0 as const, background: '#07070f', borderTop: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '12px', fontSize: '11px', color: '#2d3748' },
};
