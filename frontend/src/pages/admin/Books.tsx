import { useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { Spinner } from '@/components/Spinner';
import { BookOpen, FileText, CheckCircle2, XCircle, Clock } from 'lucide-react';
import { formatDateTime } from '@/utils/helpers';
import type { Book } from '@/types';

const statusConfig: Record<Book['processingStatus'], { icon: React.ReactNode; color: string }> = {
  PENDING: { icon: <Clock size={14} className="animate-pulse" />, color: 'text-blue-600 bg-blue-50 dark:bg-blue-900/20' },
  PROCESSING: { icon: <Clock size={14} className="animate-spin" />, color: 'text-yellow-600 bg-yellow-50 dark:bg-yellow-900/20' },
  COMPLETED: { icon: <CheckCircle2 size={14} />, color: 'text-green-600 bg-green-50 dark:bg-green-900/20' },
  FAILED: { icon: <XCircle size={14} />, color: 'text-red-600 bg-red-50 dark:bg-red-900/20' },
};

export default function BooksPage() {
  const { data: books = [], isLoading } = useQuery({
    queryKey: ['books'],
    queryFn: ragService.getBooks,
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
          <BookOpen size={24} />
          Books Library
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          All uploaded textbooks in your organization
        </p>
      </div>

      <div className="card">
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner size="lg" />
          </div>
        ) : books.length === 0 ? (
          <div className="py-12 text-center">
            <BookOpen className="mx-auto h-12 w-12 text-gray-300 dark:text-gray-600" />
            <p className="mt-3 text-sm text-gray-500 dark:text-gray-400">
              No books uploaded yet. Teachers can upload books from their portal.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {books.map((book) => {
              const status = statusConfig[book.processingStatus];
              return (
                <div
                  key={book.id}
                  className="rounded-lg border border-gray-200 p-4 hover:shadow-md dark:border-gray-700 transition-shadow"
                >
                  <div className="flex items-start gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-50 dark:bg-primary-900/30">
                      <FileText className="h-5 w-5 text-primary-600 dark:text-primary-400" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="font-medium text-gray-900 dark:text-white truncate">
                        {book.name}
                      </h4>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {book.subject} · Class {book.classLevel}
                      </p>
                    </div>
                  </div>
                  <div className="mt-3 flex items-center justify-between">
                    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${status?.color ?? ''}`}>
                      {status?.icon}
                      {book.processingStatus}
                    </span>
                    <span className="text-xs text-gray-400">{formatDateTime(book.createdAt)}</span>
                  </div>
                  {book.totalPages != null && (
                    <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
                      {book.totalPages} pages
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
