'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/axios';
import { toast } from 'sonner';
import { PdfRenderer } from '@/components/editor/PdfRenderer';
import {
  ArrowLeft, Download, Sparkles, ChevronLeft, ChevronRight,
  Search, X, FileText, ZoomIn, ZoomOut, Save, Check, Type, RefreshCw,
} from 'lucide-react';

export interface PdfTextEditorProps {
  documentId: string;
  filename: string;
  initialBlocks: any[];   // synthetic blocks from pdf-parse, already have text
  downloadUrl: string | null;
}

interface TextBlock {
  id: string;
  text: string;
  edited: string;
  pageNum: number;
  y: number;
}

function SLabel({ children }: { children: React.ReactNode }) {
  return <p style={{ fontSize: '10px', color: '#374151', fontWeight: 700, letterSpacing: '0.09em', textTransform: 'uppercase', marginBottom: '8px' }}>{children}</p>;
}

export function PdfTextEditor({ documentId, filename, initialBlocks, downloadUrl }: PdfTextEditorProps) {
  const router = useRouter();
  const [blocks,      setBlocks]      = useState<TextBlock[]>([]);
  const [pageCount,   setPageCount]   = useState(1);
  const [activePage,  setActivePage]  = useState(1);
  const [zoom,        setZoom]        = useState(1);
  const [selectedId,  setSelectedId]  = useState<string | null>(null);
  const [isDirty,     setIsDirty]     = useState(false);
  const [isSaving,    setIsSaving]    = useState(false);
  const [lastSaved,   setLastSaved]   = useState<Date | null>(null);
  const [showSearch,  setShowSearch]  = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [replaceWith, setReplaceWith] = useState('');
  const [rightTab,    setRightTab]    = useState<'text' | 'search'>('text');
  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Seed text blocks from prop
  useEffect(() => {
    setBlocks(initialBlocks.map(b => ({
      id: b.id || `b-${Math.random().toString(36).slice(2)}`,
      text:    b.text    || b.editedText || '',
      edited:  b.editedText || b.text || '',
      pageNum: b.pageNum ?? 1,
      y:       b.y       ?? 0,
    })));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Auto-save
  useEffect(() => {
    if (!isDirty) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(saveBlocks, 3000);
    return () => { if (saveTimerRef.current) clearTimeout(saveTimerRef.current); };
  }, [isDirty, blocks]); // eslint-disable-line react-hooks/exhaustive-deps

  const saveBlocks = useCallback(async () => {
    if (isSaving) return;
    try {
      setIsSaving(true);
      const payload = blocks.map(b => ({
        id: b.id, text: b.text, editedText: b.edited, pageNum: b.pageNum, y: b.y,
        x: 60, width: 680, height: 20, confidence: 95, fontSize: 13,
        fontFamily: 'sans-serif', bold: false, italic: false, color: '#1a1a1a',
      }));
      await api.patch(`/documents/${documentId}/ocr-blocks`, { blocks: payload });
      setIsDirty(false);
      setLastSaved(new Date());
    } catch { toast.error('Save failed'); }
    finally { setIsSaving(false); }
  }, [documentId, isSaving, blocks]);

  // Keyboard shortcuts
  useEffect(() => {
    const handler = (e: globalThis.KeyboardEvent) => {
      const ctrl = e.ctrlKey || e.metaKey;
      if (ctrl && e.key === 's') { e.preventDefault(); saveBlocks(); }
      if (ctrl && e.key === 'f') { e.preventDefault(); setShowSearch(v => !v); }
      if (ctrl && e.key === '=') { e.preventDefault(); setZoom(z => +(Math.min(4, z + 0.15).toFixed(2))); }
      if (ctrl && e.key === '-') { e.preventDefault(); setZoom(z => +(Math.max(0.25, z - 0.15).toFixed(2))); }
      if (ctrl && e.key === '0') { e.preventDefault(); setZoom(1); }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [saveBlocks]);

  const updateBlock = (id: string, edited: string) => {
    setBlocks(bs => bs.map(b => b.id === id ? { ...b, edited } : b));
    setIsDirty(true);
  };

  const handleReplaceAll = () => {
    if (!searchQuery) return;
    const q = searchQuery.toLowerCase();
    let count = 0;
    setBlocks(bs => bs.map(b => {
      if (!b.edited.toLowerCase().includes(q)) return b;
      count++;
      return { ...b, edited: b.edited.replace(new RegExp(searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi'), replaceWith) };
    }));
    setIsDirty(true);
    toast.success(`Replaced in ${count} paragraph${count !== 1 ? 's' : ''}`);
  };

  const handleExportTxt = () => {
    const text = [...blocks].sort((a, b) => a.pageNum - b.pageNum || a.y - b.y)
      .map(b => b.edited).filter(Boolean).join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename.replace(/\.pdf$/i, '')}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Exported as TXT');
  };

  const pageBlocks    = blocks.filter(b => b.pageNum === activePage);
  const selectedBlock = blocks.find(b => b.id === selectedId) ?? null;
  const editedCount   = blocks.filter(b => b.edited !== b.text).length;
  const searchMatches = searchQuery
    ? blocks.filter(b => b.edited.toLowerCase().includes(searchQuery.toLowerCase()))
    : [];

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
        <span style={{ ...S.badge, background: 'rgba(99,102,241,0.12)', border: '1px solid rgba(99,102,241,0.2)', color: '#818cf8' }}>Digital PDF</span>
        {editedCount > 0 && <span style={{ ...S.badge, background: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.2)', color: '#f59e0b' }}>{editedCount} edited</span>}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '8px' }}>
          {isSaving ? <span style={{ fontSize: '11.5px', color: '#6366f1' }}>● Saving…</span>
            : isDirty ? <span style={{ fontSize: '11.5px', color: '#f59e0b' }}>● Unsaved</span>
            : lastSaved ? <span style={{ fontSize: '11.5px', color: '#22c55e', display: 'flex', alignItems: 'center', gap: '4px' }}><Check size={11} /> Saved</span>
            : null}
          <button onClick={() => saveBlocks()} disabled={!isDirty} style={{ ...S.btnSave, ...(isDirty ? {} : S.btnSaveDisabled) }}>
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
        <span style={{ fontSize: '11px', color: '#475569' }}>Native text PDF — direct editing enabled</span>
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <button onClick={() => setShowSearch(v => !v)} title="Search (Ctrl+F)"
            style={{ ...S.toolBtn, background: showSearch ? 'rgba(99,102,241,0.18)' : undefined, color: showSearch ? '#818cf8' : undefined }}>
            <Search size={13} />
          </button>
          <div style={S.sep} />
          <button onClick={() => setZoom(z => +(Math.max(0.25, z - 0.15).toFixed(2)))} style={S.toolBtn}><ZoomOut size={13} /></button>
          <button onClick={() => setZoom(1)} style={{ ...S.toolBtn, minWidth: '42px', fontSize: '11px', fontWeight: 700 }}>{Math.round(zoom * 100)}%</button>
          <button onClick={() => setZoom(z => +(Math.min(4, z + 0.15).toFixed(2)))} style={S.toolBtn}><ZoomIn size={13} /></button>
        </div>
      </div>

      {/* SEARCH PANEL */}
      {showSearch && (
        <div style={S.searchPanel}>
          <Search size={13} color="#64748b" />
          <input placeholder="Search text…" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
            style={S.searchInput} autoFocus onKeyDown={e => { if (e.key === 'Escape') setShowSearch(false); }} />
          {searchMatches.length > 0 && <span style={{ fontSize: '11px', color: '#818cf8', whiteSpace: 'nowrap' }}>{searchMatches.length} found</span>}
          <span style={{ fontSize: '11px', color: '#475569' }}>→</span>
          <input placeholder="Replace with…" value={replaceWith} onChange={e => setReplaceWith(e.target.value)} style={S.searchInput} />
          <button onClick={handleReplaceAll} disabled={!searchQuery} style={{ ...S.toolBtn, fontSize: '11px', fontWeight: 600, whiteSpace: 'nowrap' }}>Replace all</button>
          <button onClick={() => setShowSearch(false)} style={S.toolBtn}><X size={13} /></button>
        </div>
      )}

      {/* BODY */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>

        {/* PDF Viewer */}
        <main style={{ flex: 1, background: '#12121c', overflow: 'auto', position: 'relative' }}>
          {pageCount > 1 && (
            <div style={{ position: 'sticky', top: 0, zIndex: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', padding: '6px', background: 'rgba(18,18,28,0.95)', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
              <button onClick={() => setActivePage(p => Math.max(1, p - 1))} disabled={activePage === 1} style={{ ...S.toolBtn, opacity: activePage === 1 ? 0.3 : 1 }}><ChevronLeft size={14} /></button>
              <span style={{ fontSize: '12px', color: '#94a3b8', fontWeight: 600 }}>Page {activePage} of {pageCount}</span>
              <button onClick={() => setActivePage(p => Math.min(pageCount, p + 1))} disabled={activePage === pageCount} style={{ ...S.toolBtn, opacity: activePage === pageCount ? 0.3 : 1 }}><ChevronRight size={14} /></button>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'center', padding: '40px 24px' }}>
            <div style={{ transform: `scale(${zoom})`, transformOrigin: 'top center', transition: 'transform 0.15s ease' }}>
              {downloadUrl ? (
                <div style={{ boxShadow: '0 30px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.06)', borderRadius: '3px', overflow: 'hidden' }}>
                  <PdfRenderer url={downloadUrl} pageNum={activePage} scale={1.2}
                    onPageCount={setPageCount}
                    onReady={() => {}}
                    onError={msg => toast.error(`PDF error: ${msg}`)} />
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', color: '#2d2d3a', paddingTop: '60px' }}>
                  <FileText size={52} strokeWidth={1} />
                  <p style={{ fontSize: '14px' }}>No preview available</p>
                </div>
              )}
            </div>
          </div>
        </main>

        {/* Right Sidebar — Text Editor */}
        <aside style={{ width: '320px', flexShrink: 0, background: '#0f0f18', borderLeft: '1px solid rgba(255,255,255,0.06)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <div style={S.tabRow}>
            {(['text', 'search'] as const).map(t => (
              <button key={t} onClick={() => setRightTab(t)}
                style={{ ...S.tab, ...(rightTab === t ? S.tabActive : {}) }}>
                {t === 'text' ? 'Text Editor' : 'Find & Replace'}
              </button>
            ))}
          </div>

          {rightTab === 'text' && (
            <div style={{ flex: 1, overflow: 'auto', padding: '10px 10px' }}>
              <SLabel>{pageBlocks.length} paragraphs · page {activePage}</SLabel>
              {pageBlocks.length === 0 ? (
                <div style={{ textAlign: 'center', paddingTop: '40px' }}>
                  <FileText size={32} color="#2d2d3a" strokeWidth={1} style={{ margin: '0 auto 12px' }} />
                  <p style={{ fontSize: '12px', color: '#374151' }}>No text on this page</p>
                </div>
              ) : (
                pageBlocks.map(block => (
                  <div key={block.id} style={{ marginBottom: '8px' }}>
                    <textarea
                      value={block.edited}
                      onChange={e => updateBlock(block.id, e.target.value)}
                      onClick={() => setSelectedId(block.id)}
                      rows={Math.max(2, Math.ceil(block.edited.length / 40))}
                      style={{
                        width: '100%', padding: '8px 10px', borderRadius: '6px', resize: 'vertical',
                        fontSize: '12px', lineHeight: 1.55, fontFamily: 'system-ui,-apple-system,sans-serif',
                        background: selectedId === block.id ? 'rgba(99,102,241,0.1)' : 'rgba(255,255,255,0.04)',
                        border: `1px solid ${selectedId === block.id ? 'rgba(99,102,241,0.3)' : 'rgba(255,255,255,0.08)'}`,
                        color: '#e2e8f0', outline: 'none', boxSizing: 'border-box',
                        ...(block.edited !== block.text ? { borderColor: 'rgba(245,158,11,0.3)', background: 'rgba(245,158,11,0.05)' } : {}),
                        ...(searchQuery && block.edited.toLowerCase().includes(searchQuery.toLowerCase())
                          ? { borderColor: 'rgba(251,191,36,0.5)', background: 'rgba(251,191,36,0.06)' } : {}),
                      }}
                    />
                    {block.edited !== block.text && (
                      <button onClick={() => updateBlock(block.id, block.text)}
                        style={{ fontSize: '10px', color: '#f59e0b', background: 'none', border: 'none', cursor: 'pointer', padding: '2px 0' }}>
                        ↺ Reset
                      </button>
                    )}
                  </div>
                ))
              )}
            </div>
          )}

          {rightTab === 'search' && (
            <div style={{ flex: 1, overflow: 'auto', padding: '14px 10px' }}>
              <SLabel>Find & Replace</SLabel>
              <div style={{ marginBottom: '10px' }}>
                <label style={{ fontSize: '11px', color: '#64748b', display: 'block', marginBottom: '5px' }}>Search</label>
                <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                  placeholder="Find text…" style={{ ...S.searchInput, width: '100%', display: 'block', padding: '7px 10px' }} />
              </div>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ fontSize: '11px', color: '#64748b', display: 'block', marginBottom: '5px' }}>Replace with</label>
                <input value={replaceWith} onChange={e => setReplaceWith(e.target.value)}
                  placeholder="Replacement…" style={{ ...S.searchInput, width: '100%', display: 'block', padding: '7px 10px' }} />
              </div>
              {searchMatches.length > 0 && (
                <div style={{ padding: '8px 10px', background: 'rgba(99,102,241,0.08)', border: '1px solid rgba(99,102,241,0.15)', borderRadius: '6px', marginBottom: '10px', fontSize: '11.5px', color: '#818cf8' }}>
                  {searchMatches.length} paragraph{searchMatches.length !== 1 ? 's' : ''} matched
                </div>
              )}
              <button onClick={handleReplaceAll} disabled={!searchQuery}
                style={{ ...S.btnPrimary, width: '100%', opacity: searchQuery ? 1 : 0.4 }}>
                <RefreshCw size={13} /> Replace All
              </button>
            </div>
          )}
        </aside>
      </div>

      {/* STATUS BAR */}
      <footer style={S.statusBar}>
        <span>Zoom <b style={{ color: '#4a5568' }}>{Math.round(zoom * 100)}%</b></span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span><b style={{ color: '#4a5568' }}>{blocks.length}</b> paragraphs</span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span><b style={{ color: '#4a5568' }}>{editedCount}</b> edited</span>
        <span style={{ marginLeft: 'auto' }}>
          {isSaving ? <span style={{ color: '#6366f1' }}>● Saving…</span>
            : isDirty ? <span style={{ color: '#f59e0b' }}>● Unsaved</span>
            : lastSaved ? <span style={{ color: '#22c55e' }}>✓ Saved · {lastSaved.toLocaleTimeString()}</span>
            : <span>Digital PDF — Ctrl+S save · Ctrl+F search</span>}
        </span>
      </footer>
    </div>
  );
}

const S = {
  header:   { height: '52px', flexShrink: 0 as const, background: 'rgba(10,10,18,0.98)', borderBottom: '1px solid rgba(255,255,255,0.07)', backdropFilter: 'blur(16px)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '10px', zIndex: 50 },
  toolbar:  { height: '44px', flexShrink: 0 as const, background: 'rgba(13,13,20,0.97)', borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 12px', gap: '4px', backdropFilter: 'blur(8px)', zIndex: 40 },
  searchPanel: { height: '40px', flexShrink: 0 as const, background: 'rgba(15,15,22,0.98)', borderBottom: '1px solid rgba(255,255,255,0.06)', display: 'flex', alignItems: 'center', padding: '0 12px', gap: '8px', zIndex: 35 },
  tabRow:   { display: 'flex', padding: '4px', gap: '2px', borderBottom: '1px solid rgba(255,255,255,0.06)', flexShrink: 0 as const },
  tab:      { flex: 1, padding: '6px 4px', borderRadius: '5px', border: 'none', cursor: 'pointer', fontSize: '10.5px', fontWeight: 700 as const, letterSpacing: '0.05em', textTransform: 'uppercase' as const, background: 'transparent', color: '#475569' },
  tabActive:{ background: 'rgba(99,102,241,0.18)', color: '#818cf8' },
  logoIcon: { width: '26px', height: '26px', borderRadius: '7px', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 as const },
  sep:      { width: '1px', height: '18px', background: 'rgba(255,255,255,0.08)', flexShrink: 0 as const },
  badge:    { fontSize: '10px', color: '#64748b', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '4px', padding: '2px 7px', fontWeight: 600 as const },
  filenameText: { fontSize: '14px', fontWeight: 500 as const, maxWidth: '260px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' as const },
  backBtn:  { display: 'flex', alignItems: 'center', gap: '5px', color: '#94a3b8', fontSize: '13px', fontWeight: 500 as const, padding: '6px 8px', borderRadius: '6px', border: 'none', background: 'transparent', cursor: 'pointer' },
  btnSave:  { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 12px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: '#6366f1', color: '#fff', fontSize: '12px', fontWeight: 700 as const },
  btnSaveDisabled: { background: 'rgba(255,255,255,0.05)', color: '#475569', cursor: 'default' as const },
  btnOutline: { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 10px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.08)', background: 'transparent', cursor: 'pointer', color: '#94a3b8', fontSize: '12px', fontWeight: 600 as const },
  btnPrimary: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px', padding: '8px 14px', borderRadius: '7px', border: 'none', cursor: 'pointer', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', color: '#fff', fontSize: '13px', fontWeight: 700 as const },
  toolBtn:  { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', height: '30px', minWidth: '30px', padding: '0 8px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: 'transparent', color: '#64748b', fontSize: '13px' },
  searchInput: { flex: 1, minWidth: 0, padding: '4px 8px', borderRadius: '5px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', color: '#e2e8f0', fontSize: '12px', outline: 'none' },
  statusBar:{ height: '26px', flexShrink: 0 as const, background: '#07070f', borderTop: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '12px', fontSize: '11px', color: '#2d3748' },
};
