import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Upload, FileText, CheckCircle2, XCircle, Clock, ChevronDown, Trash2, Cpu } from 'lucide-react';
import toast from 'react-hot-toast';
import { ragService } from '@/services/ragService';
import { Spinner } from '@/components/Spinner';
import { sanitizeInput } from '@/utils/sanitize';
import { CBSE_CLASSES, CBSE_SUBJECTS, formatDateTime } from '@/utils/helpers';
import { useModelStore } from '@/store/modelStore';
import type { Book } from '@/types';

const uploadSchema = z.object({
  name: z.string().min(2, 'Book name is required'),
  subject: z.string().min(1, 'Select a subject'),
  classLevel: z.coerce.number().min(6).max(12),
});

type UploadForm = z.infer<typeof uploadSchema>;

const statusConfig: Record<Book['processingStatus'], { icon: React.ReactNode; color: string }> = {
  PENDING: { icon: <Clock size={14} className="animate-pulse" />, color: 'text-blue-600 bg-blue-50 dark:bg-blue-900/20' },
  PROCESSING: { icon: <Clock size={14} className="animate-spin" />, color: 'text-yellow-600 bg-yellow-50 dark:bg-yellow-900/20' },
  COMPLETED: { icon: <CheckCircle2 size={14} />, color: 'text-green-600 bg-green-50 dark:bg-green-900/20' },
  FAILED: { icon: <XCircle size={14} />, color: 'text-red-600 bg-red-50 dark:bg-red-900/20' },
};

