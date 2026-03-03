import { useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { useAuthStore } from '@/store/authStore';
import { StatCard } from '@/components/StatCard';
import { CardSkeleton } from '@/components/Skeleton';
import { Users, BookOpen, MessageSquare, BarChart3 } from 'lucide-react';
import { formatNumber } from '@/utils/helpers';

export default function TeacherDashboard() {
  const user = useAuthStore((s) => s.user);

  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: ragService.getDashboard,
  });

  const { data: usage } = useQuery({
    queryKey: ['usage'],
    queryFn: ragService.getUsage,
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Teacher Dashboard
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Welcome back, {user?.fullName}. Here's your class overview.
        </p>
      </div>

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
              title="Total Students"
              value={formatNumber(dashboard?.totalUsers ?? 0)}
              icon={<Users size={22} />}
            />
            <StatCard
              title="Books Uploaded"
              value={formatNumber(dashboard?.totalDocuments ?? 0)}
              icon={<BookOpen size={22} />}
            />
            <StatCard
              title="Student Questions"
              value={formatNumber(dashboard?.totalQueries ?? 0)}
              icon={<MessageSquare size={22} />}
              subtitle="This month"
            />
            <StatCard
              title="Avg Response Time"
              value={`${dashboard?.averageResponseTime ?? 0}ms`}
              icon={<BarChart3 size={22} />}
            />
          </>
        )}
      </div>

      {/* Usage Overview */}
      {usage && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Token Usage
          </h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-600 dark:text-gray-400">Monthly Usage</span>
              <span className="font-medium text-gray-900 dark:text-white">
                {formatNumber(usage.totalTokensUsed)} / {formatNumber(usage.monthlyTokenLimit)}
              </span>
            </div>
            <div className="h-3 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
              <div
                className="h-full rounded-full bg-primary-600 transition-all duration-500"
                style={{ width: `${Math.min(usage.usagePercentage, 100)}%` }}
              />
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {usage.usagePercentage.toFixed(1)}% used · Plan: {usage.subscriptionPlan}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
