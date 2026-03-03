import { useState, useRef, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { useModelStore } from '@/store/modelStore';
import { sanitizeInput, sanitizeHtml } from '@/utils/sanitize';
import { Spinner } from '@/components/Spinner';
import toast from 'react-hot-toast';
import {
  Wand2, FileText, HelpCircle, Volume2, ChevronDown, Brain,
  Play, Pause, Download, Copy, Check, BookOpen, Sparkles, ListChecks,
} from 'lucide-react';
import type { StudioResponse, Book, QAPair } from '@/types';

type StudioTab = 'prompt' | 'summarize' | 'qa' | 'tts';

function highlightMarkdown(text: string): string {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>');
}

export default function AIStudioPage() {
  const [activeTab, setActiveTab] = useState<StudioTab>('prompt');
  const { chatModel, setChatModel } = useModelStore();

  const { data: models } = useQuery({
    queryKey: ['available-models'],
    queryFn: ragService.getModels,
    staleTime: 10 * 60 * 1000,
  });

  const { data: books = [] } = useQuery({
    queryKey: ['books'],
    queryFn: ragService.getBooks,
  });

  // Auto-correct stale chat model
  useEffect(() => {
    if (!models) return;
    const chatIds = models.chatModels.map((m) => m.id);
    if (chatIds.length > 0 && !chatIds.includes(chatModel)) {
      setChatModel(chatIds.includes('gpt-4.1-mini') ? 'gpt-4.1-mini' : chatIds[0]);
    }
  }, [models, chatModel, setChatModel]);

  const completedBooks = books.filter((b) => b.processingStatus === 'COMPLETED');

  const tabs: { key: StudioTab; label: string; icon: React.ReactNode }[] = [
    { key: 'prompt', label: 'Custom Prompt', icon: <Wand2 size={16} /> },
    { key: 'summarize', label: 'Summarize', icon: <FileText size={16} /> },
    { key: 'qa', label: 'Q&A Generator', icon: <HelpCircle size={16} /> },
    { key: 'tts', label: 'Text to Speech', icon: <Volume2 size={16} /> },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
          <Sparkles className="h-7 w-7 text-primary-600" />
          AI Studio
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Create custom prompts, summarize documents, generate Q&A, and listen to content with text-to-speech
        </p>
      </div>

      {/* Tab Bar */}
      <div className="flex gap-1 rounded-lg bg-gray-100 dark:bg-gray-800 p-1">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium transition-all
              ${activeTab === tab.key
                ? 'bg-white dark:bg-gray-700 text-primary-600 dark:text-primary-400 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
              }`}
          >
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === 'prompt' && (
        <CustomPromptTab chatModel={chatModel} setChatModel={setChatModel} models={models} books={completedBooks} />
      )}
      {activeTab === 'summarize' && (
        <SummarizeTab chatModel={chatModel} setChatModel={setChatModel} models={models} books={completedBooks} />
      )}
      {activeTab === 'qa' && (
        <QAGeneratorTab chatModel={chatModel} setChatModel={setChatModel} models={models} books={completedBooks} />
      )}
      {activeTab === 'tts' && <TTSTab />}
    </div>
  );
}

// ═══════════════════════════════════════════════════
//  Custom Prompt Tab
// ═══════════════════════════════════════════════════

function CustomPromptTab({
  chatModel, setChatModel, models, books,
}: {
  chatModel: string; setChatModel: (m: string) => void;
  models: any; books: Book[];
}) {
  const [prompt, setPrompt] = useState('');
  const [selectedBook, setSelectedBook] = useState('');
  const [useDocContext, setUseDocContext] = useState(true);
  const [result, setResult] = useState<StudioResponse | null>(null);
  const resultRef = useRef<HTMLDivElement>(null);

  const mutation = useMutation({
    mutationFn: ragService.studioCustomPrompt,
    onSuccess: (data) => {
      setResult(data);
      setTimeout(() => resultRef.current?.scrollIntoView({ behavior: 'smooth' }), 100);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Failed to execute prompt');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) return;
    mutation.mutate({
      prompt: sanitizeInput(prompt),
      bookId: selectedBook || undefined,
      chatModel,
      useDocumentContext: useDocContext,
    });
  };

  return (
    <div className="space-y-4">
      <form onSubmit={handleSubmit} className="card space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Your Custom Prompt
          </label>
          <textarea
            rows={6}
            className="input-field resize-none"
            placeholder="Write your custom prompt here... e.g., 'Extract all formulas from this chapter and explain each one' or 'Create a mind map outline of this document'"
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            maxLength={5000}
          />
          <p className="mt-1 text-xs text-gray-400">{prompt.length}/5000 characters</p>
        </div>

        {/* Document Context */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5 flex items-center gap-1">
              <BookOpen size={14} />
              Document Context (Optional)
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={selectedBook}
                onChange={(e) => setSelectedBook(e.target.value)}
              >
                <option value="">No document — use general knowledge</option>
                {books.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} (Class {b.classLevel} — {b.subject})
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

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
                {models?.chatModels.map((m: any) => (
                  <option key={m.id} value={m.id}>
                    {m.name} — {m.description}
                  </option>
                )) ?? <option value={chatModel}>{chatModel}</option>}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
        </div>

        {selectedBook && (
          <label className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
            <input
              type="checkbox"
              checked={useDocContext}
              onChange={(e) => setUseDocContext(e.target.checked)}
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            Use document as context for the prompt
          </label>
        )}

        <button
          type="submit"
          disabled={mutation.isPending || !prompt.trim()}
          className="btn-primary w-full flex items-center justify-center gap-2"
        >
          {mutation.isPending ? (
            <>
              <Spinner size="sm" /> Executing prompt...
            </>
          ) : (
            <>
              <Wand2 size={18} /> Execute Prompt
            </>
          )}
        </button>
      </form>

      {result && (
        <div ref={resultRef} className="card">
          <ResultDisplay result={result} />
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════
//  Summarize Tab
// ═══════════════════════════════════════════════════

function SummarizeTab({
  chatModel, setChatModel, models, books,
}: {
  chatModel: string; setChatModel: (m: string) => void;
  models: any; books: Book[];
}) {
  const [selectedBook, setSelectedBook] = useState('');
  const [style, setStyle] = useState<'brief' | 'detailed' | 'bullet-points'>('detailed');
  const [result, setResult] = useState<StudioResponse | null>(null);
  const resultRef = useRef<HTMLDivElement>(null);

  const mutation = useMutation({
    mutationFn: ragService.studioSummarize,
    onSuccess: (data) => {
      setResult(data);
      setTimeout(() => resultRef.current?.scrollIntoView({ behavior: 'smooth' }), 100);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Failed to summarize document');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBook) { toast.error('Please select a document'); return; }
    mutation.mutate({ bookId: selectedBook, chatModel, style });
  };

  return (
    <div className="space-y-4">
      <form onSubmit={handleSubmit} className="card space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5 flex items-center gap-1">
              <BookOpen size={14} />
              Select Document
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={selectedBook}
                onChange={(e) => setSelectedBook(e.target.value)}
              >
                <option value="">Choose a document...</option>
                {books.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} (Class {b.classLevel} — {b.subject})
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5 flex items-center gap-1">
              <ListChecks size={14} />
              Summary Style
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={style}
                onChange={(e) => setStyle(e.target.value as any)}
              >
                <option value="brief">Brief (3-5 paragraphs)</option>
                <option value="detailed">Detailed (comprehensive)</option>
                <option value="bullet-points">Bullet Points</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

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
                {models?.chatModels.map((m: any) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                )) ?? <option value={chatModel}>{chatModel}</option>}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
        </div>

        <button
          type="submit"
          disabled={mutation.isPending || !selectedBook}
          className="btn-primary w-full flex items-center justify-center gap-2"
        >
          {mutation.isPending ? (
            <>
              <Spinner size="sm" /> Summarizing document...
            </>
          ) : (
            <>
              <FileText size={18} /> Summarize Document
            </>
          )}
        </button>
      </form>

      {result && (
        <div ref={resultRef} className="card">
          <ResultDisplay result={result} />
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════
//  Q&A Generator Tab
// ═══════════════════════════════════════════════════

function QAGeneratorTab({
  chatModel, setChatModel, models, books,
}: {
  chatModel: string; setChatModel: (m: string) => void;
  models: any; books: Book[];
}) {
  const [selectedBook, setSelectedBook] = useState('');
  const [count, setCount] = useState(10);
  const [difficulty, setDifficulty] = useState<'easy' | 'medium' | 'hard' | 'mixed'>('mixed');
  const [result, setResult] = useState<StudioResponse | null>(null);
  const resultRef = useRef<HTMLDivElement>(null);

  const mutation = useMutation({
    mutationFn: ragService.studioGenerateQA,
    onSuccess: (data) => {
      setResult(data);
      setTimeout(() => resultRef.current?.scrollIntoView({ behavior: 'smooth' }), 100);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Failed to generate Q&A');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBook) { toast.error('Please select a document'); return; }
    mutation.mutate({ bookId: selectedBook, chatModel, count, difficulty });
  };

  return (
    <div className="space-y-4">
      <form onSubmit={handleSubmit} className="card space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5 flex items-center gap-1">
              <BookOpen size={14} />
              Select Document
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={selectedBook}
                onChange={(e) => setSelectedBook(e.target.value)}
              >
                <option value="">Choose a document...</option>
                {books.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} (Class {b.classLevel})
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Number of Questions
            </label>
            <input
              type="number"
              className="input-field"
              value={count}
              onChange={(e) => setCount(Math.min(30, Math.max(1, +e.target.value)))}
              min={1}
              max={30}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Difficulty
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value as any)}
              >
                <option value="easy">Easy</option>
                <option value="medium">Medium</option>
                <option value="hard">Hard</option>
                <option value="mixed">Mixed</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

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
                {models?.chatModels.map((m: any) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                )) ?? <option value={chatModel}>{chatModel}</option>}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
        </div>

        <button
          type="submit"
          disabled={mutation.isPending || !selectedBook}
          className="btn-primary w-full flex items-center justify-center gap-2"
        >
          {mutation.isPending ? (
            <>
              <Spinner size="sm" /> Generating {count} questions...
            </>
          ) : (
            <>
              <HelpCircle size={18} /> Generate Q&A
            </>
          )}
        </button>
      </form>

      {result && (
        <div ref={resultRef} className="space-y-4">
          <div className="card">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Generated Questions & Answers
              </h3>
              <CopyButton text={result.result} />
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
              {result.tokensUsed} tokens · {result.responseTimeMs}ms · {result.chatModel}
            </p>
          </div>

          {result.qaPairs && result.qaPairs.length > 0 ? (
            result.qaPairs.map((qa) => <QACard key={qa.number} qa={qa} />)
          ) : (
            <div className="card">
              <div
                className="prose dark:prose-invert max-w-none text-sm"
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(highlightMarkdown(result.result)) }}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function QACard({ qa }: { qa: QAPair }) {
  const [showAnswer, setShowAnswer] = useState(false);

  return (
    <div className="card border-l-4 border-l-primary-500">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1">
          <p className="text-sm font-semibold text-primary-700 dark:text-primary-400 mb-1">
            Question {qa.number}
          </p>
          <p className="text-sm text-gray-900 dark:text-white">{qa.question}</p>
        </div>
        <span className="shrink-0 px-2 py-0.5 rounded-full text-[10px] font-medium bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300">
          {qa.difficulty}
        </span>
      </div>
      <button
        onClick={() => setShowAnswer(!showAnswer)}
        className="mt-3 text-xs font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 flex items-center gap-1"
      >
        {showAnswer ? 'Hide Answer' : 'Show Answer'}
      </button>
      {showAnswer && (
        <div className="mt-2 p-3 rounded-lg bg-green-50 dark:bg-green-900/20 text-sm text-green-900 dark:text-green-200">
          {qa.answer}
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════
//  Text-to-Speech Tab
// ═══════════════════════════════════════════════════

function TTSTab() {
  const [text, setText] = useState('');
  const [voice, setVoice] = useState<string>('nova');
  const [model, setModel] = useState<string>('tts-1');
  const [speed, setSpeed] = useState(1.0);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const mutation = useMutation({
    mutationFn: ragService.studioTTS,
    onSuccess: (blob) => {
      if (audioUrl) URL.revokeObjectURL(audioUrl);
      const url = URL.createObjectURL(blob);
      setAudioUrl(url);
      toast.success('Audio generated!');
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Failed to generate audio');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;
    mutation.mutate({ text: sanitizeInput(text), voice: voice as any, model: model as any, speed });
  };

  const togglePlay = () => {
    if (!audioRef.current) return;
    if (isPlaying) { audioRef.current.pause(); }
    else { audioRef.current.play(); }
    setIsPlaying(!isPlaying);
  };

  const downloadAudio = () => {
    if (!audioUrl) return;
    const a = document.createElement('a');
    a.href = audioUrl;
    a.download = 'speech.mp3';
    a.click();
  };

  useEffect(() => {
    return () => { if (audioUrl) URL.revokeObjectURL(audioUrl); };
  }, [audioUrl]);

  const voices = [
    { id: 'alloy', label: 'Alloy', desc: 'Neutral & balanced' },
    { id: 'echo', label: 'Echo', desc: 'Warm & conversational' },
    { id: 'fable', label: 'Fable', desc: 'Expressive & storytelling' },
    { id: 'onyx', label: 'Onyx', desc: 'Deep & authoritative' },
    { id: 'nova', label: 'Nova', desc: 'Friendly & natural' },
    { id: 'shimmer', label: 'Shimmer', desc: 'Clear & soothing' },
  ];

  return (
    <div className="space-y-4">
      <form onSubmit={handleSubmit} className="card space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Text to Convert to Speech
          </label>
          <textarea
            rows={5}
            className="input-field resize-none"
            placeholder="Paste or type the text you want to listen to... (max 4096 characters)"
            value={text}
            onChange={(e) => setText(e.target.value)}
            maxLength={4096}
          />
          <p className="mt-1 text-xs text-gray-400">{text.length}/4096 characters</p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Voice
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={voice}
                onChange={(e) => setVoice(e.target.value)}
              >
                {voices.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.label} — {v.desc}
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Quality
            </label>
            <div className="relative">
              <select
                className="input-field appearance-none"
                value={model}
                onChange={(e) => setModel(e.target.value)}
              >
                <option value="tts-1">Standard (tts-1)</option>
                <option value="tts-1-hd">HD (tts-1-hd)</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Speed ({speed.toFixed(1)}x)
            </label>
            <input
              type="range"
              min="0.25"
              max="4.0"
              step="0.25"
              value={speed}
              onChange={(e) => setSpeed(parseFloat(e.target.value))}
              className="w-full mt-2 accent-primary-600"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={mutation.isPending || !text.trim()}
          className="btn-primary w-full flex items-center justify-center gap-2"
        >
          {mutation.isPending ? (
            <>
              <Spinner size="sm" /> Generating audio...
            </>
          ) : (
            <>
              <Volume2 size={18} /> Generate Speech
            </>
          )}
        </button>
      </form>

      {/* Audio Player */}
      {audioUrl && (
        <div className="card">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-3">Generated Audio</h3>
          <div className="flex items-center gap-3">
            <button
              onClick={togglePlay}
              className="flex items-center justify-center w-12 h-12 rounded-full bg-primary-600 text-white hover:bg-primary-700 transition"
            >
              {isPlaying ? <Pause size={20} /> : <Play size={20} />}
            </button>
            <audio
              ref={audioRef}
              src={audioUrl}
              onEnded={() => setIsPlaying(false)}
              onPause={() => setIsPlaying(false)}
              onPlay={() => setIsPlaying(true)}
              className="flex-1"
              controls
            />
            <button
              onClick={downloadAudio}
              className="flex items-center gap-1 px-3 py-2 rounded-lg text-sm font-medium bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 transition"
            >
              <Download size={16} /> Download
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════
//  Shared Components
// ═══════════════════════════════════════════════════

function ResultDisplay({ result }: { result: StudioResponse }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Result</h3>
        <CopyButton text={result.result} />
      </div>
      <p className="text-xs text-gray-500 dark:text-gray-400 mb-3">
        {result.tokensUsed} tokens · {result.responseTimeMs}ms · {result.chatModel}
      </p>
      <div
        className="prose dark:prose-invert max-w-none text-sm leading-relaxed"
        dangerouslySetInnerHTML={{ __html: sanitizeHtml(highlightMarkdown(result.result)) }}
      />
    </div>
  );
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <button
      onClick={handleCopy}
      className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
    >
      {copied ? <Check size={14} className="text-green-500" /> : <Copy size={14} />}
      {copied ? 'Copied!' : 'Copy'}
    </button>
  );
}
