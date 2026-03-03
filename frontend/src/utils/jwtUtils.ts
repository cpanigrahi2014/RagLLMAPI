import type { Role } from '@/types';

interface JwtPayload {
  sub: string;
  email: string;
  role: Role;
  tenantId: string;
  tenantName?: string;
  fullName?: string;
  exp: number;
  iat: number;
}

export function decodeToken(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload as JwtPayload;
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeToken(token);
  if (!payload) return true;
  // Add 30-second buffer
  return Date.now() >= (payload.exp * 1000) - 30000;
}

export function getTokenExpiryMs(token: string): number {
  const payload = decodeToken(token);
  if (!payload) return 0;
  return (payload.exp * 1000) - Date.now();
}
