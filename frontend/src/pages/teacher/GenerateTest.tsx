import { Brain } from 'lucide-react';

export default function GenerateTestPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Generate Test</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Create AI-generated tests from uploaded textbooks
        </p>
      </div>

      <div className="card flex flex-col items-center justify-center py-16">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <Brain className="h-8 w-8 text-gray-400" />
        </div>
        <h3 className="mt-4 text-lg font-medium text-gray-900 dark:text-white">
          Coming Soon
        </h3>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 text-center max-w-sm">
          Automated test generation from your uploaded content will be available soon. 
          You'll be able to select topics, difficulty levels, and question types.
        </p>
      </div>
    </div>
  );
}
