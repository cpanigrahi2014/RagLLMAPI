import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role, User } from '@/types';
import { decodeToken, isTokenExpired, getTokenExpiryMs } from '@/utils/jwtUtils';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  _logoutTimer: ReturnType<typeof setTimeout> | null;

  setAuth: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
  checkAuth: () => boolean;
  hasRole: (...roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      _logoutTimer: null,

      setAuth: (accessToken: string, refreshToken: string) => {
        const payload = decodeToken(accessToken);
        if (!payload) return;

        const user: User = {
          userId: payload.sub,
          email: payload.email,
          fullName: payload.fullName ?? payload.email,
          role: payload.role,
          tenantId: payload.tenantId,
          tenantName: payload.tenantName ?? '',
        };

        // Clear any existing timer
        const existingTimer = get()._logoutTimer;
        if (existingTimer) clearTimeout(existingTimer);

        // Set auto-logout timer
        const expiryMs = getTokenExpiryMs(accessToken);
        const timer = setTimeout(() => {
          get().logout();
        }, Math.max(expiryMs, 0));

        set({
          accessToken,
          refreshToken,
          user,
          isAuthenticated: true,
          _logoutTimer: timer,
        });
      },

      logout: () => {
        const timer = get()._logoutTimer;
        if (timer) clearTimeout(timer);

        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
          _logoutTimer: null,
        });
      },

      checkAuth: () => {
        const { accessToken, isAuthenticated } = get();
        if (!accessToken || isTokenExpired(accessToken)) {
          return false;
        }
        return isAuthenticated;
      },

      hasRole: (...roles: Role[]) => {
        const { user } = get();
        return user !== null && roles.includes(user.role);
      },
    }),
    {
      name: 'ragllm-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
