import { apiClient } from './client';
import type { MutualLoanHistoryResponse, MutualLoanResponse } from './types';

export async function getMutualLoans(familyIds?: number[]): Promise<MutualLoanResponse[]> {
  const { data } = await apiClient.get<MutualLoanResponse[]>('/api/mutual-loans', { params: { familyIds } });
  return data;
}

export async function getMutualLoanHistory(familyIds?: number[]): Promise<MutualLoanHistoryResponse[]> {
  const { data } = await apiClient.get<MutualLoanHistoryResponse[]>('/api/mutual-loans/history', { params: { familyIds } });
  return data;
}
