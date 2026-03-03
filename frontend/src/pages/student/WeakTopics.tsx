import { TrendingDown, BookOpen } from 'lucide-react';

export default function WeakTopics() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Weak Topics</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Track your weak areas and get targeted practice suggestions
        </p>
      </div>

      <div className="card flex flex-col items-center justify-center py-16">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <TrendingDown className="h-8 w-8 text-gray-400" />
        </div>
        <h3 className="mt-4 text-lg font-medium text-gray-900 dark:text-white">
          Keep Asking Questions
        </h3>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 text-center max-w-sm">
          As you use the AI tutor, we'll analyze your questions and identify topics where you need more practice.
        </p>
        <a href="/student/ask" className="btn-primary mt-6 flex items-center gap-2">
          <BookOpen size={16} />
          Start Learning
        </a>
      </div>
    </div>
  );
}
