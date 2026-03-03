import api from './api';
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest } from '@/types';

export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', data);
    return res.data.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', data);
    return res.data.data;
  },

  health: async (): Promise<string> => {
    const res = await api.get('/auth/health');
    return res.data;
  },
};
