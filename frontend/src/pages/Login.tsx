import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Brain, Eye, EyeOff } from 'lucide-react';
import toast from 'react-hot-toast';
import { authService } from '@/services/authService';
import { useAuthStore } from '@/store/authStore';
import { Spinner } from '@/components/Spinner';
import { sanitizeInput } from '@/utils/sanitize';

const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (data: LoginForm) => {
    try {
      const sanitized = {
        email: sanitizeInput(data.email),
        password: data.password,
      };
      const res = await authService.login(sanitized);
      setAuth(res.accessToken, res.refreshToken);
      toast.success('Welcome back!');

      // Role-based redirect
      switch (res.role) {
        case 'ADMIN':
          navigate('/admin', { replace: true });
          break;
        case 'TEACHER':
          navigate('/teacher', { replace: true });
          break;
        default:
          navigate('/student', { replace: true });
      }
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Invalid email or password';
      toast.error(message);
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* Left – Branding */}
      <div className="hidden lg:flex lg:w-1/2 items-center justify-center bg-gradient-to-br from-primary-600 to-primary-900 p-12">
        <div className="max-w-md text-center text-white">
          <Brain className="mx-auto h-20 w-20 mb-8 opacity-90" />
          <h1 className="text-4xl font-bold">CBSE AI Tutor</h1>
          <p className="mt-4 text-lg text-primary-100">
            AI-powered learning platform for CBSE curriculum. Ask questions, get answers from your prescribed textbooks.
          </p>
          <div className="mt-8 grid grid-cols-3 gap-4 text-sm text-primary-200">
            <div className="rounded-lg bg-white/10 p-3">
              <p className="text-2xl font-bold text-white">10K+</p>
              <p>Students</p>
            </div>
            <div className="rounded-lg bg-white/10 p-3">
              <p className="text-2xl font-bold text-white">500+</p>
              <p>Books</p>
            </div>
            <div className="rounded-lg bg-white/10 p-3">
              <p className="text-2xl font-bold text-white">99%</p>
              <p>Accuracy</p>
            </div>
          </div>
        </div>
      </div>

      {/* Right – Form */}
      <div className="flex w-full items-center justify-center px-6 lg:w-1/2 bg-white dark:bg-[#0f0f17]">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden flex items-center gap-2 justify-center">
            <Brain className="h-8 w-8 text-primary-600" />
            <span className="text-xl font-bold text-gray-900 dark:text-white">CBSE AI Tutor</span>
          </div>

          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Welcome back</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Sign in to your account to continue learning
          </p>

          <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Email address
              </label>
              <input
                type="email"
                autoComplete="email"
                className="input-field"
                placeholder="you@school.com"
                {...register('email')}
              />
              {errors.email && (
                <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  className="input-field pr-10"
                  placeholder="••••••••"
                  {...register('password')}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1 text-xs text-red-500">{errors.password.message}</p>
              )}
            </div>

            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center gap-2 text-gray-600 dark:text-gray-400">
                <input type="checkbox" className="rounded border-gray-300" />
                Remember me
              </label>
              <Link
                to="/forgot-password"
                className="font-medium text-primary-600 hover:text-primary-500"
              >
                Forgot password?
              </Link>
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full">
              {isSubmitting ? (
                <span className="flex items-center justify-center gap-2">
                  <Spinner size="sm" className="text-white" />
                  Signing in...
                </span>
              ) : (
                'Sign in'
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Don't have an account?{' '}
            <Link to="/register" className="font-medium text-primary-600 hover:text-primary-500">
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
