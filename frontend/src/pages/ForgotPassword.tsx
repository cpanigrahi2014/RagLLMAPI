import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Brain } from 'lucide-react';
import toast from 'react-hot-toast';

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
});

type ForgotForm = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotForm>({ resolver: zodResolver(schema) });

  const onSubmit = async (_data: ForgotForm) => {
    // Placeholder – backend endpoint not yet implemented
    await new Promise((r) => setTimeout(r, 1000));
    toast.success('If an account with that email exists, a reset link has been sent.');
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-white px-6 dark:bg-[#0f0f17]">
      <div className="w-full max-w-md">
        <div className="mb-8 flex items-center gap-2 justify-center">
          <Brain className="h-8 w-8 text-primary-600" />
          <span className="text-xl font-bold text-gray-900 dark:text-white">CBSE AI Tutor</span>
        </div>

        <h2 className="text-2xl font-bold text-gray-900 dark:text-white text-center">
          Reset your password
        </h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400 text-center">
          Enter your email and we'll send you a reset link
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              Email address
            </label>
            <input
              type="email"
              className="input-field"
              placeholder="you@school.com"
              {...register('email')}
            />
            {errors.email && (
              <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>
            )}
          </div>

          <button type="submit" disabled={isSubmitting} className="btn-primary w-full">
            {isSubmitting ? 'Sending...' : 'Send reset link'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
          Remember your password?{' '}
          <Link to="/login" className="font-medium text-primary-600 hover:text-primary-500">
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
