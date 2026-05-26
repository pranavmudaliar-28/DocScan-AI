'use client';

import { useState, useRef, useCallback, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft, Download, Sparkles, RotateCcw, RotateCw,
  FlipHorizontal, FlipVertical, ScanText, ZoomIn, ZoomOut,
  Sun, Contrast, Palette, Sliders,
} from 'lucide-react';

export interface ImageEditorProps {
  documentId: string;
  filename: string;
  mimeType: string;
  downloadUrl: string | null;
}

interface Adjustments {
  brightness: number;    // 50–200
  contrast:   number;    // 50–200
  saturation: number;    // 0–200
  hue:        number;    // -180–180
  sharpness:  number;    // 0–10 (simulated via brightness micro-boost)
  rotation:   number;    // 0, 90, 180, 270
  flipH:      boolean;
  flipV:      boolean;
}

const DEFAULT: Adjustments = {
  brightness: 100, contrast: 100, saturation: 100,
  hue: 0, sharpness: 0, rotation: 0, flipH: false, flipV: false,
};

function buildFilterString(adj: Adjustments) {
  return [
    `brightness(${adj.brightness}%)`,
    `contrast(${adj.contrast}%)`,
    `saturate(${adj.saturation}%)`,
    `hue-rotate(${adj.hue}deg)`,
  ].join(' ');
}

function buildTransform(adj: Adjustments, zoom: number) {
  const parts: string[] = [];
  if (adj.flipH || adj.flipV) parts.push(`scale(${adj.flipH ? -1 : 1}, ${adj.flipV ? -1 : 1})`);
  if (adj.rotation !== 0) parts.push(`rotate(${adj.rotation}deg)`);
  parts.push(`scale(${zoom})`);
  return parts.join(' ');
}

