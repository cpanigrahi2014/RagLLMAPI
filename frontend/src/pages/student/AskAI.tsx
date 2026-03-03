import { useState, useRef, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Send, BookOpen, Sparkles, AlertCircle, ChevronDown, Brain, Cpu, ToggleLeft, ToggleRight, Database, Zap } from 'lucide-react';
import toast from 'react-hot-toast';
import { ragService } from '@/services/ragService';
import { Spinner } from '@/components/Spinner';
import { sanitizeInput, sanitizeHtml } from '@/utils/sanitize';
import { CBSE_CLASSES, CBSE_SUBJECTS } from '@/utils/helpers';
import { useModelStore } from '@/store/modelStore';
import type { QueryResponse } from '@/types';

const askSchema = z.object({
  query: z.string().min(5, 'Question must be at least 5 characters').max(1000),
  subject: z.string().optional(),
  classLevel: z.coerce.number().optional(),
});

type AskForm = z.infer<typeof askSchema>;

function highlightKeywords(text: string): string {
  // Bold text between ** markers and highlight technical terms
  let html = text
    .replace(/\*\*(.*?)\*\*/g, '<mark class="bg-yellow-100 dark:bg-yellow-900/40 px-0.5 rounded">$1</mark>')
    .replace(/\n/g, '<br/>');
  return html;
}

