import { useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { StatCard } from '@/components/StatCard';
import { CardSkeleton } from '@/components/Skeleton';
import { Users, BookOpen, MessageSquare, CreditCard, BarChart3, Zap } from 'lucide-react';
import { formatNumber, formatCurrency } from '@/utils/helpers';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

export default function AdminDashboard() {
  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: ragService.getDashboard,
  });

  const { data: usage } = useQuery({
    queryKey: ['usage'],
    queryFn: ragService.getUsage,
  });

  const { data: trend = [] } = useQuery({
    queryKey: ['queryTrend'],
    queryFn: ragService.getQueryTrend,
  });

  const { data: cost } = useQuery({
    queryKey: ['currentCost'],
    queryFn: ragService.getCurrentCost,
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Admin Dashboard</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Complete overview of your organization's platform usage
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        {isLoading ? (
          Array.from({ length: 6 }).map((_, i) => <CardSkeleton key={i} />)
        ) : (
          <>
            <StatCard
              title="Total Users"
              value={formatNumber(dashboard?.totalUsers ?? 0)}
              icon={<Users size={22} />}
            />
            <StatCard
              title="Documents"
              value={formatNumber(dashboard?.totalDocuments ?? 0)}
              icon={<BookOpen size={22} />}
            />
            <StatCard
              title="Total Queries"
              value={formatNumber(dashboard?.totalQueries ?? 0)}
              icon={<MessageSquare size={22} />}
            />
            <StatCard
              title="Avg Response"
              value={`${dashboard?.averageResponseTime ?? 0}ms`}
              icon={<Zap size={22} />}
            />
            <StatCard
              title="Est. Cost"
              value={formatCurrency(cost?.estimatedCost ?? usage?.estimatedCostUsd ?? 0)}
              icon={<CreditCard size={22} />}
            />
            <StatCard
              title="Plan"
              value={usage?.subscriptionPlan ?? 'FREE'}
              icon={<BarChart3 size={22} />}
            />
          </>
        )}
      </div>

      {/* Usage Bar */}
      {usage && (
        <div className="card">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Token Usage</h3>
            <span className="text-sm text-gray-500 dark:text-gray-400">
              {formatNumber(usage.totalTokensUsed)} / {formatNumber(usage.monthlyTokenLimit)}
            </span>
          </div>
          <div className="h-3 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
            <div
              className="h-full rounded-full bg-gradient-to-r from-primary-500 to-primary-700 transition-all duration-500"
              style={{ width: `${Math.min(usage.usagePercentage, 100)}%` }}
            />
          </div>
          <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            {usage.usagePercentage.toFixed(1)}% used · {formatNumber(usage.remainingTokens)} remaining
          </p>
        </div>
      )}

      {/* Query Trend Chart */}
      {trend.length > 0 && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Daily Query Trend
          </h3>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trend}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(v: string) => new Date(v).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
                />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip
                  contentStyle={{
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                    fontSize: '12px',
                  }}
                />
                <Line
                  type="monotone"
                  dataKey="queryCount"
                  stroke="#3b82f6"
                  strokeWidth={2}
                  dot={false}
                  name="Queries"
                />
                <Line
                  type="monotone"
                  dataKey="tokensUsed"
                  stroke="#10b981"
                  strokeWidth={2}
                  dot={false}
                  name="Tokens"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  );
}