export default function UploadBookPage() {
  const queryClient = useQueryClient();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [deletingBookId, setDeletingBookId] = useState<string | null>(null);
  const { embeddingModel, setEmbeddingModel } = useModelStore();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<UploadForm>({ resolver: zodResolver(uploadSchema) });

  const { data: books = [], isLoading: booksLoading } = useQuery({
    queryKey: ['books'],
    queryFn: ragService.getBooks,
  });

  const { data: models } = useQuery({
    queryKey: ['available-models'],
    queryFn: ragService.getModels,
    staleTime: 10 * 60 * 1000,
  });

  // Auto-correct stale embedding model selection
  useEffect(() => {
    if (!models) return;
    const embedIds = models.embeddingModels.map((m) => m.id);
    if (embedIds.length > 0 && !embedIds.includes(embeddingModel)) {
      setEmbeddingModel(embedIds.includes('text-embedding-3-small') ? 'text-embedding-3-small' : embedIds[0]);
    }
  }, [models, embeddingModel, setEmbeddingModel]);

  const uploadMutation = useMutation({
    mutationFn: (formData: FormData) => ragService.uploadBook(formData),
    onSuccess: () => {
      toast.success('Book upload initiated! Processing will take a few minutes.');
      reset();
      setSelectedFile(null);
      queryClient.invalidateQueries({ queryKey: ['books'] });
    },
    onError: (err: unknown) => {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Upload failed. Please try again.';
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (bookId: string) => ragService.deleteBook(bookId),
    onSuccess: () => {
      toast.success('Book deleted successfully');
      setDeletingBookId(null);
      queryClient.invalidateQueries({ queryKey: ['books'] });
    },
    onError: (err: unknown) => {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Delete failed. Please try again.';
      toast.error(message);
      setDeletingBookId(null);
    },
  });

  const handleDelete = (bookId: string, bookName: string) => {
    if (window.confirm(`Are you sure you want to delete "${bookName}"? This will remove all chapters, embeddings, and cannot be undone.`)) {
      setDeletingBookId(bookId);
      deleteMutation.mutate(bookId);
    }
  };

  const onSubmit = (data: UploadForm) => {
    if (!selectedFile) {
      toast.error('Please select a file');
      return;
    }

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('name', sanitizeInput(data.name));
    formData.append('subject', data.subject);
    formData.append('classLevel', String(data.classLevel));
    formData.append('embeddingModel', embeddingModel);

    uploadMutation.mutate(formData);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Upload Books</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Upload CBSE textbooks (PDF, DOC, DOCX, TXT) for AI-powered question answering
        </p>
      </div>

      {/* Upload Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="card space-y-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          New Upload
        </h3>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Book Name
            </label>
            <input
              type="text"
              className="input-field"
              placeholder="e.g., Physics NCERT Class 12"
              {...register('name')}
            />
            {errors.name && (
              <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Subject
            </label>
            <div className="relative">
              <select className="input-field appearance-none" {...register('subject')}>
                <option value="">Select subject</option>
                {CBSE_SUBJECTS.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
            {errors.subject && (
              <p className="mt-1 text-xs text-red-500">{errors.subject.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Class
            </label>
            <div className="relative">
              <select className="input-field appearance-none" {...register('classLevel')}>
                {CBSE_CLASSES.map((c) => (
                  <option key={c} value={c}>Class {c}</option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

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
                    {m.name} ({m.dimensions}d)
                  </option>
                )) ?? <option value={embeddingModel}>{embeddingModel}</option>}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
        </div>

        {/* File dropzone */}
        <div>
          <label
            className="flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 p-8 hover:border-primary-400 hover:bg-primary-50/30 dark:border-gray-600 dark:bg-gray-800/50 dark:hover:border-primary-500 transition-colors"
          >
            <Upload className="h-10 w-10 text-gray-400 mb-3" />
            <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
              {selectedFile ? selectedFile.name : 'Click to upload a document'}
            </p>
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              {selectedFile
                ? `${(selectedFile.size / 1024 / 1024).toFixed(2)} MB`
                : 'PDF, DOC, DOCX, TXT — max 50MB'}
            </p>
            <input
              type="file"
              accept=".pdf,.doc,.docx,.txt"
              className="hidden"
              onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
            />
          </label>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={uploadMutation.isPending || !selectedFile}
            className="btn-primary flex items-center gap-2"
          >
            {uploadMutation.isPending ? (
              <>
                <Spinner size="sm" className="text-white" />
                Uploading...
              </>
            ) : (
              <>
                <Upload size={16} />
                Upload Book
              </>
            )}
          </button>
        </div>
      </form>

      {/* Books List */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Uploaded Books
        </h3>

        {booksLoading ? (
          <div className="flex justify-center py-8">
            <Spinner size="lg" />
          </div>
        ) : books.length === 0 ? (
          <p className="py-8 text-center text-sm text-gray-500 dark:text-gray-400">
            No books uploaded yet. Upload your first book above.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Book</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Subject</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Class</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Status</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Uploaded</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {books.map((book) => {
                  const status = statusConfig[book.processingStatus];
                  return (
                    <tr key={book.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                      <td className="py-3 pr-4">
                        <div className="flex items-center gap-2">
                          <FileText size={16} className="text-primary-500 shrink-0" />
                          <span className="font-medium text-gray-900 dark:text-white">
                            {book.name}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 pr-4 text-gray-600 dark:text-gray-400">{book.subject}</td>
                      <td className="py-3 pr-4 text-gray-600 dark:text-gray-400">{book.classLevel}</td>
                      <td className="py-3 pr-4">
                        <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${status?.color ?? ''}`}>
                          {status?.icon}
                          {book.processingStatus}
                        </span>
                      </td>
                      <td className="py-3 pr-4 text-gray-500 dark:text-gray-400">
                        {formatDateTime(book.createdAt)}
                      </td>
                      <td className="py-3">
                        <button
                          onClick={() => handleDelete(book.id, book.name)}
                          disabled={deletingBookId === book.id}
                          className="inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/20 transition-colors disabled:opacity-50"
                          title="Delete book and all associated data"
                        >
                          {deletingBookId === book.id ? (
                            <Spinner size="sm" className="text-red-500" />
                          ) : (
                            <Trash2 size={14} />
                          )}
                          Delete
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
