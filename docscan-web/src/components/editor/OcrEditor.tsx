'use client';

import {
  useState, useEffect, useRef, useCallback, useLayoutEffect,
} from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/axios';
import { toast } from 'sonner';
import { PdfRenderer } from '@/components/editor/PdfRenderer';
import { useUndoRedo } from '@/hooks/useUndoRedo';
import {
  ArrowLeft, Save, Download, Eye, EyeOff, ZoomIn, ZoomOut,
  FileText, Sparkles, Brain, Bold, Italic, Type, Check,
  Layers, RotateCcw, Undo2, Redo2, Search, X, ChevronLeft,
  ChevronRight, ScanText, AlertTriangle, RefreshCw, FileSearch,
} from 'lucide-react';

// ─── Types ─────────────────────────────────────────────────────────────────────

export interface OcrBlock {
  id: string;
  text: string;
  editedText: string;
  x: number;
  y: number;
  width: number;
  height: number;
  confidence: number;
  fontSize: number;
  fontFamily: string;
  bold: boolean;
  italic: boolean;
  color: string;
  pageNum: number;
}

export interface OcrEditorProps {
  documentId: string;
  filename: string;
  mimeType: string;
  initialBlocks: any[];
  initialDimensions: { width: number; height: number };
  isPdf: boolean;
  downloadUrl: string | null;
}

type InteractionMode =
  | { type: 'idle' }
  | { type: 'selected'; id: string }
  | { type: 'editing'; id: string }
  | { type: 'dragging'; id: string; startX: number; startY: number; origX: number; origY: number };

// ─── Helpers ───────────────────────────────────────────────────────────────────

const confColor  = (c: number) => c >= 80 ? '#22c55e' : c >= 60 ? '#f59e0b' : '#ef4444';
const confBg     = (c: number) => c >= 80 ? 'rgba(34,197,94,0.08)' : c >= 60 ? 'rgba(245,158,11,0.10)' : 'rgba(239,68,68,0.10)';
const confBorder = (c: number) => c >= 80 ? 'rgba(34,197,94,0.30)' : c >= 60 ? 'rgba(245,158,11,0.35)' : 'rgba(239,68,68,0.35)';

function normalizeBlock(b: any): OcrBlock {
  return {
    id:         b.id        || `b-${Math.random().toString(36).slice(2)}`,
    text:       b.text      || '',
    editedText: b.editedText ?? b.text ?? '',
    x:          b.x         ?? 0,
    y:          b.y         ?? 0,
    width:      b.width     ?? 80,
    height:     b.height    ?? 18,
    confidence: b.confidence ?? 90,
    fontSize:   b.fontSize  ?? 13,
    fontFamily: b.fontFamily || 'sans-serif',
    bold:       b.bold      ?? false,
    italic:     b.italic    ?? false,
    color:      b.color     || '#1a1a1a',
    pageNum:    b.pageNum   ?? 1,
  };
}

// ─── Single OCR Block overlay ──────────────────────────────────────────────────

function OcrBlockOverlay({
  block, natW, natH, mode,
  onMouseDown, onClick, onDoubleClick, onSave, onCancel,
}: {
  block: OcrBlock; natW: number; natH: number; mode: InteractionMode;
  onMouseDown: (e: React.MouseEvent) => void;
  onClick: () => void; onDoubleClick: () => void;
  onSave: (text: string) => void; onCancel: () => void;
}) {
  const [draft, setDraft] = useState(block.editedText || block.text);
  const taRef = useRef<HTMLTextAreaElement>(null);
  const isSelected = mode.type === 'selected' && mode.id === block.id;
  const isEditing  = mode.type === 'editing'  && mode.id === block.id;
  const isDragging = mode.type === 'dragging' && mode.id === block.id;

  useEffect(() => { setDraft(block.editedText || block.text); }, [block.editedText, block.text]);
  useLayoutEffect(() => { if (isEditing) { taRef.current?.focus(); taRef.current?.select(); } }, [isEditing]);

  if (natW === 0 || natH === 0) return null;

  const pct = {
    left:   `${(block.x / natW) * 100}%`,
    top:    `${(block.y / natH) * 100}%`,
    width:  `${(block.width / natW) * 100}%`,
    height: `${(block.height / natH) * 100}%`,
  };

  if (isEditing) {
    return (
      <textarea
        ref={taRef}
        value={draft}
        onChange={e => setDraft(e.target.value)}
        onKeyDown={e => {
          e.stopPropagation();
          if (e.key === 'Escape') onCancel();
          if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); onSave(draft); }
        }}
        onBlur={() => onSave(draft)}
        onClick={e => e.stopPropagation()}
        style={{
          position: 'absolute', ...pct, minHeight: pct.height,
          fontSize: `${Math.max(10, block.fontSize)}px`,
          fontFamily: block.fontFamily || 'sans-serif',
          fontWeight: block.bold ? 700 : 400,
          fontStyle: block.italic ? 'italic' : 'normal',
          color: '#111', background: 'rgba(255,255,255,0.98)',
          border: '2px solid #6366f1', borderRadius: '3px',
          padding: '2px 4px', resize: 'none', outline: 'none', zIndex: 30,
          boxShadow: '0 0 0 4px rgba(99,102,241,0.25), 0 4px 16px rgba(0,0,0,0.2)',
          lineHeight: 1.25, overflow: 'hidden', boxSizing: 'border-box',
        }}
      />
    );
  }

  return (
    <div
      onMouseDown={onMouseDown}
      onClick={e => { e.stopPropagation(); onClick(); }}
      onDoubleClick={e => { e.stopPropagation(); onDoubleClick(); }}
      title={`"${block.text}" — ${block.confidence}% confidence`}
      style={{
        position: 'absolute', ...pct,
        background: isSelected ? 'rgba(99,102,241,0.18)' : confBg(block.confidence),
        border: `1px solid ${isSelected ? '#6366f1' : confBorder(block.confidence)}`,
        borderRadius: '2px',
        cursor: isDragging ? 'grabbing' : isSelected ? 'grab' : 'pointer',
        zIndex: isSelected ? 15 : 5,
        transition: isDragging ? 'none' : 'background 0.1s, border-color 0.1s',
        boxSizing: 'border-box', userSelect: 'none',
      }}
    >
      {isSelected && (
        <>
          <div style={{ position: 'absolute', top: '-20px', left: 0, fontSize: '9px', fontWeight: 700, whiteSpace: 'nowrap', background: '#6366f1', color: '#fff', padding: '1px 6px', borderRadius: '3px 3px 0 0' }}>
            drag · dbl-click to edit
          </div>
          {(['nw','ne','sw','se'] as const).map(corner => (
            <div key={corner} style={{
              position: 'absolute', width: '7px', height: '7px', background: '#6366f1', borderRadius: '1px',
              ...(corner.includes('n') ? { top: '-3px' } : { bottom: '-3px' }),
              ...(corner.includes('w') ? { left: '-3px' } : { right: '-3px' }),
            }} />
          ))}
        </>
      )}
    </div>
  );
}

