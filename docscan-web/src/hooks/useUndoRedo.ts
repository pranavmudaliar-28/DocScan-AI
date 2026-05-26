import { useState, useCallback } from 'react';

interface HistoryState<T> {
  past: T[];
  present: T;
  future: T[];
}

export function useUndoRedo<T>(initialState: T, maxHistory = 60) {
  const [state, setState] = useState<HistoryState<T>>({
    past: [],
    present: initialState,
    future: [],
  });

  const push = useCallback(
    (newPresent: T) => {
      setState(({ past, present }) => ({
        past: [...past, present].slice(-maxHistory),
        present: newPresent,
        future: [],
      }));
    },
    [maxHistory],
  );

  const undo = useCallback(() => {
    setState(({ past, present, future }) => {
      if (past.length === 0) return { past, present, future };
      const previous = past[past.length - 1];
      return {
        past: past.slice(0, -1),
        present: previous,
        future: [present, ...future],
      };
    });
  }, []);

  const redo = useCallback(() => {
    setState(({ past, present, future }) => {
      if (future.length === 0) return { past, present, future };
      const next = future[0];
      return {
        past: [...past, present],
        present: next,
        future: future.slice(1),
      };
    });
  }, []);

  const reset = useCallback((newState: T) => {
    setState({ past: [], present: newState, future: [] });
  }, []);

  return {
    state: state.present,
    push,
    undo,
    redo,
    reset,
    canUndo: state.past.length > 0,
    canRedo: state.future.length > 0,
    historySize: state.past.length,
  };
}
