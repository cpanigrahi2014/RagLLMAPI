import { useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { StatCard } from '@/components/StatCard';
import { CardSkeleton } from '@/components/Skeleton';
import { BarChart3, MessageSquare, Search } from 'lucide-react';
import { formatNumber } from '@/utils/helpers';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

export default function AnalyticsPage() {
  const { data: trend = [], isLoading: trendLoading } = useQuery({
    queryKey: ['queryTrend'],
    queryFn: ragService.getQueryTrend,
  });

  const { data: topQueries = [], isLoading: topLoading } = useQuery({
    queryKey: ['topQueries'],
    queryFn: ragService.getTopQueries,
  });

  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: ragService.getDashboard,
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
          <BarChart3 size={24} />
          Analytics
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Detailed analytics of platform usage
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
        {isLoading ? (
          <>
            <CardSkeleton />
            <CardSkeleton />
            <CardSkeleton />
          </>
        ) : (
          <>
            <StatCard
              title="Weekly Queries"
              value={formatNumber(dashboard?.queriesThisWeek ?? 0)}
              icon={<MessageSquare size={22} />}
            />
            <StatCard
              title="Today's Tokens"
              value={formatNumber(dashboard?.tokensUsedToday ?? 0)}
              icon={<BarChart3 size={22} />}
            />
            <StatCard
              title="Avg Response"
              value={`${dashboard?.averageResponseTime ?? 0}ms`}
              icon={<Search size={22} />}
            />
          </>
        )}
      </div>

      {/* Query Trend Chart */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Daily Query Volume
        </h3>
        {trendLoading ? (
          <div className="h-64 skeleton rounded-lg" />
        ) : (
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={trend}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(v: string) =>
                    new Date(v).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
                  }
                />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip
                  contentStyle={{
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                    fontSize: '12px',
                  }}
                />
                <Bar dataKey="queryCount" fill="#3b82f6" radius={[4, 4, 0, 0]} name="Queries" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      {/* Top Queries */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Top Queries
        </h3>
        {topLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="skeleton h-10 w-full rounded" />
            ))}
          </div>
        ) : topQueries.length === 0 ? (
          <p className="py-4 text-center text-sm text-gray-500 dark:text-gray-400">
            No query data yet.
          </p>
        ) : (
          <div className="space-y-2">
            {topQueries.map((q, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between rounded-lg border border-gray-200 p-3 dark:border-gray-700"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary-100 text-xs font-bold text-primary-700 dark:bg-primary-900/40 dark:text-primary-400">
                    {idx + 1}
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                      {q.query}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{q.subject}</p>
                  </div>
                </div>
                <span className="ml-4 shrink-0 rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-300">
                  {q.count}x
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
