import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppBar } from '../components/AppBar';
import { BackIcon } from '../components/icons';
import { ToggleSwitch } from '../components/ToggleSwitch';
import { getNotificationSettings, updateNotificationSettings } from '../api/notificationSettings';
import { registerPushSubscription, unregisterPushSubscription, urlBase64ToUint8Array } from '../api/pushSubscriptions';
import type { NotificationSettings } from '../types';

const TIMING_OPTIONS = [
  { value: 'd3', label: 'D-3' },
  { value: 'd2', label: 'D-2' },
  { value: 'd1', label: 'D-1' },
  { value: 'd0', label: '당일' },
] as const;

const PUSH_SUPPORTED = 'serviceWorker' in navigator && 'PushManager' in window;

export function NotificationSettingsPage() {
  const navigate = useNavigate();
  const [settings, setSettings] = useState<NotificationSettings>({
    dueAlertEnabled: true,
    dueAlertTiming: 'd3',
  });
  const [pushEnabled, setPushEnabled] = useState(false);
  const [pushBusy, setPushBusy] = useState(false);
  const [pushError, setPushError] = useState<string | null>(null);

  useEffect(() => {
    getNotificationSettings().then(setSettings);
    if (!PUSH_SUPPORTED) return;
    navigator.serviceWorker.getRegistration().then(async (registration) => {
      const subscription = await registration?.pushManager.getSubscription();
      setPushEnabled(Boolean(subscription));
    });
  }, []);

  // 낙관적 업데이트 후 PUT — 실패하면 이전 값으로 롤백.
  const update = (patch: Partial<NotificationSettings>) => {
    const previous = settings;
    const next = { ...settings, ...patch };
    setSettings(next);
    updateNotificationSettings(patch).catch(() => setSettings(previous));
  };

  const enablePush = async () => {
    setPushError(null);
    if (!PUSH_SUPPORTED) {
      setPushError('이 브라우저는 푸시 알림을 지원하지 않아요.');
      return;
    }
    setPushBusy(true);
    try {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        setPushError('브라우저 알림 권한이 필요해요.');
        return;
      }
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(import.meta.env.VITE_VAPID_PUBLIC_KEY),
      });
      await registerPushSubscription(subscription.toJSON() as { endpoint: string; keys: { p256dh: string; auth: string } });
      setPushEnabled(true);
    } catch {
      setPushError('푸시 알림을 켜지 못했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setPushBusy(false);
    }
  };

  const disablePush = async () => {
    setPushError(null);
    setPushBusy(true);
    try {
      const registration = await navigator.serviceWorker.getRegistration();
      const subscription = await registration?.pushManager.getSubscription();
      if (subscription) {
        await subscription.unsubscribe();
        await unregisterPushSubscription(subscription.endpoint);
      }
      setPushEnabled(false);
    } catch {
      setPushError('푸시 알림을 끄지 못했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setPushBusy(false);
    }
  };

  const togglePush = () => {
    if (pushBusy) return;
    if (pushEnabled) {
      disablePush();
    } else {
      enablePush();
    }
  };

  return (
    <div className="phone">
      <AppBar
        title="알림 설정"
        leading={
          <button className="icon-btn" onClick={() => navigate(-1)}>
            <BackIcon />
          </button>
        }
      />
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', padding: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)' }}>반납예정 알림</span>
            <ToggleSwitch on={settings.dueAlertEnabled} onToggle={() => update({ dueAlertEnabled: !settings.dueAlertEnabled })} />
          </div>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              fontSize: 'var(--font-size-xs)',
              color: 'var(--color-neutral-600)',
              opacity: settings.dueAlertEnabled ? 1 : 0.4,
              pointerEvents: settings.dueAlertEnabled ? 'auto' : 'none',
            }}
          >
            <span>알림 시점</span>
            {TIMING_OPTIONS.map((t) => (
              <button
                key={t.value}
                className={`pill-btn${settings.dueAlertTiming === t.value ? ' selected' : ''}`}
                onClick={() => update({ dueAlertTiming: t.value })}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', padding: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)' }}>
              이 기기에서 푸시 알림 받기
            </span>
            <ToggleSwitch on={pushEnabled} onToggle={togglePush} busy={pushBusy} />
          </div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>
            연체 도서는 매일, 반납예정 도서는 위 설정에 따라 이 기기로 알림이 와요.
          </div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>
            iOS(아이폰)에서는 Safari 공유 메뉴 → 홈 화면에 추가 후에만 알림을 받을 수 있어요.
          </div>
          {pushError && <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-accent-red)' }}>{pushError}</div>}
        </div>
      </div>
    </div>
  );
}
