import { useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import type { Role } from '@/types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: Role[];
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const location = useLocation();
  const { isAuthenticated, user, checkAuth, logout } = useAuthStore();

  const isValid = checkAuth();

  // If token is expired, schedule logout as a side effect (not during render)
  useEffect(() => {
    if (!isValid && isAuthenticated) {
      logout();
    }
  }, [isValid, isAuthenticated, logout]);

  if (!isAuthenticated || !isValid || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    const rolePath = user.role === 'ADMIN' ? '/admin' : user.role === 'TEACHER' ? '/teacher' : '/student';
    return <Navigate to={rolePath} replace />;
  }

  return <>{children}</>;
}
