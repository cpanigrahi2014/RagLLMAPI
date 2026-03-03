import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { AppLayout } from '@/components/Layout/AppLayout';
import { FullPageSpinner } from '@/components/Spinner';
import { useAuthStore } from '@/store/authStore';

// Lazy-loaded pages
const LoginPage = lazy(() => import('@/pages/Login'));
const RegisterPage = lazy(() => import('@/pages/Register'));
const ForgotPasswordPage = lazy(() => import('@/pages/ForgotPassword'));

// Student pages
const StudentDashboard = lazy(() => import('@/pages/student/Dashboard'));
const AskAIPage = lazy(() => import('@/pages/student/AskAI'));
const QuestionHistory = lazy(() => import('@/pages/student/QuestionHistory'));
const MockTests = lazy(() => import('@/pages/student/MockTests'));
const WeakTopics = lazy(() => import('@/pages/student/WeakTopics'));
const AIStudioPage = lazy(() => import('@/pages/student/AIStudio'));

// Teacher pages
const TeacherDashboard = lazy(() => import('@/pages/teacher/Dashboard'));
const UploadBookPage = lazy(() => import('@/pages/teacher/UploadBook'));
const StudentUsagePage = lazy(() => import('@/pages/teacher/StudentUsage'));
const GenerateTestPage = lazy(() => import('@/pages/teacher/GenerateTest'));

// Admin pages
const AdminDashboard = lazy(() => import('@/pages/admin/Dashboard'));
const ManageUsersPage = lazy(() => import('@/pages/admin/ManageUsers'));
const BooksPage = lazy(() => import('@/pages/admin/Books'));
const AnalyticsPage = lazy(() => import('@/pages/admin/Analytics'));
const BillingPage = lazy(() => import('@/pages/admin/Billing'));
const SettingsPage = lazy(() => import('@/pages/admin/Settings'));

function SuspenseWrapper({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<FullPageSpinner />}>{children}</Suspense>;
}

// Role-based root redirect
function RootRedirect() {
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  if (!isAuthenticated || !user) return <Navigate to="/login" replace />;

  switch (user.role) {
    case 'ADMIN':
      return <Navigate to="/admin" replace />;
    case 'TEACHER':
      return <Navigate to="/teacher" replace />;
    default:
      return <Navigate to="/student" replace />;
  }
}

const router = createBrowserRouter([
  // Public routes
  {
    path: '/login',
    element: <SuspenseWrapper><LoginPage /></SuspenseWrapper>,
  },
  {
    path: '/register',
    element: <SuspenseWrapper><RegisterPage /></SuspenseWrapper>,
  },
  {
    path: '/forgot-password',
    element: <SuspenseWrapper><ForgotPasswordPage /></SuspenseWrapper>,
  },

  // Root redirect
  {
    path: '/',
    element: <RootRedirect />,
  },

  // Student routes
  {
    element: (
      <ProtectedRoute allowedRoles={['STUDENT']}>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { path: '/student', element: <SuspenseWrapper><StudentDashboard /></SuspenseWrapper> },
      { path: '/student/ask', element: <SuspenseWrapper><AskAIPage /></SuspenseWrapper> },
      { path: '/student/upload', element: <SuspenseWrapper><UploadBookPage /></SuspenseWrapper> },
      { path: '/student/studio', element: <SuspenseWrapper><AIStudioPage /></SuspenseWrapper> },
      { path: '/student/history', element: <SuspenseWrapper><QuestionHistory /></SuspenseWrapper> },
      { path: '/student/tests', element: <SuspenseWrapper><MockTests /></SuspenseWrapper> },
      { path: '/student/weak-topics', element: <SuspenseWrapper><WeakTopics /></SuspenseWrapper> },
      { path: '/student/settings', element: <SuspenseWrapper><SettingsPage /></SuspenseWrapper> },
    ],
  },

  // Teacher routes
  {
    element: (
      <ProtectedRoute allowedRoles={['TEACHER']}>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { path: '/teacher', element: <SuspenseWrapper><TeacherDashboard /></SuspenseWrapper> },
      { path: '/teacher/ask', element: <SuspenseWrapper><AskAIPage /></SuspenseWrapper> },
      { path: '/teacher/upload', element: <SuspenseWrapper><UploadBookPage /></SuspenseWrapper> },
      { path: '/teacher/studio', element: <SuspenseWrapper><AIStudioPage /></SuspenseWrapper> },
      { path: '/teacher/students', element: <SuspenseWrapper><StudentUsagePage /></SuspenseWrapper> },
      { path: '/teacher/generate-test', element: <SuspenseWrapper><GenerateTestPage /></SuspenseWrapper> },
      { path: '/teacher/settings', element: <SuspenseWrapper><SettingsPage /></SuspenseWrapper> },
    ],
  },

  // Admin routes
  {
    element: (
      <ProtectedRoute allowedRoles={['ADMIN']}>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { path: '/admin', element: <SuspenseWrapper><AdminDashboard /></SuspenseWrapper> },
      { path: '/admin/ask', element: <SuspenseWrapper><AskAIPage /></SuspenseWrapper> },
      { path: '/admin/upload', element: <SuspenseWrapper><UploadBookPage /></SuspenseWrapper> },
      { path: '/admin/studio', element: <SuspenseWrapper><AIStudioPage /></SuspenseWrapper> },
      { path: '/admin/users', element: <SuspenseWrapper><ManageUsersPage /></SuspenseWrapper> },
      { path: '/admin/books', element: <SuspenseWrapper><BooksPage /></SuspenseWrapper> },
      { path: '/admin/analytics', element: <SuspenseWrapper><AnalyticsPage /></SuspenseWrapper> },
      { path: '/admin/billing', element: <SuspenseWrapper><BillingPage /></SuspenseWrapper> },
      { path: '/admin/settings', element: <SuspenseWrapper><SettingsPage /></SuspenseWrapper> },
    ],
  },

  // Catch-all
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
]);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,      // 5 min
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              borderRadius: '10px',
              background: '#333',
              color: '#fff',
              fontSize: '14px',
            },
          }}
        />
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
