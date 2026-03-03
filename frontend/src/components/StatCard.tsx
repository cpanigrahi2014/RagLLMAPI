import { cn } from '@/utils/helpers';
import type { ReactNode } from 'react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: ReactNode;
  trend?: { value: number; positive: boolean };
  className?: string;
}

export function StatCard({ title, value, subtitle, icon, trend, className }: StatCardProps) {
  return (
    <div className={cn('card flex items-start justify-between', className)}>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{title}</p>
        <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
        {subtitle && (
          <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">{subtitle}</p>
        )}
        {trend && (
          <p
            className={cn(
              'mt-1 text-xs font-medium',
              trend.positive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
            )}
          >
            {trend.positive ? '↑' : '↓'} {Math.abs(trend.value)}% from last period
          </p>
        )}
      </div>
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400">
        {icon}
      </div>
    </div>
  );
}