// ─── OcrEditor ─────────────────────────────────────────────────────────────────

export function OcrEditor({ documentId, filename, mimeType, initialBlocks, initialDimensions, isPdf, downloadUrl }: OcrEditorProps) {
  const router = useRouter();

  const { state: blocks, push: pushBlocks, undo, redo, reset: resetHistory,
          canUndo, canRedo } = useUndoRedo<OcrBlock[]>([]);

  const [interaction,  setInteraction]  = useState<InteractionMode>({ type: 'idle' });
  const [zoom,         setZoom]         = useState(1);
  const [natSize,      setNatSize]      = useState({ w: initialDimensions.width, h: initialDimensions.height });
  const [pageCount,    setPageCount]    = useState(1);
  const [activePage,   setActivePage]   = useState(1);
  const [showOverlays, setShowOverlays] = useState(true);
  const [isSaving,     setIsSaving]     = useState(false);
  const [isDirty,      setIsDirty]      = useState(false);
  const [lastSaved,    setLastSaved]    = useState<Date | null>(null);
  const [leftTab,      setLeftTab]      = useState<'pages' | 'blocks' | 'info'>('blocks');
  const [rightTab,     setRightTab]     = useState<'props' | 'ai'>('props');
  const [showSearch,   setShowSearch]   = useState(false);
  const [searchQuery,  setSearchQuery]  = useState('');
  const [replaceWith,  setReplaceWith]  = useState('');
  const [searchResults,setSearchResults]= useState<string[]>([]);
  const [noOcrBlocks,  setNoOcrBlocks]  = useState(false);
  const [reOcrLoading, setReOcrLoading] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);
  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Seed blocks from props
  useEffect(() => {
    if (initialBlocks.length === 0) {
      setNoOcrBlocks(true);
    } else {
      resetHistory(initialBlocks.map(normalizeBlock));
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Auto-save debounce
  useEffect(() => {
    if (!isDirty) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(saveBlocks, 3000);
    return () => { if (saveTimerRef.current) clearTimeout(saveTimerRef.current); };
  }, [isDirty, blocks]); // eslint-disable-line react-hooks/exhaustive-deps

  const saveBlocks = useCallback(async () => {
    if (isSaving || !documentId) return;
    try {
      setIsSaving(true);
      await api.patch(`/documents/${documentId}/ocr-blocks`, { blocks });
      setIsDirty(false);
      setLastSaved(new Date());
    } catch { toast.error('Save failed'); }
    finally { setIsSaving(false); }
  }, [documentId, isSaving, blocks]);

  // Keyboard shortcuts
  useEffect(() => {
    const handler = (e: globalThis.KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      const ctrl = e.ctrlKey || e.metaKey;
      if (ctrl && e.key === 'z') { e.preventDefault(); undo(); }
      if (ctrl && e.key === 'y') { e.preventDefault(); redo(); }
      if (ctrl && e.key === 's') { e.preventDefault(); saveBlocks(); }
      if (ctrl && e.key === 'f') { e.preventDefault(); setShowSearch(v => !v); }
      if (e.key === 'Escape') { setInteraction({ type: 'idle' }); setShowSearch(false); }
      if (ctrl && e.key === '=') { e.preventDefault(); setZoom(z => +(Math.min(4, z + 0.15).toFixed(2))); }
      if (ctrl && e.key === '-') { e.preventDefault(); setZoom(z => +(Math.max(0.25, z - 0.15).toFixed(2))); }
      if (ctrl && e.key === '0') { e.preventDefault(); setZoom(1); }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [undo, redo, saveBlocks]);

  useEffect(() => {
    const handler = (e: WheelEvent) => {
      if (!e.ctrlKey) return;
      e.preventDefault();
      setZoom(z => +(Math.max(0.25, Math.min(4, z - e.deltaY * 0.001)).toFixed(2)));
    };
    window.addEventListener('wheel', handler, { passive: false });
    return () => window.removeEventListener('wheel', handler);
  }, []);

  // Block helpers
  const updateBlock = (id: string, patch: Partial<OcrBlock>, addHistory = true) => {
    const next = blocks.map(b => b.id === id ? { ...b, ...patch } : b);
    if (addHistory) pushBlocks(next); else resetHistory(next);
    setIsDirty(true);
  };

  const updateSelectedStyle = (patch: Partial<OcrBlock>) => {
    if (interaction.type !== 'selected' && interaction.type !== 'editing') return;
    updateBlock(interaction.id, patch);
  };

  // Drag
  const handleBlockMouseDown = useCallback((e: React.MouseEvent, block: OcrBlock) => {
    if (interaction.type === 'editing' && interaction.id === block.id) return;
    e.stopPropagation(); e.preventDefault();
    setInteraction({ type: 'dragging', id: block.id, startX: e.clientX, startY: e.clientY, origX: block.x, origY: block.y });
  }, [interaction]);

  const handleCanvasMouseMove = useCallback((e: React.MouseEvent) => {
    if (interaction.type !== 'dragging') return;
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect || natSize.w === 0) return;
    const dxOrig = ((e.clientX - interaction.startX) / rect.width)  * natSize.w;
    const dyOrig = ((e.clientY - interaction.startY) / rect.height) * natSize.h;
    const block = blocks.find(b => b.id === interaction.id);
    if (!block) return;
    const newX = Math.max(0, Math.min(natSize.w - block.width,  interaction.origX + dxOrig));
    const newY = Math.max(0, Math.min(natSize.h - block.height, interaction.origY + dyOrig));
    const next = blocks.map(b => b.id === interaction.id ? { ...b, x: newX, y: newY } : b);
    resetHistory(next);
    setIsDirty(true);
  }, [interaction, blocks, natSize, resetHistory]);

  const handleCanvasMouseUp = useCallback((e: React.MouseEvent) => {
    if (interaction.type !== 'dragging') return;
    e.stopPropagation();
    const wasDragged = Math.abs(e.clientX - interaction.startX) > 3 || Math.abs(e.clientY - interaction.startY) > 3;
    if (wasDragged) { pushBlocks(blocks); }
    setInteraction({ type: 'selected', id: interaction.id });
  }, [interaction, blocks, pushBlocks]);

  // Search/replace
  useEffect(() => {
    if (!searchQuery) { setSearchResults([]); return; }
    const q = searchQuery.toLowerCase();
    setSearchResults(blocks.filter(b => (b.editedText || b.text).toLowerCase().includes(q)).map(b => b.id));
  }, [searchQuery, blocks]);

  const handleReplaceAll = () => {
    if (!searchQuery) return;
    const q = searchQuery.toLowerCase();
    const matchCount = blocks.filter(b => (b.editedText || b.text).toLowerCase().includes(q)).length;
    const next = blocks.map(b => {
      const t = b.editedText || b.text;
      if (!t.toLowerCase().includes(q)) return b;
      return { ...b, editedText: t.replace(new RegExp(searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi'), replaceWith) };
    });
    pushBlocks(next);
    setIsDirty(true);
    toast.success(`Replaced in ${matchCount} block${matchCount !== 1 ? 's' : ''}`);
  };

  const handleRerunOcr = async () => {
    setReOcrLoading(true);
    try {
      await api.post(`/documents/${documentId}/reprocess`);
      toast.success('Re-processing started. Refresh in a moment.');
    } catch { toast.error('Re-process request failed'); }
    finally { setReOcrLoading(false); }
  };

  const handleExportTxt = () => {
    const text = [...blocks]
      .sort((a, b) => a.pageNum - b.pageNum || a.y - b.y || a.x - b.x)
      .map(b => b.editedText || b.text).filter(Boolean).join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename.replace(/\.[^.]+$/, '') || 'document'}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Exported as TXT');
  };

  // Derived
  const pageBlocks    = blocks.filter(b => b.pageNum === activePage || !isPdf);
  const selectedId    = (interaction.type === 'selected' || interaction.type === 'editing') ? interaction.id : null;
  const selectedBlock = blocks.find(b => b.id === selectedId) ?? null;
  const avgConf       = blocks.length ? Math.round(blocks.reduce((s, b) => s + b.confidence, 0) / blocks.length) : 0;
  const editedCount   = blocks.filter(b => b.editedText !== b.text).length;

  return (
    <div
      style={{ position: 'fixed', inset: 0, display: 'flex', flexDirection: 'column', background: '#0a0a12', color: '#e2e8f0', fontFamily: 'system-ui,-apple-system,sans-serif', overflow: 'hidden' }}
      onClick={() => { if (interaction.type !== 'editing') setInteraction({ type: 'idle' }); }}
    >
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
        <span style={{ ...S.badge, background: 'rgba(99,102,241,0.12)', border: '1px solid rgba(99,102,241,0.2)', color: '#818cf8' }}>
          {isPdf ? 'Scanned PDF' : 'Scanned Image'} · OCR
        </span>
        {editedCount > 0 && <span style={{ ...S.badge, background: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.2)', color: '#f59e0b' }}>{editedCount} edited</span>}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '8px' }}>
          {isSaving ? <span style={{ fontSize: '11.5px', color: '#6366f1' }}>● Saving…</span>
            : isDirty  ? <span style={{ fontSize: '11.5px', color: '#f59e0b' }}>● Unsaved</span>
            : lastSaved ? <span style={{ fontSize: '11.5px', color: '#22c55e', display: 'flex', alignItems: 'center', gap: '4px' }}><Check size={11} /> Saved</span>
            : null}
          <button onClick={e => { e.stopPropagation(); saveBlocks(); }} disabled={!isDirty} style={{ ...S.btnSave, ...(isDirty ? {} : S.btnSaveDisabled) }}>
            <Save size={12} /> Save
          </button>
          <button onClick={e => { e.stopPropagation(); handleExportTxt(); }} style={S.btnOutline}
            onMouseEnter={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.08)')}
            onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
            <Download size={12} /> Export TXT
          </button>
        </div>
      </header>

      {/* TOOLBAR */}
      <div style={S.toolbar} onClick={e => e.stopPropagation()}>
        <button onClick={() => undo()} disabled={!canUndo} style={{ ...S.toolBtn, opacity: canUndo ? 1 : 0.3 }} title="Undo (Ctrl+Z)"><Undo2 size={14} /></button>
        <button onClick={() => redo()} disabled={!canRedo} style={{ ...S.toolBtn, opacity: canRedo ? 1 : 0.3 }} title="Redo (Ctrl+Y)"><Redo2 size={14} /></button>
        <div style={S.sep} />
        {selectedBlock ? (
          <>
            <button onClick={() => updateSelectedStyle({ bold: !selectedBlock.bold })} title="Bold"
              style={{ ...S.toolBtn, background: selectedBlock.bold ? 'rgba(99,102,241,0.22)' : undefined, color: selectedBlock.bold ? '#818cf8' : undefined }}>
              <Bold size={13} />
            </button>
            <button onClick={() => updateSelectedStyle({ italic: !selectedBlock.italic })} title="Italic"
              style={{ ...S.toolBtn, background: selectedBlock.italic ? 'rgba(99,102,241,0.22)' : undefined, color: selectedBlock.italic ? '#818cf8' : undefined }}>
              <Italic size={13} />
            </button>
            <div style={S.sep} />
            <span style={{ fontSize: '11px', color: '#475569', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <Type size={11} />
              <input type="number" value={selectedBlock.fontSize}
                onChange={e => updateSelectedStyle({ fontSize: +e.target.value })}
                style={S.sizeInput} min={6} max={96} onClick={e => e.stopPropagation()} />
              px
            </span>
            <div style={S.sep} />
            <button onClick={() => updateSelectedStyle({ editedText: selectedBlock.text })} style={S.toolBtn} title="Reset to OCR"><RotateCcw size={13} /></button>
            <button onClick={() => setInteraction({ type: 'editing', id: selectedBlock.id })}
              style={{ ...S.toolBtn, background: 'rgba(99,102,241,0.18)', color: '#818cf8' }} title="Edit text">
              <Type size={13} /><span style={{ fontSize: '11px', fontWeight: 600 }}>Edit</span>
            </button>
          </>
        ) : (
          <span style={{ fontSize: '11px', color: '#374151', fontStyle: 'italic' }}>Select a block to format</span>
        )}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <button onClick={() => setShowSearch(v => !v)} title="Search (Ctrl+F)"
            style={{ ...S.toolBtn, background: showSearch ? 'rgba(99,102,241,0.18)' : undefined, color: showSearch ? '#818cf8' : undefined }}>
            <Search size={13} />
          </button>
          <button onClick={() => setShowOverlays(v => !v)} title="Toggle overlays"
            style={{ ...S.toolBtn, background: showOverlays ? 'rgba(99,102,241,0.18)' : undefined, color: showOverlays ? '#818cf8' : undefined }}>
            {showOverlays ? <Eye size={13} /> : <EyeOff size={13} />}
          </button>
          <div style={S.sep} />
          <button onClick={() => setZoom(z => +(Math.max(0.25, z - 0.15).toFixed(2)))} style={S.toolBtn} title="Zoom out"><ZoomOut size={13} /></button>
          <button onClick={() => setZoom(1)} style={{ ...S.toolBtn, minWidth: '42px', fontSize: '11px', fontWeight: 700 }}>{Math.round(zoom * 100)}%</button>
          <button onClick={() => setZoom(z => +(Math.min(4, z + 0.15).toFixed(2)))} style={S.toolBtn} title="Zoom in"><ZoomIn size={13} /></button>
        </div>
      </div>

      {/* SEARCH PANEL */}
      {showSearch && (
        <div style={S.searchPanel} onClick={e => e.stopPropagation()}>
          <Search size={13} color="#64748b" />
          <input placeholder="Search OCR text…" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
            style={S.searchInput} autoFocus onKeyDown={e => { if (e.key === 'Escape') setShowSearch(false); }} />
          {searchResults.length > 0 && <span style={{ fontSize: '11px', color: '#818cf8', whiteSpace: 'nowrap' }}>{searchResults.length} found</span>}
          <span style={{ fontSize: '11px', color: '#475569' }}>→</span>
          <input placeholder="Replace with…" value={replaceWith} onChange={e => setReplaceWith(e.target.value)} style={S.searchInput} />
          <button onClick={handleReplaceAll} disabled={!searchQuery} style={{ ...S.toolBtn, fontSize: '11px', fontWeight: 600, whiteSpace: 'nowrap' }}>Replace all</button>
          <button onClick={() => setShowSearch(false)} style={S.toolBtn}><X size={13} /></button>
        </div>
      )}

      {/* BODY */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>

        {/* Left Sidebar */}
        <aside style={S.sidebar}>
          <div style={S.tabRow}>
            {(['pages', 'blocks', 'info'] as const).map(t => (
              <button key={t} onClick={e => { e.stopPropagation(); setLeftTab(t); }}
                style={{ ...S.tab, ...(leftTab === t ? S.tabActive : {}) }}>
                {t === 'pages' ? 'Pages' : t === 'blocks' ? 'OCR Blocks' : 'Info'}
              </button>
            ))}
          </div>

          {leftTab === 'pages' && (
            <div style={S.panelBody}>
              <SLabel>{pageCount} page{pageCount !== 1 ? 's' : ''}</SLabel>
              {Array.from({ length: pageCount }, (_, i) => i + 1).map(p => (
                <button key={p} onClick={e => { e.stopPropagation(); setActivePage(p); }}
                  style={{ ...S.pageThumb, ...(activePage === p ? S.pageThumbActive : {}) }}>
                  <div style={{ fontSize: '10px', fontWeight: 700 }}>Page {p}</div>
                  <div style={{ fontSize: '9px', color: activePage === p ? '#818cf8' : '#374151' }}>
                    {blocks.filter(b => b.pageNum === p).length} blocks
                  </div>
                </button>
              ))}
            </div>
          )}

          {leftTab === 'blocks' && (
            <div style={S.panelBody}>
              <SLabel>{pageBlocks.length} blocks · page {activePage}</SLabel>
              {noOcrBlocks ? (
                <div style={{ padding: '16px 8px', textAlign: 'center' }}>
                  <FileSearch size={24} color="#374151" strokeWidth={1} style={{ margin: '0 auto 8px' }} />
                  <p style={{ fontSize: '12px', color: '#374151', lineHeight: 1.5 }}>No OCR data yet</p>
                  <button onClick={e => { e.stopPropagation(); handleRerunOcr(); }} disabled={reOcrLoading}
                    style={{ ...S.btnPrimary, marginTop: '10px', fontSize: '11px' }}>
                    {reOcrLoading ? <><RefreshCw size={11} /> Processing…</> : <><ScanText size={11} /> Run OCR</>}
                  </button>
                </div>
              ) : (
                [...pageBlocks].sort((a, b) => a.y - b.y || a.x - b.x).map(block => (
                  <div key={block.id}
                    onClick={e => { e.stopPropagation(); setInteraction({ type: 'selected', id: block.id }); }}
                    style={{ ...S.blockItem, ...(selectedId === block.id ? S.blockItemActive : {}) }}
                    onMouseEnter={e => { if (selectedId !== block.id) e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; }}
                    onMouseLeave={e => { if (selectedId !== block.id) e.currentTarget.style.background = 'transparent'; }}>
                    <div style={{ fontSize: '11.5px', color: '#cbd5e1', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: '3px' }}>
                      {block.editedText || block.text || '(empty)'}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <div style={{ width: '6px', height: '6px', borderRadius: '50%', background: confColor(block.confidence) }} />
                      <span style={{ fontSize: '10px', color: confColor(block.confidence), fontWeight: 700 }}>{block.confidence}%</span>
                      {searchResults.includes(block.id) && <span style={{ fontSize: '9px', color: '#f59e0b', fontWeight: 800 }}>◆ MATCH</span>}
                      {block.editedText !== block.text && <span style={{ fontSize: '9px', color: '#6366f1', fontWeight: 800, marginLeft: 'auto' }}>✎ EDITED</span>}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {leftTab === 'info' && (
            <div style={S.panelBody}>
              <SLabel>File</SLabel>
              <p style={{ fontSize: '12.5px', color: '#e2e8f0', fontWeight: 500, marginBottom: '3px', wordBreak: 'break-all' }}>{filename}</p>
              <p style={{ fontSize: '11px', color: '#475569', marginBottom: '14px' }}>{mimeType}</p>
              <Divider />
              <SLabel>OCR Summary</SLabel>
              {[
                ['Total blocks', blocks.length, '#94a3b8'],
                ['High conf ≥80%', blocks.filter(b => b.confidence >= 80).length, '#22c55e'],
                ['Med conf 60–79%', blocks.filter(b => b.confidence >= 60 && b.confidence < 80).length, '#f59e0b'],
                ['Low conf <60%', blocks.filter(b => b.confidence < 60).length, '#ef4444'],
                ['Edited', editedCount, '#818cf8'],
              ].map(([label, value, color]) => (
                <div key={label as string} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                  <span style={{ fontSize: '11.5px', color: '#475569' }}>{label as string}</span>
                  <span style={{ fontSize: '12px', fontWeight: 700, color: color as string }}>{value as number}</span>
                </div>
              ))}
            </div>
          )}
        </aside>

        {/* Canvas */}
        <main
          style={{ flex: 1, background: '#12121c', overflow: 'auto', position: 'relative' }}
          onMouseMove={handleCanvasMouseMove}
          onMouseUp={handleCanvasMouseUp}
          onClick={() => { if (interaction.type !== 'editing') setInteraction({ type: 'idle' }); }}
        >
          {isPdf && pageCount > 1 && (
            <div style={{ position: 'sticky', top: 0, zIndex: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', padding: '6px', background: 'rgba(18,18,28,0.95)', borderBottom: '1px solid rgba(255,255,255,0.05)' }}
              onClick={e => e.stopPropagation()}>
              <button onClick={() => setActivePage(p => Math.max(1, p - 1))} disabled={activePage === 1} style={{ ...S.toolBtn, opacity: activePage === 1 ? 0.3 : 1 }}><ChevronLeft size={14} /></button>
              <span style={{ fontSize: '12px', color: '#94a3b8', fontWeight: 600 }}>Page {activePage} of {pageCount}</span>
              <button onClick={() => setActivePage(p => Math.min(pageCount, p + 1))} disabled={activePage === pageCount} style={{ ...S.toolBtn, opacity: activePage === pageCount ? 0.3 : 1 }}><ChevronRight size={14} /></button>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'center', padding: '48px 32px', minHeight: '100%' }}>
            <div style={{ transform: `scale(${zoom})`, transformOrigin: 'top center', transition: 'transform 0.15s ease' }}>
              {!downloadUrl ? (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', color: '#2d2d3a', paddingTop: '60px' }}>
                  <FileText size={52} strokeWidth={1} />
                  <p style={{ fontSize: '14px' }}>No preview available</p>
                </div>
              ) : isPdf ? (
                <div style={{ position: 'relative', display: 'inline-block', boxShadow: S.docShadow, borderRadius: '3px', overflow: 'hidden' }} ref={containerRef} onClick={e => e.stopPropagation()}>
                  <PdfRenderer url={downloadUrl} pageNum={activePage} scale={1.2}
                    onPageCount={setPageCount}
                    onReady={info => setNatSize({ w: info.naturalWidth, h: info.naturalHeight })}
                    onError={msg => toast.error(`PDF error: ${msg}`)} />
                  {showOverlays && natSize.w > 0 && (
                    <div style={{ position: 'absolute', inset: 0, pointerEvents: 'auto' }}>
                      {pageBlocks.map(block => (
                        <OcrBlockOverlay key={block.id} block={block} natW={natSize.w} natH={natSize.h} mode={interaction}
                          onMouseDown={e => handleBlockMouseDown(e, block)}
                          onClick={() => setInteraction({ type: 'selected', id: block.id })}
                          onDoubleClick={() => setInteraction({ type: 'editing', id: block.id })}
                          onSave={text => { updateBlock(block.id, { editedText: text }); setInteraction({ type: 'selected', id: block.id }); }}
                          onCancel={() => setInteraction({ type: 'selected', id: block.id })}
                        />
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <div style={{ position: 'relative', display: 'inline-block', boxShadow: S.docShadow, borderRadius: '3px', overflow: 'hidden' }} ref={containerRef} onClick={e => e.stopPropagation()}>
                  <img src={downloadUrl} alt={filename} draggable={false}
                    onLoad={e => { const img = e.currentTarget; setNatSize({ w: img.naturalWidth, h: img.naturalHeight }); }}
                    style={{ display: 'block', maxWidth: '860px', width: '100%', height: 'auto', userSelect: 'none', pointerEvents: 'none' }}
                  />
                  {showOverlays && natSize.w > 0 && (
                    <div style={{ position: 'absolute', inset: 0, pointerEvents: 'auto' }}>
                      {pageBlocks.map(block => (
                        <OcrBlockOverlay key={block.id} block={block} natW={natSize.w} natH={natSize.h} mode={interaction}
                          onMouseDown={e => handleBlockMouseDown(e, block)}
                          onClick={() => setInteraction({ type: 'selected', id: block.id })}
                          onDoubleClick={() => setInteraction({ type: 'editing', id: block.id })}
                          onSave={text => { updateBlock(block.id, { editedText: text }); setInteraction({ type: 'selected', id: block.id }); }}
                          onCancel={() => setInteraction({ type: 'selected', id: block.id })}
                        />
                      ))}
                      {showOverlays && blocks.length === 0 && natSize.w > 0 && (
                        <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'none' }}>
                          <div style={{ background: 'rgba(10,10,18,0.75)', backdropFilter: 'blur(8px)', borderRadius: '10px', padding: '20px 28px', textAlign: 'center', border: '1px solid rgba(255,255,255,0.08)' }}>
                            <ScanText size={28} color="#475569" strokeWidth={1.5} style={{ margin: '0 auto 8px' }} />
                            <p style={{ fontSize: '13px', color: '#94a3b8', fontWeight: 500 }}>No OCR data found</p>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </main>

        {/* Right Sidebar */}
        <aside style={S.sidebar}>
          <div style={S.tabRow}>
            {(['props', 'ai'] as const).map(t => (
              <button key={t} onClick={e => { e.stopPropagation(); setRightTab(t); }}
                style={{ ...S.tab, ...(rightTab === t ? S.tabActive : {}) }}>
                {t === 'props' ? 'Properties' : 'AI Insights'}
              </button>
            ))}
          </div>
          <div style={S.panelBody}>
            {rightTab === 'props' ? (
              selectedBlock ? (
                <>
                  <SLabel>Selected Block</SLabel>
                  <div style={S.blockPreview}>
                    <p style={{ fontSize: '12px', color: '#e2e8f0', lineHeight: 1.4, marginBottom: '5px', wordBreak: 'break-word' }}>{selectedBlock.editedText || selectedBlock.text}</p>
                    <p style={{ fontSize: '10px', color: '#475569' }}>({Math.round(selectedBlock.x)}, {Math.round(selectedBlock.y)}) · {Math.round(selectedBlock.width)}×{Math.round(selectedBlock.height)} px</p>
                    {selectedBlock.editedText !== selectedBlock.text && (
                      <div style={{ marginTop: '6px', padding: '4px 7px', background: 'rgba(99,102,241,0.08)', borderRadius: '4px', fontSize: '10px', color: '#818cf8' }}>
                        Original: "{selectedBlock.text}"
                      </div>
                    )}
                  </div>
                  <SLabel>OCR Confidence</SLabel>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '9px', marginBottom: '14px' }}>
                    <div style={{ flex: 1, height: '6px', background: 'rgba(255,255,255,0.07)', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${selectedBlock.confidence}%`, background: confColor(selectedBlock.confidence), borderRadius: '3px', transition: 'width 0.3s' }} />
                    </div>
                    <span style={{ fontSize: '12px', fontWeight: 700, color: confColor(selectedBlock.confidence) }}>{selectedBlock.confidence}%</span>
                  </div>
                  {selectedBlock.confidence < 60 && (
                    <div style={{ padding: '7px 9px', background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.18)', borderRadius: '5px', marginBottom: '14px' }}>
                      <p style={{ fontSize: '11px', color: '#ef4444' }}>⚠ Low confidence — verify manually</p>
                    </div>
                  )}
                  <Divider />
                  <SLabel>Typography</SLabel>
                  <div style={{ display: 'flex', gap: '4px', marginBottom: '10px' }}>
                    <button onClick={e => { e.stopPropagation(); updateSelectedStyle({ bold: !selectedBlock.bold }); }} title="Bold"
                      style={{ ...S.stylBtn, ...(selectedBlock.bold ? S.stylBtnActive : {}) }}><Bold size={13} /></button>
                    <button onClick={e => { e.stopPropagation(); updateSelectedStyle({ italic: !selectedBlock.italic }); }} title="Italic"
                      style={{ ...S.stylBtn, ...(selectedBlock.italic ? S.stylBtnActive : {}) }}><Italic size={13} /></button>
                  </div>
                  <SLabel>Font Size</SLabel>
                  <input type="number" value={selectedBlock.fontSize}
                    onChange={e => updateSelectedStyle({ fontSize: +e.target.value })}
                    style={{ ...S.sizeInput, width: '70px', marginBottom: '16px' }}
                    min={6} max={96} onClick={e => e.stopPropagation()} />
                  <Divider />
                  <button onClick={e => { e.stopPropagation(); setInteraction({ type: 'editing', id: selectedBlock.id }); }} style={S.editBtn}>
                    <Type size={14} /> Edit Text
                  </button>
                  <button onClick={e => { e.stopPropagation(); updateBlock(selectedBlock.id, { editedText: selectedBlock.text }); }}
                    style={{ ...S.btnOutline, width: '100%', justifyContent: 'center', marginTop: '6px', fontSize: '12px' }}>
                    <RotateCcw size={12} /> Reset to OCR
                  </button>
                </>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '200px', gap: '10px', textAlign: 'center' }}>
                  <Layers size={30} strokeWidth={1} color="#2d2d3a" />
                  <p style={{ fontSize: '12px', color: '#374151', lineHeight: 1.5 }}>Click an OCR block<br />on the canvas to edit it</p>
                  <p style={{ fontSize: '10.5px', color: '#2d2d3a' }}>Drag to move · Double-click to type</p>
                </div>
              )
            ) : (
              <>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
                  <div style={S.logoIcon}><Brain size={13} color="#fff" /></div>
                  <span style={{ fontSize: '13px', fontWeight: 700 }}>AI Insights</span>
                </div>
                <div style={{ padding: '12px', background: 'rgba(99,102,241,0.07)', border: '1px solid rgba(99,102,241,0.15)', borderRadius: '9px', marginBottom: '14px' }}>
                  <SLabel>OCR Quality Score</SLabel>
                  <p style={{ fontSize: '32px', fontWeight: 800, color: confColor(avgConf), marginBottom: '4px', lineHeight: 1 }}>
                    {avgConf}<span style={{ fontSize: '14px', color: '#475569', fontWeight: 400 }}>%</span>
                  </p>
                  <p style={{ fontSize: '11px', color: '#64748b' }}>
                    {avgConf >= 80 ? 'Excellent recognition quality' : avgConf >= 60 ? 'Good — review highlighted blocks' : 'Low confidence — manual review recommended'}
                  </p>
                </div>
                {blocks.filter(b => b.confidence < 60).length > 0 ? (
                  <div style={{ padding: '9px', background: 'rgba(245,158,11,0.07)', border: '1px solid rgba(245,158,11,0.15)', borderRadius: '6px', marginBottom: '8px' }}>
                    <p style={{ fontSize: '11.5px', color: '#f59e0b', fontWeight: 700, marginBottom: '3px' }}>
                      {blocks.filter(b => b.confidence < 60).length} blocks need review
                    </p>
                    <p style={{ fontSize: '11px', color: '#64748b' }}>Red overlays mark low-confidence text</p>
                  </div>
                ) : (
                  <div style={{ padding: '9px', background: 'rgba(34,197,94,0.07)', border: '1px solid rgba(34,197,94,0.15)', borderRadius: '6px' }}>
                    <p style={{ fontSize: '11.5px', color: '#22c55e', fontWeight: 700, marginBottom: '3px' }}>✓ All blocks look great</p>
                    <p style={{ fontSize: '11px', color: '#64748b' }}>High confidence across all OCR results</p>
                  </div>
                )}
              </>
            )}
          </div>
        </aside>
      </div>

      {/* STATUS BAR */}
      <footer style={S.statusBar}>
        <span>Zoom <b style={{ color: '#4a5568' }}>{Math.round(zoom * 100)}%</b></span>
        <Dot /><span><b style={{ color: '#4a5568' }}>{blocks.length}</b> OCR blocks</span>
        <Dot /><span>Avg <b style={{ color: confColor(avgConf) }}>{avgConf}%</b></span>
        <Dot /><span><b style={{ color: '#4a5568' }}>{editedCount}</b> edited</span>
        <span style={{ marginLeft: 'auto' }}>
          {isSaving ? <span style={{ color: '#6366f1' }}>● Saving…</span>
            : isDirty  ? <span style={{ color: '#f59e0b' }}>● Unsaved changes</span>
            : lastSaved ? <span style={{ color: '#22c55e' }}>✓ Saved · {lastSaved.toLocaleTimeString()}</span>
            : <span>Ctrl+S save · Ctrl+Z undo · Double-click to edit</span>}
        </span>
      </footer>
    </div>
  );
}

// ─── Micro components ──────────────────────────────────────────────────────────

function SLabel({ children }: { children: React.ReactNode }) {
  return <p style={{ fontSize: '10px', color: '#374151', fontWeight: 700, letterSpacing: '0.09em', textTransform: 'uppercase', marginBottom: '8px' }}>{children}</p>;
}
function Divider() {
  return <div style={{ height: '1px', background: 'rgba(255,255,255,0.06)', margin: '12px 0' }} />;
}
function Dot() {
  return <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)', flexShrink: 0 }} />;
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const S = {
  header:   { height: '52px', flexShrink: 0 as const, background: 'rgba(10,10,18,0.98)', borderBottom: '1px solid rgba(255,255,255,0.07)', backdropFilter: 'blur(16px)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '10px', zIndex: 50 },
  toolbar:  { height: '44px', flexShrink: 0 as const, background: 'rgba(13,13,20,0.97)', borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 12px', gap: '4px', backdropFilter: 'blur(8px)', zIndex: 40 },
  searchPanel: { height: '40px', flexShrink: 0 as const, background: 'rgba(15,15,22,0.98)', borderBottom: '1px solid rgba(255,255,255,0.06)', display: 'flex', alignItems: 'center', padding: '0 12px', gap: '8px', zIndex: 35 },
  sidebar:  { width: '240px', flexShrink: 0 as const, background: '#0f0f18', borderRight: '1px solid rgba(255,255,255,0.06)', display: 'flex', flexDirection: 'column' as const, overflow: 'hidden' },
  tabRow:   { display: 'flex', padding: '4px', gap: '2px', borderBottom: '1px solid rgba(255,255,255,0.06)', flexShrink: 0 as const },
  tab:      { flex: 1, padding: '6px 4px', borderRadius: '5px', border: 'none', cursor: 'pointer', fontSize: '10.5px', fontWeight: 700 as const, letterSpacing: '0.05em', textTransform: 'uppercase' as const, background: 'transparent', color: '#475569' },
  tabActive:{ background: 'rgba(99,102,241,0.18)', color: '#818cf8' },
  panelBody:{ flex: 1, overflow: 'auto', padding: '10px 8px' },
  blockItem:{ padding: '6px 8px', borderRadius: '6px', cursor: 'pointer', marginBottom: '2px', background: 'transparent', border: '1px solid transparent' },
  blockItemActive: { background: 'rgba(99,102,241,0.14)', border: '1px solid rgba(99,102,241,0.3)' },
  blockPreview: { background: 'rgba(255,255,255,0.04)', borderRadius: '7px', border: '1px solid rgba(255,255,255,0.07)', padding: '9px 10px', marginBottom: '14px' },
  pageThumb: { width: '100%', padding: '8px 10px', borderRadius: '6px', border: 'none', cursor: 'pointer', marginBottom: '4px', textAlign: 'left' as const, background: 'transparent', color: '#94a3b8' },
  pageThumbActive: { background: 'rgba(99,102,241,0.15)', color: '#818cf8' },
  logoIcon: { width: '26px', height: '26px', borderRadius: '7px', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 as const },
  sep:      { width: '1px', height: '18px', background: 'rgba(255,255,255,0.08)', flexShrink: 0 as const },
  badge:    { fontSize: '10px', color: '#64748b', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '4px', padding: '2px 7px', fontWeight: 600 as const },
  filenameText: { fontSize: '14px', fontWeight: 500 as const, maxWidth: '260px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' as const },
  backBtn:  { display: 'flex', alignItems: 'center', gap: '5px', color: '#94a3b8', fontSize: '13px', fontWeight: 500 as const, padding: '6px 8px', borderRadius: '6px', border: 'none', background: 'transparent', cursor: 'pointer' },
  btnSave:  { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 12px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: '#6366f1', color: '#fff', fontSize: '12px', fontWeight: 700 as const },
  btnSaveDisabled: { background: 'rgba(255,255,255,0.05)', color: '#475569', cursor: 'default' as const },
  btnOutline: { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 10px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.08)', background: 'transparent', cursor: 'pointer', color: '#94a3b8', fontSize: '12px', fontWeight: 600 as const },
  btnPrimary: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px', padding: '7px 14px', borderRadius: '7px', border: 'none', cursor: 'pointer', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', color: '#fff', fontSize: '12px', fontWeight: 700 as const },
  editBtn:  { width: '100%', padding: '9px', borderRadius: '8px', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', color: '#fff', border: 'none', cursor: 'pointer', fontSize: '13px', fontWeight: 700 as const, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' },
  stylBtn:  { display: 'flex', alignItems: 'center', justifyContent: 'center', width: '30px', height: '30px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: 'rgba(255,255,255,0.05)', color: '#64748b' },
  stylBtnActive: { background: 'rgba(99,102,241,0.22)', color: '#818cf8' },
  toolBtn:  { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', height: '30px', minWidth: '30px', padding: '0 8px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: 'transparent', color: '#64748b', fontSize: '13px' },
  sizeInput:{ width: '48px', padding: '3px 6px', borderRadius: '5px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', color: '#e2e8f0', fontSize: '12px', fontWeight: 600 as const, textAlign: 'center' as const },
  searchInput: { flex: 1, minWidth: 0, padding: '4px 8px', borderRadius: '5px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', color: '#e2e8f0', fontSize: '12px', outline: 'none' },
  statusBar:{ height: '26px', flexShrink: 0 as const, background: '#07070f', borderTop: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '12px', fontSize: '11px', color: '#2d3748' },
  docShadow:'0 30px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.06)',
};
