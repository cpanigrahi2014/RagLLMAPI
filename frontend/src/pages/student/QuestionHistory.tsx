import { MessageSquare, Clock } from 'lucide-react';

// Placeholder – question history requires a backend endpoint
export default function QuestionHistory() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">My Questions</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          View your previous questions and answers
        </p>
      </div>

      <div className="card flex flex-col items-center justify-center py-16">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <MessageSquare className="h-8 w-8 text-gray-400" />
        </div>
        <h3 className="mt-4 text-lg font-medium text-gray-900 dark:text-white">
          No questions yet
        </h3>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 text-center max-w-sm">
          Your question history will appear here. Go to the Ask AI page to start asking questions.
        </p>
        <a href="/student/ask" className="btn-primary mt-6 flex items-center gap-2">
          <Clock size={16} />
          Ask your first question
        </a>
      </div>
    </div>
  );
}
