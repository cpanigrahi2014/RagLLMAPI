import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ragService } from '@/services/ragService';
import { Spinner } from '@/components/Spinner';
import { CreditCard, CheckCircle2, ArrowUpCircle } from 'lucide-react';
import { formatCurrency, formatDate, formatNumber } from '@/utils/helpers';
import toast from 'react-hot-toast';
import type { SubscriptionPlan } from '@/types';

const plans: { name: SubscriptionPlan; price: string; features: string[] }[] = [
  { name: 'FREE', price: '$0/mo', features: ['500K tokens', '5 users', 'Basic support'] },
  { name: 'BASIC', price: '$29/mo', features: ['2M tokens', '25 users', 'Email support'] },
  { name: 'STANDARD', price: '$79/mo', features: ['10M tokens', '100 users', 'Priority support'] },
  { name: 'PREMIUM', price: '$199/mo', features: ['50M tokens', '500 users', 'Dedicated support'] },
  { name: 'ENTERPRISE', price: 'Custom', features: ['Unlimited', 'Unlimited users', 'SLA guarantee'] },
];

export default function BillingPage() {
  const queryClient = useQueryClient();

  const { data: usage } = useQuery({
    queryKey: ['usage'],
    queryFn: ragService.getUsage,
  });

  const { data: cost } = useQuery({
    queryKey: ['currentCost'],
    queryFn: ragService.getCurrentCost,
  });

  const { data: invoices = [], isLoading: invoicesLoading } = useQuery({
    queryKey: ['invoices'],
    queryFn: ragService.getInvoices,
  });

  const upgradeMutation = useMutation({
    mutationFn: ragService.upgradePlan,
    onSuccess: () => {
      toast.success('Plan upgraded successfully!');
      queryClient.invalidateQueries({ queryKey: ['usage'] });
    },
    onError: () => {
      toast.error('Failed to upgrade plan.');
    },
  });

  const currentPlan = usage?.subscriptionPlan ?? 'FREE';

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
          <CreditCard size={24} />
          Billing & Subscription
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Manage your subscription and view billing details
        </p>
      </div>

      {/* Current Plan */}
      {cost && (
        <div className="card bg-gradient-to-r from-primary-50 to-blue-50 dark:from-primary-900/20 dark:to-blue-900/20 border-primary-200 dark:border-primary-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-primary-700 dark:text-primary-400">Current Period</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">
                {formatCurrency(cost.estimatedCost)}
              </p>
              <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                {formatNumber(cost.tokensUsed)} tokens used · Plan: {cost.plan}
              </p>
            </div>
            <div className="text-right">
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{cost.currentPeriod}</p>
            </div>
          </div>
        </div>
      )}

      {/* Plans */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Available Plans
        </h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {plans.map((plan) => {
            const isCurrent = currentPlan === plan.name;
            return (
              <div
                key={plan.name}
                className={`card relative ${isCurrent ? 'ring-2 ring-primary-500' : ''}`}
              >
                {isCurrent && (
                  <span className="absolute -top-2.5 left-4 rounded-full bg-primary-600 px-2.5 py-0.5 text-xs font-medium text-white">
                    Current
                  </span>
                )}
                <h4 className="text-sm font-bold text-gray-900 dark:text-white">{plan.name}</h4>
                <p className="mt-1 text-xl font-bold text-primary-600">{plan.price}</p>
                <ul className="mt-3 space-y-1">
                  {plan.features.map((f) => (
                    <li key={f} className="flex items-center gap-1.5 text-xs text-gray-600 dark:text-gray-400">
                      <CheckCircle2 size={12} className="text-green-500 shrink-0" />
                      {f}
                    </li>
                  ))}
                </ul>
                {!isCurrent && (
                  <button
                    onClick={() => upgradeMutation.mutate(plan.name)}
                    disabled={upgradeMutation.isPending}
                    className="btn-secondary mt-4 w-full flex items-center justify-center gap-1 text-xs"
                  >
                    <ArrowUpCircle size={14} />
                    {isCurrent ? 'Current' : 'Upgrade'}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Invoices */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Invoice History
        </h3>

        {invoicesLoading ? (
          <div className="flex justify-center py-8">
            <Spinner size="lg" />
          </div>
        ) : invoices.length === 0 ? (
          <p className="py-8 text-center text-sm text-gray-500 dark:text-gray-400">
            No invoices yet.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Invoice</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Period</th>
                  <th className="pb-3 text-right font-medium text-gray-500 dark:text-gray-400">Amount</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Status</th>
                  <th className="pb-3 text-left font-medium text-gray-500 dark:text-gray-400">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {invoices.map((inv) => (
                  <tr key={inv.invoiceId} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                    <td className="py-3 pr-4 font-medium text-gray-900 dark:text-white">
                      #{inv.invoiceId.slice(0, 8)}
                    </td>
                    <td className="py-3 pr-4 text-gray-600 dark:text-gray-400">{inv.period}</td>
                    <td className="py-3 pr-4 text-right font-medium text-gray-900 dark:text-white">
                      {formatCurrency(inv.amount)}
                    </td>
                    <td className="py-3 pr-4">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        inv.status === 'PAID'
                          ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                          : 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
                      }`}>
                        {inv.status}
                      </span>
                    </td>
                    <td className="py-3 text-gray-500 dark:text-gray-400">
                      {formatDate(inv.createdAt)}
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