function Slider({ label, icon: Icon, value, min, max, step = 1, onChange, unit = '%' }: {
  label: string; icon: any; value: number; min: number; max: number; step?: number;
  onChange: (v: number) => void; unit?: string;
}) {
  return (
    <div style={{ marginBottom: '14px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Icon size={12} color="#64748b" />
          <span style={{ fontSize: '11.5px', color: '#94a3b8', fontWeight: 500 }}>{label}</span>
        </div>
        <span style={{ fontSize: '11.5px', color: '#818cf8', fontWeight: 700 }}>{value}{unit}</span>
      </div>
      <input
        type="range" min={min} max={max} step={step} value={value}
        onChange={e => onChange(+e.target.value)}
        style={{ width: '100%', accentColor: '#6366f1', cursor: 'pointer' }}
      />
    </div>
  );
}

function SLabel({ children }: { children: React.ReactNode }) {
  return <p style={{ fontSize: '10px', color: '#374151', fontWeight: 700, letterSpacing: '0.09em', textTransform: 'uppercase', marginBottom: '10px' }}>{children}</p>;
}

export function ImageEditor({ documentId, filename, mimeType, downloadUrl }: ImageEditorProps) {
  const router = useRouter();
  const imgRef  = useRef<HTMLImageElement>(null);
  const [adj,   setAdj]   = useState<Adjustments>(DEFAULT);
  const [zoom,  setZoom]  = useState(1);
  const [tab,   setTab]   = useState<'adjust' | 'filters' | 'info'>('adjust');
  const isModified = JSON.stringify(adj) !== JSON.stringify(DEFAULT);

  const update = (key: keyof Adjustments) => (value: any) =>
    setAdj(prev => ({ ...prev, [key]: value }));

  const reset = () => setAdj(DEFAULT);

  const rotateCW  = () => setAdj(prev => ({ ...prev, rotation: (prev.rotation + 90) % 360 }));
  const rotateCCW = () => setAdj(prev => ({ ...prev, rotation: (prev.rotation + 270) % 360 }));

  // Canvas-based download preserving filters
  const handleDownload = useCallback(() => {
    const img = imgRef.current;
    if (!img || !downloadUrl) return;

    const canvas = document.createElement('canvas');
    const isRotated90 = adj.rotation === 90 || adj.rotation === 270;
    canvas.width  = isRotated90 ? img.naturalHeight : img.naturalWidth;
    canvas.height = isRotated90 ? img.naturalWidth  : img.naturalHeight;

    const ctx = canvas.getContext('2d')!;
    ctx.filter = buildFilterString(adj);

    // Apply transforms
    ctx.translate(canvas.width / 2, canvas.height / 2);
    ctx.rotate((adj.rotation * Math.PI) / 180);
    if (adj.flipH || adj.flipV) ctx.scale(adj.flipH ? -1 : 1, adj.flipV ? -1 : 1);
    ctx.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2);

    canvas.toBlob(blob => {
      if (!blob) return;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    }, mimeType || 'image/jpeg', 0.93);
  }, [adj, downloadUrl, filename, mimeType]);

  // Preset filters
  const PRESETS = [
    { name: 'Original',   adj: DEFAULT },
    { name: 'Vivid',      adj: { ...DEFAULT, saturation: 160, contrast: 120 } },
    { name: 'Cool',       adj: { ...DEFAULT, hue: -30, saturation: 80 } },
    { name: 'Warm',       adj: { ...DEFAULT, hue: 20, brightness: 108 } },
    { name: 'B&W',        adj: { ...DEFAULT, saturation: 0 } },
    { name: 'Faded',      adj: { ...DEFAULT, brightness: 115, contrast: 80, saturation: 60 } },
    { name: 'High Cont',  adj: { ...DEFAULT, contrast: 155, brightness: 95 } },
    { name: 'Brighten',   adj: { ...DEFAULT, brightness: 140, contrast: 105 } },
  ];

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
        <span style={{ ...S.badge, background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.2)', color: '#22c55e' }}>
          Image Editor
        </span>
        {isModified && <span style={{ ...S.badge, background: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.2)', color: '#f59e0b' }}>Modified</span>}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button onClick={reset} disabled={!isModified}
            style={{ ...S.btnOutline, opacity: isModified ? 1 : 0.4 }}>
            <RotateCcw size={12} /> Reset
          </button>
          <button onClick={handleDownload} style={S.btnSave}>
            <Download size={12} /> Download
          </button>
        </div>
      </header>

      {/* TOOLBAR */}
      <div style={S.toolbar}>
        <span style={{ fontSize: '11px', color: '#475569', marginRight: '4px' }}>Transform:</span>
        <button onClick={rotateCCW} style={S.toolBtn} title="Rotate 90° counter-clockwise"><RotateCcw size={14} /></button>
        <button onClick={rotateCW}  style={S.toolBtn} title="Rotate 90° clockwise"><RotateCw size={14} /></button>
        <div style={S.sep} />
        <button onClick={() => setAdj(p => ({ ...p, flipH: !p.flipH }))} title="Flip horizontal"
          style={{ ...S.toolBtn, background: adj.flipH ? 'rgba(99,102,241,0.18)' : undefined, color: adj.flipH ? '#818cf8' : undefined }}>
          <FlipHorizontal size={14} />
        </button>
        <button onClick={() => setAdj(p => ({ ...p, flipV: !p.flipV }))} title="Flip vertical"
          style={{ ...S.toolBtn, background: adj.flipV ? 'rgba(99,102,241,0.18)' : undefined, color: adj.flipV ? '#818cf8' : undefined }}>
          <FlipVertical size={14} />
        </button>
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <button onClick={() => setZoom(z => +(Math.max(0.25, z - 0.15).toFixed(2)))} style={S.toolBtn}><ZoomOut size={13} /></button>
          <button onClick={() => setZoom(1)} style={{ ...S.toolBtn, minWidth: '42px', fontSize: '11px', fontWeight: 700 }}>{Math.round(zoom * 100)}%</button>
          <button onClick={() => setZoom(z => +(Math.min(4, z + 0.15).toFixed(2)))} style={S.toolBtn}><ZoomIn size={13} /></button>
        </div>
      </div>

      {/* BODY */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>

        {/* Left Sidebar — Adjustments */}
        <aside style={{ ...S.sidebar, borderRight: '1px solid rgba(255,255,255,0.06)' }}>
          <div style={S.tabRow}>
            {(['adjust', 'filters', 'info'] as const).map(t => (
              <button key={t} onClick={() => setTab(t)}
                style={{ ...S.tab, ...(tab === t ? S.tabActive : {}) }}>
                {t === 'adjust' ? 'Adjust' : t === 'filters' ? 'Presets' : 'Info'}
              </button>
            ))}
          </div>

          <div style={S.panelBody}>
            {tab === 'adjust' && (
              <>
                <SLabel>Tone</SLabel>
                <Slider label="Brightness" icon={Sun}      value={adj.brightness} min={50}  max={200} onChange={update('brightness')} />
                <Slider label="Contrast"   icon={Contrast} value={adj.contrast}   min={50}  max={200} onChange={update('contrast')} />
                <div style={{ height: '1px', background: 'rgba(255,255,255,0.06)', margin: '14px 0' }} />
                <SLabel>Color</SLabel>
                <Slider label="Saturation" icon={Palette}  value={adj.saturation} min={0}   max={200} onChange={update('saturation')} />
                <Slider label="Hue Shift"  icon={Sliders}  value={adj.hue}        min={-180} max={180} onChange={update('hue')} unit="°" />
                <div style={{ height: '1px', background: 'rgba(255,255,255,0.06)', margin: '14px 0' }} />
                <SLabel>Rotation</SLabel>
                <p style={{ fontSize: '11px', color: '#475569', marginBottom: '10px' }}>Current: {adj.rotation}°</p>
                <div style={{ display: 'flex', gap: '6px' }}>
                  {[0, 90, 180, 270].map(deg => (
                    <button key={deg} onClick={() => setAdj(p => ({ ...p, rotation: deg }))}
                      style={{ flex: 1, padding: '5px 4px', borderRadius: '5px', border: 'none', cursor: 'pointer', fontSize: '10px', fontWeight: 700,
                        background: adj.rotation === deg ? 'rgba(99,102,241,0.25)' : 'rgba(255,255,255,0.05)',
                        color: adj.rotation === deg ? '#818cf8' : '#64748b' }}>
                      {deg}°
                    </button>
                  ))}
                </div>
              </>
            )}

            {tab === 'filters' && (
              <>
                <SLabel>Quick Presets</SLabel>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px' }}>
                  {PRESETS.map(preset => (
                    <button key={preset.name} onClick={() => setAdj({ ...preset.adj, rotation: adj.rotation, flipH: adj.flipH, flipV: adj.flipV })}
                      style={{ padding: '8px 6px', borderRadius: '7px', border: '1px solid rgba(255,255,255,0.07)', cursor: 'pointer', fontSize: '10.5px', fontWeight: 600,
                        background: JSON.stringify(adj) === JSON.stringify({ ...preset.adj, rotation: adj.rotation, flipH: adj.flipH, flipV: adj.flipV }) ? 'rgba(99,102,241,0.18)' : 'rgba(255,255,255,0.04)',
                        color: '#94a3b8' }}>
                      {preset.name}
                    </button>
                  ))}
                </div>
              </>
            )}

            {tab === 'info' && (
              <>
                <SLabel>File</SLabel>
                <p style={{ fontSize: '12.5px', color: '#e2e8f0', fontWeight: 500, marginBottom: '3px', wordBreak: 'break-all' }}>{filename}</p>
                <p style={{ fontSize: '11px', color: '#475569', marginBottom: '14px' }}>{mimeType}</p>
                <div style={{ height: '1px', background: 'rgba(255,255,255,0.06)', margin: '12px 0' }} />
                <SLabel>Current Adjustments</SLabel>
                {[
                  ['Brightness', `${adj.brightness}%`],
                  ['Contrast',   `${adj.contrast}%`],
                  ['Saturation', `${adj.saturation}%`],
                  ['Hue',        `${adj.hue}°`],
                  ['Rotation',   `${adj.rotation}°`],
                  ['Flip H',     adj.flipH ? 'Yes' : 'No'],
                  ['Flip V',     adj.flipV ? 'Yes' : 'No'],
                ].map(([k, v]) => (
                  <div key={k} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                    <span style={{ fontSize: '11.5px', color: '#475569' }}>{k}</span>
                    <span style={{ fontSize: '11.5px', color: '#e2e8f0', fontWeight: 600 }}>{v}</span>
                  </div>
                ))}
              </>
            )}
          </div>
        </aside>

        {/* Canvas */}
        <main style={{ flex: 1, background: '#12121c', overflow: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px' }}>
          {downloadUrl ? (
            <div style={{ position: 'relative', display: 'inline-block' }}>
              <img
                ref={imgRef}
                src={downloadUrl}
                alt={filename}
                draggable={false}
                style={{
                  display: 'block',
                  maxWidth: '800px',
                  maxHeight: 'calc(100vh - 160px)',
                  width: 'auto',
                  height: 'auto',
                  filter: buildFilterString(adj),
                  transform: buildTransform(adj, zoom),
                  transformOrigin: 'center',
                  transition: 'filter 0.1s ease, transform 0.1s ease',
                  boxShadow: '0 30px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.06)',
                  borderRadius: '3px',
                  userSelect: 'none',
                }}
              />
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', color: '#2d2d3a' }}>
              <ScanText size={52} strokeWidth={1} />
              <p style={{ fontSize: '14px' }}>No image available</p>
            </div>
          )}
        </main>

        {/* Right info panel */}
        <aside style={{ ...S.sidebar, borderLeft: '1px solid rgba(255,255,255,0.06)', borderRight: 'none' }}>
          <div style={{ padding: '14px 10px', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
            <SLabel>Editor Type</SLabel>
            <div style={{ padding: '10px', background: 'rgba(34,197,94,0.07)', border: '1px solid rgba(34,197,94,0.15)', borderRadius: '8px' }}>
              <p style={{ fontSize: '12px', color: '#22c55e', fontWeight: 700, marginBottom: '4px' }}>Image Editor</p>
              <p style={{ fontSize: '11px', color: '#64748b', lineHeight: 1.5 }}>Non-document image. Adjust brightness, contrast, saturation, and apply transforms.</p>
            </div>
          </div>
          <div style={S.panelBody}>
            <SLabel>Quick Actions</SLabel>
            <button onClick={handleDownload}
              style={{ ...S.btnPrimary, width: '100%', marginBottom: '8px', fontSize: '12px' }}>
              <Download size={13} /> Download Image
            </button>
            <button onClick={reset} disabled={!isModified}
              style={{ ...S.btnOutline, width: '100%', justifyContent: 'center', fontSize: '12px', opacity: isModified ? 1 : 0.4 }}>
              <RotateCcw size={12} /> Reset All Adjustments
            </button>
            <div style={{ height: '1px', background: 'rgba(255,255,255,0.06)', margin: '14px 0' }} />
            <SLabel>Shortcuts</SLabel>
            {[['Zoom in', 'Ctrl + +'], ['Zoom out', 'Ctrl + -'], ['Reset zoom', 'Ctrl + 0']].map(([k, v]) => (
              <div key={k} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                <span style={{ fontSize: '11px', color: '#475569' }}>{k}</span>
                <span style={{ fontSize: '10px', color: '#374151', background: 'rgba(255,255,255,0.06)', padding: '1px 6px', borderRadius: '3px', fontFamily: 'monospace' }}>{v}</span>
              </div>
            ))}
          </div>
        </aside>
      </div>

      {/* STATUS BAR */}
      <footer style={S.statusBar}>
        <span>Zoom <b style={{ color: '#4a5568' }}>{Math.round(zoom * 100)}%</b></span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span>Rotation <b style={{ color: '#4a5568' }}>{adj.rotation}°</b></span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span>Brightness <b style={{ color: '#4a5568' }}>{adj.brightness}%</b></span>
        <span style={{ width: '1px', height: '10px', background: 'rgba(255,255,255,0.06)' }} />
        <span>Contrast <b style={{ color: '#4a5568' }}>{adj.contrast}%</b></span>
        <span style={{ marginLeft: 'auto', color: '#374151' }}>
          {isModified ? 'Modified — click Download to save' : 'No adjustments made'}
        </span>
      </footer>
    </div>
  );
}

const S = {
  header:   { height: '52px', flexShrink: 0 as const, background: 'rgba(10,10,18,0.98)', borderBottom: '1px solid rgba(255,255,255,0.07)', backdropFilter: 'blur(16px)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '10px', zIndex: 50 },
  toolbar:  { height: '44px', flexShrink: 0 as const, background: 'rgba(13,13,20,0.97)', borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 12px', gap: '4px', backdropFilter: 'blur(8px)', zIndex: 40 },
  sidebar:  { width: '220px', flexShrink: 0 as const, background: '#0f0f18', display: 'flex', flexDirection: 'column' as const, overflow: 'hidden' },
  tabRow:   { display: 'flex', padding: '4px', gap: '2px', borderBottom: '1px solid rgba(255,255,255,0.06)', flexShrink: 0 as const },
  tab:      { flex: 1, padding: '6px 4px', borderRadius: '5px', border: 'none', cursor: 'pointer', fontSize: '10.5px', fontWeight: 700 as const, letterSpacing: '0.05em', textTransform: 'uppercase' as const, background: 'transparent', color: '#475569' },
  tabActive:{ background: 'rgba(99,102,241,0.18)', color: '#818cf8' },
  panelBody:{ flex: 1, overflow: 'auto', padding: '12px 10px' },
  logoIcon: { width: '26px', height: '26px', borderRadius: '7px', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 as const },
  sep:      { width: '1px', height: '18px', background: 'rgba(255,255,255,0.08)', flexShrink: 0 as const },
  badge:    { fontSize: '10px', color: '#64748b', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '4px', padding: '2px 7px', fontWeight: 600 as const },
  filenameText: { fontSize: '14px', fontWeight: 500 as const, maxWidth: '260px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' as const },
  backBtn:  { display: 'flex', alignItems: 'center', gap: '5px', color: '#94a3b8', fontSize: '13px', fontWeight: 500 as const, padding: '6px 8px', borderRadius: '6px', border: 'none', background: 'transparent', cursor: 'pointer' },
  btnSave:  { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 12px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: '#6366f1', color: '#fff', fontSize: '12px', fontWeight: 700 as const },
  btnOutline: { display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 10px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.08)', background: 'transparent', cursor: 'pointer', color: '#94a3b8', fontSize: '12px', fontWeight: 600 as const },
  btnPrimary: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px', padding: '8px 14px', borderRadius: '7px', border: 'none', cursor: 'pointer', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', color: '#fff', fontSize: '13px', fontWeight: 700 as const },
  toolBtn:  { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', height: '30px', minWidth: '30px', padding: '0 8px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: 'transparent', color: '#64748b', fontSize: '13px' },
  statusBar:{ height: '26px', flexShrink: 0 as const, background: '#07070f', borderTop: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 14px', gap: '12px', fontSize: '11px', color: '#2d3748' },
};
