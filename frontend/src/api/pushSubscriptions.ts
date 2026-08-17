import { apiClient } from './client';
import type { PushSubscriptionRequest } from './types';

export async function registerPushSubscription(sub: PushSubscriptionRequest): Promise<void> {
  await apiClient.post('/api/push-subscriptions', sub);
}

export async function unregisterPushSubscription(endpoint: string): Promise<void> {
  await apiClient.delete('/api/push-subscriptions', { data: { endpoint } });
}

export function urlBase64ToUint8Array(base64: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4);
  const base64Safe = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(base64Safe);
  const array = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) {
    array[i] = raw.charCodeAt(i);
  }
  return array;
}
