import { apiClient, API_BASE_URL } from './client';
import type { MeResponse } from './types';

export type OAuthProvider = 'google' | 'naver';

// Spring Security의 oauth2Login이 처리하는 전체 페이지 리다이렉트 — axios 호출이 아니라 location 이동.
export function oauthLoginUrl(provider: OAuthProvider): string {
  return `${API_BASE_URL}/oauth2/authorization/${provider}`;
}

export async function fetchMe(): Promise<MeResponse> {
  const { data } = await apiClient.get<MeResponse>('/api/auth/me');
  return data;
}

export async function logout(): Promise<void> {
  await apiClient.post('/api/auth/logout');
}
