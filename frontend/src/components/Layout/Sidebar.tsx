import { Link, useLocation } from 'react-router-dom';
import { cn } from '@/utils/helpers';
import { useAuthStore } from '@/store/authStore';
import {
  LayoutDashboard,
  MessageSquare,
  BookOpen,
  Upload,
  Users,
  BarChart3,
  CreditCard,
  Settings,
  FileText,
  Brain,
  ClipboardList,
  TrendingDown,
  X,
  Sparkles,
} from 'lucide-react';
import type { Role } from '@/types';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  roles: Role[];
}

const navItems: NavItem[] = [
  // Student
  { label: 'Dashboard', path: '/student', icon: <LayoutDashboard size={20} />, roles: ['STUDENT'] },
  { label: 'Ask AI', path: '/student/ask', icon: <MessageSquare size={20} />, roles: ['STUDENT', 'TEACHER', 'ADMIN'] },
  { label: 'Upload Books', path: '/student/upload', icon: <Upload size={20} />, roles: ['STUDENT', 'TEACHER', 'ADMIN'] },
  { label: 'AI Studio', path: '/student/studio', icon: <Sparkles size={20} />, roles: ['STUDENT', 'TEACHER', 'ADMIN'] },
  { label: 'My Questions', path: '/student/history', icon: <ClipboardList size={20} />, roles: ['STUDENT'] },
  { label: 'Mock Tests', path: '/student/tests', icon: <FileText size={20} />, roles: ['STUDENT'] },
  { label: 'Weak Topics', path: '/student/weak-topics', icon: <TrendingDown size={20} />, roles: ['STUDENT'] },
  { label: 'Settings', path: '/student/settings', icon: <Settings size={20} />, roles: ['STUDENT'] },

  // Teacher
  { label: 'Dashboard', path: '/teacher', icon: <LayoutDashboard size={20} />, roles: ['TEACHER'] },
  { label: 'Ask AI', path: '/teacher/ask', icon: <MessageSquare size={20} />, roles: ['TEACHER'] },
  { label: 'Upload Books', path: '/teacher/upload', icon: <Upload size={20} />, roles: ['TEACHER'] },
  { label: 'AI Studio', path: '/teacher/studio', icon: <Sparkles size={20} />, roles: ['TEACHER'] },
  { label: 'Student Usage', path: '/teacher/students', icon: <Users size={20} />, roles: ['TEACHER'] },
  { label: 'Generate Test', path: '/teacher/generate-test', icon: <Brain size={20} />, roles: ['TEACHER'] },
  { label: 'Settings', path: '/teacher/settings', icon: <Settings size={20} />, roles: ['TEACHER'] },

  // Admin
  { label: 'Dashboard', path: '/admin', icon: <LayoutDashboard size={20} />, roles: ['ADMIN'] },
  { label: 'Ask AI', path: '/admin/ask', icon: <MessageSquare size={20} />, roles: ['ADMIN'] },
  { label: 'Upload Books', path: '/admin/upload', icon: <Upload size={20} />, roles: ['ADMIN'] },
  { label: 'AI Studio', path: '/admin/studio', icon: <Sparkles size={20} />, roles: ['ADMIN'] },
  { label: 'Manage Users', path: '/admin/users', icon: <Users size={20} />, roles: ['ADMIN'] },
  { label: 'Books', path: '/admin/books', icon: <BookOpen size={20} />, roles: ['ADMIN'] },
  { label: 'Analytics', path: '/admin/analytics', icon: <BarChart3 size={20} />, roles: ['ADMIN'] },
  { label: 'Billing', path: '/admin/billing', icon: <CreditCard size={20} />, roles: ['ADMIN'] },
  { label: 'Settings', path: '/admin/settings', icon: <Settings size={20} />, roles: ['ADMIN'] },
];

export function Sidebar({ open, onClose }: SidebarProps) {
  const location = useLocation();
  const user = useAuthStore((s) => s.user);

  const filteredItems = navItems.filter((item) => user && item.roles.includes(user.role));

  return (
    <>
      {/* Overlay for mobile */}
      {open && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-gray-200 bg-white dark:border-gray-700 dark:bg-[#1e1e2e] transition-transform duration-300 lg:translate-x-0 lg:static lg:z-auto',
          open ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Logo */}
        <div className="flex h-16 items-center justify-between border-b border-gray-200 px-6 dark:border-gray-700">
          <Link to="/" className="flex items-center gap-2">
            <Brain className="h-7 w-7 text-primary-600" />
            <span className="text-lg font-bold text-gray-900 dark:text-white">
              CBSE AI Tutor
            </span>
          </Link>
          <button onClick={onClose} className="lg:hidden text-gray-500 hover:text-gray-700 dark:text-gray-400">
            <X size={20} />
          </button>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto p-4 space-y-1">
          {filteredItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={onClose}
                className={cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-200'
                )}
              >
                {item.icon}
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* User info */}
        {user && (
          <div className="border-t border-gray-200 p-4 dark:border-gray-700">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-100 text-sm font-semibold text-primary-700 dark:bg-primary-900/40 dark:text-primary-400">
                {user.fullName.charAt(0).toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                  {user.fullName}
                </p>
                <p className="truncate text-xs text-gray-500 dark:text-gray-400">
                  {user.role}
                </p>
              </div>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}
