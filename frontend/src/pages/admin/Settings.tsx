import { useAuthStore } from '@/store/authStore';
import { useModelStore } from '@/store/modelStore';
import { useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { Settings, Cpu, Brain, Zap, Crown, FlaskConical, Check, Sparkles } from 'lucide-react';
import { Spinner } from '@/components/Spinner';
import toast from 'react-hot-toast';
import type { ChatModelInfo, EmbeddingModelInfo } from '@/types';

const tierIcons: Record<string, typeof Zap> = {
  economy: Zap,
  standard: Cpu,
  premium: Crown,
  reasoning: FlaskConical,
};

const tierColors: Record<string, string> = {
  economy: 'text-green-600 bg-green-50 border-green-200 dark:text-green-400 dark:bg-green-900/20 dark:border-green-800',
  standard: 'text-blue-600 bg-blue-50 border-blue-200 dark:text-blue-400 dark:bg-blue-900/20 dark:border-blue-800',
  premium: 'text-purple-600 bg-purple-50 border-purple-200 dark:text-purple-400 dark:bg-purple-900/20 dark:border-purple-800',
  reasoning: 'text-amber-600 bg-amber-50 border-amber-200 dark:text-amber-400 dark:bg-amber-900/20 dark:border-amber-800',
};

const providerColors: Record<string, string> = {
  openai: 'text-gray-600 bg-gray-100 dark:text-gray-300 dark:bg-gray-800',
  gemini: 'text-sky-600 bg-sky-50 dark:text-sky-300 dark:bg-sky-900/30',
};

export default function SettingsPage() {
  const user = useAuthStore((s) => s.user);
  const { chatModel, embeddingModel, setChatModel, setEmbeddingModel } = useModelStore();

  const { data: models, isLoading } = useQuery({
    queryKey: ['available-models'],
    queryFn: ragService.getModels,
    staleTime: 10 * 60 * 1000,
  });

  const handleChatModelChange = (id: string) => {
    setChatModel(id);
    toast.success(`Chat model set to ${id}`);
  };

  const handleEmbeddingModelChange = (id: string) => {
    setEmbeddingModel(id);
    toast.success(`Embedding model set to ${id}`);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
          <Settings size={24} />
          Settings
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Manage your organization settings and AI model preferences
        </p>
      </div>

      {/* Organization Info */}
      <div className="card space-y-6">
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Organization Info</h3>
          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Organization Name
              </label>
              <input type="text" className="input-field" defaultValue={user?.tenantName ?? ''} readOnly />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Tenant ID
              </label>
              <input type="text" className="input-field font-mono text-xs" defaultValue={user?.tenantId ?? ''} readOnly />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Admin Email
              </label>
              <input type="email" className="input-field" defaultValue={user?.email ?? ''} readOnly />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Role
              </label>
              <input type="text" className="input-field" defaultValue={user?.role ?? ''} readOnly />
            </div>
          </div>
        </div>
      </div>

      {/* AI Chat Model Selection */}
      <div className="card space-y-4">
        <div className="flex items-center gap-2">
          <Brain className="h-5 w-5 text-primary-600" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Chat Model</h3>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Select the AI model used for answering your questions. Premium models are more capable but cost more tokens.
        </p>

        {isLoading ? (
          <div className="flex justify-center py-8"><Spinner /></div>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {models?.chatModels.map((m: ChatModelInfo) => {
              const Icon = tierIcons[m.tier] || Cpu;
              const colors = tierColors[m.tier] || tierColors.standard;
              const isSelected = chatModel === m.id;
              return (
                <button
                  key={m.id}
                  onClick={() => handleChatModelChange(m.id)}
                  className={`relative rounded-xl border-2 p-4 text-left transition-all ${
                    isSelected
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-2 ring-primary-500/30'
                      : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                  }`}
                >
                  {isSelected && (
                    <div className="absolute top-2 right-2">
                      <Check className="h-5 w-5 text-primary-600" />
                    </div>
                  )}
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium ${colors}`}>
                      <Icon size={12} />
                      {m.tier}
                    </span>
                  </div>
                  <p className="font-semibold text-gray-900 dark:text-white">{m.name}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{m.description}</p>
                  <div className="flex items-center gap-1.5 mt-1">
                    {m.provider && (
                      <span className={`inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-[10px] font-medium ${providerColors[m.provider] || providerColors.openai}`}>
                        {m.provider === 'gemini' && <Sparkles size={9} />}
                        {m.provider}
                      </span>
                    )}
                    <span className="text-[10px] font-mono text-gray-400">{m.id}</span>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Embedding Model Selection */}
      <div className="card space-y-4">
        <div className="flex items-center gap-2">
          <Cpu className="h-5 w-5 text-primary-600" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Embedding Model</h3>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Select the model used for generating text embeddings for document search. Higher dimensions = better accuracy but slower.
        </p>

        {isLoading ? (
          <div className="flex justify-center py-8"><Spinner /></div>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {models?.embeddingModels.map((m: EmbeddingModelInfo) => {
              const isSelected = embeddingModel === m.id;
              return (
                <button
                  key={m.id}
                  onClick={() => handleEmbeddingModelChange(m.id)}
                  className={`relative rounded-xl border-2 p-4 text-left transition-all ${
                    isSelected
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-2 ring-primary-500/30'
                      : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                  }`}
                >
                  {isSelected && (
                    <div className="absolute top-2 right-2">
                      <Check className="h-5 w-5 text-primary-600" />
                    </div>
                  )}
                  <p className="font-semibold text-gray-900 dark:text-white">{m.name}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{m.description}</p>
                  <div className="flex items-center gap-1.5 mt-2">
                    <span className="inline-block rounded-full bg-gray-100 dark:bg-gray-800 px-2 py-0.5 text-xs font-mono text-gray-600 dark:text-gray-400">
                      {m.dimensions} dims
                    </span>
                    {m.provider && (
                      <span className={`inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-[10px] font-medium ${providerColors[m.provider] || providerColors.openai}`}>
                        {m.provider === 'gemini' && <Sparkles size={9} />}
                        {m.provider}
                      </span>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        )}

        <div className="rounded-lg border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20 p-3">
          <p className="text-xs text-amber-700 dark:text-amber-300">
            <strong>Note:</strong> Changing the embedding model only affects new queries. Documents already processed use
            the model that was active at upload time. For best results, re-upload documents after changing models.
          </p>
        </div>
      </div>

      {/* Security Note */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Security</h3>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Token storage best practice: For production environments, use a secure httpOnly cookie
          strategy where the backend sets auth cookies directly, preventing JavaScript access to
          tokens. The current implementation uses in-memory state with Zustand persist for
          convenience during development.
        </p>
      </div>
    </div>
  );
}
