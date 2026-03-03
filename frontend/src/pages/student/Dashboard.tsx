import { useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { useAuthStore } from '@/store/authStore';
import { StatCard } from '@/components/StatCard';
import { CardSkeleton } from '@/components/Skeleton';
import { MessageSquare, BookOpen, Zap, Clock } from 'lucide-react';
import { formatNumber } from '@/utils/helpers';

export default function StudentDashboard() {
  const user = useAuthStore((s) => s.user);

  const { data: usage, isLoading } = useQuery({
    queryKey: ['usage'],
    queryFn: ragService.getUsage,
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Welcome back, {user?.fullName?.split(' ')[0]} 👋
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Here's your learning overview
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {isLoading ? (
          <>
            <CardSkeleton />
            <CardSkeleton />
            <CardSkeleton />
            <CardSkeleton />
          </>
        ) : (
          <>
            <StatCard
              title="Questions Asked"
              value={formatNumber(usage?.totalTokensUsed ? Math.floor(usage.totalTokensUsed / 350) : 0)}
              icon={<MessageSquare size={22} />}
              subtitle="This month"
            />
            <StatCard
              title="Tokens Used"
              value={formatNumber(usage?.totalTokensUsed ?? 0)}
              icon={<Zap size={22} />}
              subtitle={`of ${formatNumber(usage?.monthlyTokenLimit ?? 0)}`}
            />
            <StatCard
              title="Remaining"
              value={`${(100 - (usage?.usagePercentage ?? 0)).toFixed(1)}%`}
              icon={<Clock size={22} />}
              subtitle={`${formatNumber(usage?.remainingTokens ?? 0)} tokens left`}
            />
            <StatCard
              title="Subscription"
              value={usage?.subscriptionPlan ?? 'FREE'}
              icon={<BookOpen size={22} />}
              subtitle={user?.tenantName ?? ''}
            />
          </>
        )}
      </div>

      {/* Quick Actions */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Quick Actions
        </h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <a
            href="/student/ask"
            className="flex items-center gap-3 rounded-lg border border-gray-200 p-4 hover:border-primary-300 hover:bg-primary-50/50 dark:border-gray-700 dark:hover:border-primary-600 dark:hover:bg-primary-900/20 transition-colors"
          >
            <MessageSquare className="h-8 w-8 text-primary-600" />
            <div>
              <p className="font-medium text-gray-900 dark:text-white">Ask AI</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Get instant answers</p>
            </div>
          </a>
          <a
            href="/student/tests"
            className="flex items-center gap-3 rounded-lg border border-gray-200 p-4 hover:border-primary-300 hover:bg-primary-50/50 dark:border-gray-700 dark:hover:border-primary-600 dark:hover:bg-primary-900/20 transition-colors"
          >
            <BookOpen className="h-8 w-8 text-green-600" />
            <div>
              <p className="font-medium text-gray-900 dark:text-white">Mock Tests</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Practice & improve</p>
            </div>
          </a>
          <a
            href="/student/weak-topics"
            className="flex items-center gap-3 rounded-lg border border-gray-200 p-4 hover:border-primary-300 hover:bg-primary-50/50 dark:border-gray-700 dark:hover:border-primary-600 dark:hover:bg-primary-900/20 transition-colors"
          >
            <Zap className="h-8 w-8 text-orange-600" />
            <div>
              <p className="font-medium text-gray-900 dark:text-white">Weak Topics</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Focus your study</p>
            </div>
          </a>
        </div>
      </div>
    </div>
  );
}
