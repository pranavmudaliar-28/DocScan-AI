import { create } from 'zustand';

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

interface EditorStore {
  ocrBlocks: OcrBlock[];
  selectedBlockId: string | null;
  isDirty: boolean;
  isSaving: boolean;
  lastSaved: Date | null;
  showOverlays: boolean;
  zoom: number;

  setOcrBlocks: (blocks: OcrBlock[]) => void;
  selectBlock: (id: string | null) => void;
  updateBlockText: (id: string, text: string) => void;
  updateBlockStyle: (id: string, style: Partial<OcrBlock>) => void;
  setZoom: (zoom: number) => void;
  setIsSaving: (saving: boolean) => void;
  setLastSaved: (date: Date) => void;
  resetDirty: () => void;
  toggleOverlays: () => void;
}

export const useEditorStore = create<EditorStore>((set) => ({
  ocrBlocks: [],
  selectedBlockId: null,
  isDirty: false,
  isSaving: false,
  lastSaved: null,
  showOverlays: true,
  zoom: 1,

  setOcrBlocks: (blocks) => set({ ocrBlocks: blocks }),
  selectBlock: (id) => set({ selectedBlockId: id }),

  updateBlockText: (id, text) =>
    set((state) => ({
      ocrBlocks: state.ocrBlocks.map((b) =>
        b.id === id ? { ...b, editedText: text } : b,
      ),
      isDirty: true,
    })),

  updateBlockStyle: (id, style) =>
    set((state) => ({
      ocrBlocks: state.ocrBlocks.map((b) =>
        b.id === id ? { ...b, ...style } : b,
      ),
      isDirty: true,
    })),

  setZoom: (zoom) => set({ zoom }),
  setIsSaving: (isSaving) => set({ isSaving }),
  setLastSaved: (lastSaved) => set({ lastSaved }),
  resetDirty: () => set({ isDirty: false }),
  toggleOverlays: () => set((state) => ({ showOverlays: !state.showOverlays })),
}));
