import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface ModelPreferences {
  chatModel: string;
  embeddingModel: string;
  setChatModel: (model: string) => void;
  setEmbeddingModel: (model: string) => void;
}

export const useModelStore = create<ModelPreferences>()(
  persist(
    (set) => ({
      chatModel: 'gpt-4.1-mini',
      embeddingModel: 'text-embedding-3-small',

      setChatModel: (model: string) => set({ chatModel: model }),
      setEmbeddingModel: (model: string) => set({ embeddingModel: model }),
    }),
    {
      name: 'ragllm-model-prefs',
    }
  )
);
