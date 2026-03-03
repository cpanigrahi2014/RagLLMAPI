import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import type { Role } from '@/types';

export function useAuth() {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout: storeLogout, hasRole, checkAuth } = useAuthStore();

  const logout = useCallback(() => {
    storeLogout();
    navigate('/login', { replace: true });
  }, [storeLogout, navigate]);

  const getHomePath = useCallback((): string => {
    if (!user) return '/login';
    switch (user.role) {
      case 'ADMIN':
        return '/admin';
      case 'TEACHER':
        return '/teacher';
      case 'STUDENT':
        return '/student';
      default:
        return '/login';
    }
  }, [user]);

  const isRole = useCallback((...roles: Role[]) => hasRole(...roles), [hasRole]);

  return {
    user,
    isAuthenticated,
    logout,
    getHomePath,
    isRole,
    checkAuth,
  };
}
