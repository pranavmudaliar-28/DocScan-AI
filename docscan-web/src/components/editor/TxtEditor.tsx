'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/axios';
import { toast } from 'sonner';
import {
  ArrowLeft, Download, Sparkles, Save, Check, Search, X,
  AlignLeft, AlignCenter, AlignRight, FileText, RefreshCw,
} from 'lucide-react';

export interface TxtEditorProps {
  documentId: string;
  filename: string;
}

type Align = 'left' | 'center' | 'right';

function SLabel({ children }: { children: React.ReactNode }) {
  return <p style={{ fontSize: '10px', color: '#374151', fontWeight: 700, letterSpacing: '0.09em', textTransform: 'uppercase', marginBottom: '8px' }}>{children}</p>;
}

export function TxtEditor({ documentId, filename }: TxtEditorProps) {
  const router = useRouter();
  const [text,       setText]       = useState('');
  const [original,   setOriginal]   = useState('');
  const [isLoading,  setIsLoading]  = useState(true);
  const [loadError,  setLoadError]  = useState<string | null>(null);
  const [isDirty,    setIsDirty]    = useState(false);
  const [isSaving,   setIsSaving]   = useState(false);
  const [lastSaved,  setLastSaved]  = useState<Date | null>(null);
  const [showSearch, setShowSearch] = useState(false);
  const [searchQuery,setSearchQuery]= useState('');
  const [replaceWith,setReplaceWith]= useState('');
  const [fontSize,   setFontSize]   = useState(14);
  const [align,      setAlign]      = useState<Align>('left');
  const [wordWrap,   setWordWrap]   = useState(true);
  const textareaRef  = useRef<HTMLTextAreaElement>(null);
  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const res = await api.get(`/documents/${documentId}/content`);
        if (res.data.type === 'txt') {
          setText(res.data.text || '');
          setOriginal(res.data.text || '');
        } else if (res.data.type === 'error') {
          setLoadError(res.data.message || 'Failed to load content');
        }
      } catch (err: any) {
        setLoadError(err.message || 'Failed to load TXT content');
        toast.error('Could not load file content');
      } finally {
        setIsLoading(false);
      }
    })();
  }, [documentId]);

  // Auto-save
  useEffect(() => {
    if (!isDirty) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(saveContent, 3000);
    return () => { if (saveTimerRef.current) clearTimeout(saveTimerRef.current); };
  }, [isDirty, text]); // eslint-disable-line react-hooks/exhaustive-deps

  const saveContent = useCallback(async () => {
    if (isSaving) return;
    try {
      setIsSaving(true);
      const lines = text.split('\n').filter(l => l.trim());
      const blocks = lines.map((line, i) => ({
        id: `txt-${i}`, text: line, editedText: line, pageNum: 1,
        x: 60, y: 80 + i * 22, width: Math.min(680, line.length * 7), height: 18,
        confidence: 99, fontSize: 13, fontFamily: 'monospace',
        bold: false, italic: false, color: '#1a1a1a',
      }));
      await api.patch(`/documents/${documentId}/ocr-blocks`, { blocks });
      setIsDirty(false);
      setLastSaved(new Date());
    } catch { toast.error('Save failed'); }
    finally { setIsSaving(false); }
  }, [documentId, isSaving, text]);

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
    const count = (text.match(regex) || []).length;
    setText(t => t.replace(regex, replaceWith));
    setIsDirty(true);
    toast.success(`Replaced ${count} occurrence${count !== 1 ? 's' : ''}`);
  };

  const handleDownload = () => {
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Downloaded');
  };

  const handleReset = () => {
    setText(original);
    setIsDirty(false);
  };

  const wordCount = text.trim().split(/\s+/).filter(Boolean).length;
  const lineCount = text.split('\n').length;
  const charCount = text.length;
  const matchCount = searchQuery ? (text.match(new RegExp(searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi')) || []).length : 0;

  if (isLoading) {
    return (
      <div style={{ position: 'fixed', inset: 0, background: '#0a0a12', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
        <div style={{ width: '44px', height: '44px', border: '3px solid rgba(99,102,241,0.2)', borderTopColor: '#6366f1', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
        <p style={{ color: '#64748b', fontSize: '14px' }}>Loading text file…</p>
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
        <span style={{ ...S.badge, background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.2)', color: '#34d399' }}>
          Plain Text
        </span>
        {isDirty && <span style={{ ...S.badge, background: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.2)', color: '#f59e0b' }}>Unsaved</span>}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '8px' }}>
          {isSaving ? <span style={{ fontSize: '11.5px', color: '#6366f1' }}>● Saving…</span>
            : lastSaved ? <span style={{ fontSize: '11.5px', color: '#22c55e', display: 'flex', alignItems: 'center', gap: '4px' }}><Check size={11} /> Saved</span>
            : null}
          <button onClick={saveContent} disabled={!isDirty} style={{ ...S.btnSave, ...(isDirty ? {} : S.btnSaveDisabled) }}>
            <Save size={12} /> Save
          </button>
          <button onClick={handleDownload} style={S.btnOutline}
            onMouseEnter={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.08)')}
            onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
            <Download size={12} /> Download
          </button>
        </div>
      </header>

      {/* TOOLBAR */}
      <div style={S.toolbar}>
        <span style={{ fontSize: '11px', color: '#475569' }}>Size</span>
        <input type="number" value={fontSize} onChange={e => setFontSize(+e.target.value)} min={10} max={28}
          style={{ width: '44px', padding: '3px 6px', borderRadius: '5px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', color: '#e2e8f0', fontSize: '12px', textAlign: 'center' }} />
        <div style={S.sep} />
        <button onClick={() => setAlign('left')}   title="Left"   style={{ ...S.toolBtn, background: align === 'left'   ? 'rgba(99,102,241,0.18)' : undefined, color: align === 'left'   ? '#818cf8' : undefined }}><AlignLeft size={13} /></button>
        <button onClick={() => setAlign('center')} title="Center" style={{ ...S.toolBtn, background: align === 'center' ? 'rgba(99,102,241,0.18)' : undefined, color: align === 'center' ? '#818cf8' : undefined }}><AlignCenter size={13} /></button>
        <button onClick={() => setAlign('right')}  title="Right"  style={{ ...S.toolBtn, background: align === 'right'  ? 'rgba(99,102,241,0.18)' : undefined, color: align === 'right'  ? '#818cf8' : undefined }}><AlignRight size={13} /></button>
        <div style={S.sep} />
        <button onClick={() => setWordWrap(v => !v)}
          style={{ ...S.toolBtn, fontSize: '11px', fontWeight: 600, background: wordWrap ? 'rgba(99,102,241,0.18)' : undefined, color: wordWrap ? '#818cf8' : undefined }}>
          Wrap
        </button>
        <div style={S.sep} />
        <button onClick={handleReset} disabled={!isDirty}
          style={{ ...S.toolBtn, fontSize: '11px', color: isDirty ? '#f59e0b' : '#374151', opacity: isDirty ? 1 : 0.4 }}>
          <RefreshCw size={12} /> Reset
        </button>
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <button onClick={() => setShowSearch(v => !v)} title="Find & Replace (Ctrl+F)"
            style={{ ...S.toolBtn, background: showSearch ? 'rgba(99,102,241,0.18)' : undefined, color: showSearch ? '#818cf8' : undefined }}>
            <Search size={13} />
          </button>
        </div>
      </div>

      {/* SEARCH PANEL */}
      {showSearch && (
        <div style={S.searchPanel}>
          <Search size={13} color="#64748b" />
          <input placeholder="Find…" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
            style={S.searchInput} autoFocus onKeyDown={e => { if (e.key === 'Escape') setShowSearch(false); }} />
          {matchCount > 0 && <span style={{ fontSize: '11px', color: '#818cf8', whiteSpace: 'nowrap' }}>{matchCount} found</span>}
          <span style={{ fontSize: '11px', color: '#475569' }}>→</span>
          <input placeholder="Replace with…" value={replaceWith} onChange={e => setReplaceWith(e.target.value)} style={S.searchInput} />
          <button onClick={handleReplaceAll} disabled={!searchQuery} style={{ ...S.toolBtn, fontSize: '11px', fontWeight: 600, whiteSpace: 'nowrap' }}>Replace all</button>
          <button onClick={() => setShowSearch(false)} style={S.toolBtn}><X size={13} /></button>
        </div>
      )}

      {/* BODY */}
      <div style={{ flex: 1, overflow: 'hidden', display: 'flex', background: '#0d0d18' }}>
        {loadError ? (
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px' }}>
            <div style={{ padding: '24px', background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: '10px', textAlign: 'center', maxWidth: '480px' }}>
              <p style={{ color: '#ef4444', fontSize: '13px', marginBottom: '8px' }}>{loadError}</p>
              <p style={{ color: '#64748b', fontSize: '11px' }}>File content requires S3 access. Mock mode returns no content.</p>
            </div>
          </div>
        ) : (
          <textarea
            ref={textareaRef}
            value={text}
            onChange={e => { setText(e.target.value); setIsDirty(true); }}
            spellCheck
            style={{
              flex: 1, padding: '32px 40px',
              fontSize: `${fontSize}px`, textAlign: align,
              fontFamily: 'ui-monospace,SFMono-Regular,Menlo,monospace',
              lineHeight: 1.75, color: '#e2e8f0',
              background: 'transparent', border: 'none', outline: 'none',
              resize: 'none', whiteSpace: wordWrap ? 'pre-wrap' : 'pre',
              overflowWrap: wordWrap ? 'break-word' : 'normal',
              boxSizing: 'border-box',
            }}
            placeholder="Start typing…"
          />
        )}
      </div>

      {/* STATUS BAR */}
      <footer style={S.statusBar}>
        <span><b style={{ color: '#4a5568' }}>{lineCount}</b> lines</span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span><b style={{ color: '#4a5568' }}>{wordCount}</b> words</span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span><b style={{ color: '#4a5568' }}>{charCount}</b> chars</span>
        {textareaRef.current && (
          <>
            <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
            <span>Pos <b style={{ color: '#4a5568' }}>{textareaRef.current.selectionStart}</b></span>
          </>
        )}
        <span style={{ marginLeft: 'auto' }}>
          {isSaving ? <span style={{ color: '#6366f1' }}>● Saving…</span>
            : isDirty ? <span style={{ color: '#f59e0b' }}>● Unsaved</span>
            : lastSaved ? <span style={{ color: '#22c55e' }}>✓ Saved · {lastSaved.toLocaleTimeString()}</span>
            : <span>Plain Text Editor — Ctrl+S save · Ctrl+F search</span>}
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
