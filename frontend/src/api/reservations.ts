import { apiClient } from './client';
import type { ReservationResponse } from './types';

export async function getReservations(familyIds?: number[]): Promise<ReservationResponse[]> {
  const { data } = await apiClient.get<ReservationResponse[]>('/api/reservations', { params: { familyIds } });
  return data;
}