export default function AskAIPage() {
  const [response, setResponse] = useState<QueryResponse | null>(null);
  const [useRag, setUseRag] = useState(true);
  const answerRef = useRef<HTMLDivElement>(null);
  const { chatModel, embeddingModel, setChatModel, setEmbeddingModel } = useModelStore();

  const { data: models } = useQuery({
    queryKey: ['available-models'],
    queryFn: ragService.getModels,
    staleTime: 10 * 60 * 1000,
  });

  // Auto-correct stale model selections that no longer exist in the available list
  useEffect(() => {
    if (!models) return;
    const chatIds = models.chatModels.map((m) => m.id);
    const embedIds = models.embeddingModels.map((m) => m.id);
    if (chatIds.length > 0 && !chatIds.includes(chatModel)) {
      setChatModel(chatIds.includes('gpt-4.1-mini') ? 'gpt-4.1-mini' : chatIds[0]);
    }
    if (embedIds.length > 0 && !embedIds.includes(embeddingModel)) {
      setEmbeddingModel(embedIds.includes('text-embedding-3-small') ? 'text-embedding-3-small' : embedIds[0]);
    }
  }, [models, chatModel, embeddingModel, setChatModel, setEmbeddingModel]);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<AskForm>({ resolver: zodResolver(askSchema) });

  const mutation = useMutation({
    mutationFn: ragService.askQuestion,
    onSuccess: (data) => {
      setResponse(data);
      setTimeout(() => answerRef.current?.scrollIntoView({ behavior: 'smooth' }), 100);
    },
    onError: (err: unknown) => {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to get answer. Please try again.';
      toast.error(message);
    },
  });

  const onSubmit = (data: AskForm) => {
    mutation.mutate({
      query: sanitizeInput(data.query),
      subject: data.subject || undefined,
      classLevel: data.classLevel || undefined,
      chatModel,
      embeddingModel: useRag ? embeddingModel : undefined,
      useRag,
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
          <Sparkles className="h-7 w-7 text-primary-600" />
          Ask AI Tutor
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Ask any question from your uploaded textbooks and get detailed answers with source citations
        </p>
      </div>

      {/* Question Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="card space-y-4">
        {/* RAG Toggle */}
        <div className="flex items-center justify-between rounded-lg border border-gray-200 dark:border-gray-700 p-3 bg-gray-50 dark:bg-gray-800/50">
          <div className="flex items-center gap-3">
            {useRag ? (
              <Database size={18} className="text-primary-600" />
            ) : (
              <Zap size={18} className="text-amber-500" />
            )}
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-white">
                {useRag ? 'RAG Mode — Using Your Textbooks' : 'Direct Mode — General Knowledge'}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {useRag
                  ? 'Answers are sourced from your uploaded documents with citations'
                  : 'Answers use the AI model\'s general knowledge without document context'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setUseRag(!useRag)}
            className="shrink-0"
            title={useRag ? 'Switch to direct mode' : 'Switch to RAG mode'}
          >
            {useRag ? (
              <ToggleRight size={36} className="text-primary-600" />
            ) : (
              <ToggleLeft size={36} className="text-gray-400" />
            )}
          </button>
        </div>

        {useRag && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Class (Optional)
            </label>
            <div className="relative">
              <Controller
                name="classLevel"
                control={control}
                render={({ field }) => (
                  <select className="input-field appearance-none" {...field}>
                    <option value="">All classes</option>
                    {CBSE_CLASSES.map((c) => (
                      <option key={c} value={c}>
                        Class {c}
                      </option>
                    ))}
                  </select>
                )}
              />
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Subject (Optional)
            </label>
            <div className="relative">
              <select className="input-field appearance-none" {...register('subject')}>
                <option value="">All subjects</option>
                {CBSE_SUBJECTS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
        </div>
        )}

        {/* Model Selection Row */}
        <div className={`grid grid-cols-1 gap-4 ${useRag ? 'sm:grid-cols-2' : ''}`}>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5 flex items-center gap-1">
              <Brain size={14} />
              Chat Model
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={chatModel}
                onChange={(e) => setChatModel(e.target.value)}
              >
                {models?.chatModels.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name} — {m.description}
                  </option>
                )) ?? <option value={chatModel}>{chatModel}</option>}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

          {useRag && (
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5 flex items-center gap-1">
              <Cpu size={14} />
              Embedding Model
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={embeddingModel}
                onChange={(e) => setEmbeddingModel(e.target.value)}
              >
                {models?.embeddingModels.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name} ({m.dimensions} dims)
                  </option>
                )) ?? <option value={embeddingModel}>{embeddingModel}</option>}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Your Question
          </label>
          <textarea
            rows={4}
            className="input-field resize-none"
            placeholder="e.g., Explain Newton's second law of motion with examples..."
            {...register('query')}
          />
          {errors.query && (
            <p className="mt-1 text-xs text-red-500">{errors.query.message}</p>
          )}
        </div>

        <div className="flex items-center justify-between">
          <p className="text-xs text-gray-400 dark:text-gray-500">
            {useRag
              ? 'Answers are generated from your uploaded textbooks'
              : 'Answers use AI general knowledge (no document context)'}
          </p>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="btn-primary flex items-center gap-2"
          >
            {mutation.isPending ? (
              <>
                <Spinner size="sm" className="text-white" />
                Thinking...
              </>
            ) : (
              <>
                <Send size={16} />
                Ask
              </>
            )}
          </button>
        </div>
      </form>

      {/* Loading State */}
      {mutation.isPending && (
        <div className="card flex items-center gap-4">
          <div className="relative">
            <div className="h-10 w-10 rounded-full border-2 border-primary-200 border-t-primary-600 animate-spin" />
          </div>
          <div>
            <p className="font-medium text-gray-900 dark:text-white">
              {useRag ? 'Searching your textbooks...' : 'Generating answer...'}
            </p>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {useRag
                ? 'Finding relevant passages and generating a detailed answer'
                : 'Using AI general knowledge to answer your question'}
            </p>
          </div>
        </div>
      )}

      {/* Answer */}
      {response && !mutation.isPending && (
        <div ref={answerRef} className="space-y-4">
          <div className="card">
            <div className="flex items-center gap-2 mb-4">
              <Sparkles className="h-5 w-5 text-primary-600" />
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Answer</h3>
              <span className="ml-auto text-xs text-gray-400">
                {response.responseTimeMs}ms · {response.tokensUsed} tokens
                {response.chatModel && ` · ${response.chatModel}`}
              </span>
            </div>

            <div
              className="prose prose-sm max-w-none dark:prose-invert prose-p:text-gray-700 dark:prose-p:text-gray-300"
              dangerouslySetInnerHTML={{
                __html: sanitizeHtml(highlightKeywords(response.answer)),
              }}
            />
          </div>

          {/* Sources */}
          {response.sources.length > 0 && (
            <div className="card">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                <BookOpen size={18} />
                Source Citations
              </h3>
              <div className="space-y-3">
                {response.sources.map((source, idx) => (
                  <div
                    key={source.chunkId}
                    className="rounded-lg border border-gray-200 p-4 dark:border-gray-700"
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary-100 text-xs font-bold text-primary-700 dark:bg-primary-900/40 dark:text-primary-400">
                          {idx + 1}
                        </span>
                        <span className="text-sm font-medium text-gray-900 dark:text-white">
                          {source.bookName}
                        </span>
                      </div>
                      <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700 dark:bg-green-900/30 dark:text-green-400">
                        {(source.similarityScore * 100).toFixed(0)}% match
                      </span>
                    </div>
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">
                      {source.chapterTitle} · Page {source.pageNumber}
                    </p>
                    <p className="text-sm text-gray-600 dark:text-gray-300 italic">
                      "{source.content.slice(0, 200)}..."
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* No results warning */}
          {response.sources.length === 0 && (
            <div className="card flex items-center gap-3 border-orange-200 bg-orange-50 dark:border-orange-800 dark:bg-orange-900/20">
              <AlertCircle className="h-5 w-5 shrink-0 text-orange-500" />
              <p className="text-sm text-orange-700 dark:text-orange-300">
                This answer may not be from your prescribed books. No matching source passages were found.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
