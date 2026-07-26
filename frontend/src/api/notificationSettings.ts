import { apiClient } from './client';
import type { NotificationSettingRequest, NotificationSettingResponse } from './types';

export async function getNotificationSettings(): Promise<NotificationSettingResponse> {
  const { data } = await apiClient.get<NotificationSettingResponse>('/api/notification-settings');
  return data;
}

export async function updateNotificationSettings(
  request: NotificationSettingRequest
): Promise<NotificationSettingResponse> {
  const { data } = await apiClient.put<NotificationSettingResponse>('/api/notification-settings', request);
  return data;
}
