import { apiClient } from './client';
import type { PushSubscriptionRequest } from './types';

export async function registerPushSubscription(sub: PushSubscriptionRequest): Promise<void> {
  await apiClient.post('/api/push-subscriptions', sub);
}

export async function unregisterPushSubscription(endpoint: string): Promise<void> {
  await apiClient.delete('/api/push-subscriptions', { data: { endpoint } });
}

export function urlBase64ToUint8Array(base64: string): Uint8Array {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4);
  const base64Safe = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(base64Safe);
  return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}
