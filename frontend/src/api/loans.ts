import { apiClient } from './client';
import type { LoanResponse, SyncResponse } from './types';

export interface LoanQuery {
  familyIds?: number[];
  libraryCodes?: string[];
  sort?: 'due' | 'loan';
  dir?: 'asc' | 'desc';
}

export async function getLoans(query: LoanQuery = {}): Promise<LoanResponse[]> {
  const { data } = await apiClient.get<LoanResponse[]>('/api/loans', { params: query });
  return data;
}

export async function syncLoans(): Promise<SyncResponse> {
  const { data } = await apiClient.post<SyncResponse>('/api/loans/sync');
  return data;
}
