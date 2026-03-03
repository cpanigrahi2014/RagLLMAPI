import { useQuery } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { Spinner } from '@/components/Spinner';
import { Users } from 'lucide-react';
import { formatNumber, formatDateTime } from '@/utils/helpers';

export default function StudentUsagePage() {
  const { data: users = [], isLoading } = useQuery({
    queryKey: ['userUsage'],
    queryFn: ragService.getUserUsage,
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
          <Users size={24} />
          Student Usage
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Monitor student activity and engagement
        </p>
      </div>

      <div className="card">
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner size="lg" />
          </div>
        ) : users.length === 0 ? (
          <p className="py-8 text-center text-sm text-gray-500 dark:text-gray-400">
            No student activity yet.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Student</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Role</th>
                  <th className="pb-3 text-right font-medium text-gray-500 dark:text-gray-400">Questions</th>
                  <th className="pb-3 text-right font-medium text-gray-500 dark:text-gray-400">Tokens</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Last Active</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {users.map((u) => (
                  <tr key={u.userId} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                    <td className="py-3 pr-4">
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">{u.fullName}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">{u.email}</p>
                      </div>
                    </td>
                    <td className="py-3 pr-4">
                      <span className="rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-400">
                        {u.role}
                      </span>
                    </td>
                    <td className="py-3 pr-4 text-right font-medium text-gray-900 dark:text-white">
                      {formatNumber(u.totalQueries)}
                    </td>
                    <td className="py-3 pr-4 text-right text-gray-600 dark:text-gray-400">
                      {formatNumber(u.tokensUsed)}
                    </td>
                    <td className="py-3 text-gray-500 dark:text-gray-400">
                      {formatDateTime(u.lastActivityAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
