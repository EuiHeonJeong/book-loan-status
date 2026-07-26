# 04. 알림 설정

## 목적
반납예정/연체 알림의 발송 여부와 시점을 사용자가 설정. 실제 발송은 브라우저 Web Push로 이루어진다(앱을 열어두지 않아도 옴).

## 요건
- 반납예정 알림: on/off 토글 + 알림 시점 선택(D-3 / D-2 / D-1 / 당일), 다중 선택이 아닌 단일 선택(pill 중 하나 강조). 선택한 날짜에만 1회 발송(누적 반복 아님).
- 이 기기에서 푸시 알림 받기: on/off 토글. 이 기기(브라우저)가 Web Push 구독을 갖고 있는지가 곧 이 토글의 상태 — 별도 서버 설정값이 아니라 브라우저의 `PushManager.getSubscription()` 결과로 판단한다.
  - 연체 알림은 별도 on/off가 없다 — 이 토글이 켜져 있는(=구독이 있는) 한, 연체 도서가 있으면 매일 1회 자동으로 알림이 온다.
  - iOS Safari는 홈 화면에 추가한 PWA에서만 푸시가 오므로(iOS 16.4+) 안내 문구를 카드 하단에 고정 노출.
- 설정은 서비스 계정(owner_user_id) 단위로 저장, 가족 구성원별 개별 설정 아님(전역 설정). 단, 푸시 구독은 기기(브라우저)별로 따로 등록된다 — 한 계정을 여러 기기에서 열면 기기마다 각각 켜야 한다.

## 레이아웃 구성

```
┌───────────────────────────────┐
│ ‹  알림 설정                   │ ← appbar (back 버튼)
├───────────────────────────────┤
│ ┌───────────────────────────┐ │
│ │ 반납예정 알림         (●─) │ │ ← 토글 on
│ │ 알림 시점 [D-3][D-2][D-1][당일] │ │ ← 선택된 pill 강조
│ └───────────────────────────┘ │
│ ┌───────────────────────────┐ │
│ │ 이 기기에서 푸시 알림 받기 (●─) │ │ ← 토글 on = 이 브라우저가 구독 중
│ │ 연체 도서는 매일, 반납예정 도서는 위 설정에 따라 알림 │ │
│ │ iOS 안내 문구                │ │
│ └───────────────────────────┘ │
└───────────────────────────────┘
```

- 토글 스위치: width 40px height 22px, radius pill, on일 때 `background:primary-700`, off일 때 `neutral-300`; 내부 knob 16px 원, on일 때 오른쪽으로 18px 이동
- 알림 시점 pill: 선택 시 `primary-700` 배경 + 흰 글씨, 미선택 `neutral-100` + `neutral-800` 글씨
- 카드: `border-radius:md`, `border-color:border`, padding 16px

## 컴포넌트
- `NotificationSettingsPage`
  - `AppBar` (title, back)
  - 반납예정 알림 카드: `ToggleSwitch` + `TimingPillGroup`
  - 푸시 구독 카드: `ToggleSwitch`(온오프 시 브라우저 Notification 권한 요청 → 구독/해지) + 안내 문구 2줄

## 상태/인터랙션
- 서버 저장값: `dueAlertEnabled: boolean`, `dueAlertTiming: 'd3'|'d2'|'d1'|'d0'`
- 브라우저 상태(서버 저장 아님): 이 기기의 Web Push 구독 여부 — `navigator.serviceWorker.getRegistration()` → `pushManager.getSubscription()`
- 반납예정 카드: 토글/pill 클릭 시 즉시 `PUT /api/notification-settings` 호출(낙관적 업데이트 후 실패 시 롤백)
- 푸시 구독 카드 on: `Notification.requestPermission()` → 거부 시 인라인 에러 → `pushManager.subscribe(...)` → `POST /api/push-subscriptions`
- 푸시 구독 카드 off: `subscription.unsubscribe()` → `DELETE /api/push-subscriptions`
- `dueAlertEnabled=false`일 때 시점 pill 그룹은 비활성(흐림) 처리

## 디자인 토큰
```css
--color-primary-700:#4F2A85;
--color-neutral-100:#F4F3F7; --color-neutral-300:#DEDCE3; --color-neutral-600:#8B8993;
--color-neutral-800:#4A4852; --color-neutral-900:#1F1F24;
--color-border:#E9E7ED;
--font-size-xs:13px; --font-size-sm:15px; --font-size-lg:24px;
--font-weight-bold:700;
--radius-md:10px; --radius-pill:999px;
```

## API 연동
- `GET /api/notification-settings`
- `PUT /api/notification-settings` — body: `{dueAlertEnabled, dueAlertTiming}`
- `POST /api/push-subscriptions` — body: 브라우저 `PushSubscription.toJSON()` 그대로(`{endpoint, keys:{p256dh, auth}}`). endpoint 기준 upsert.
- `DELETE /api/push-subscriptions` — body: `{endpoint}`. 본인 소유가 아니거나 이미 없으면 조용히 무시.

## 발송 파이프라인 (백엔드)
- 매일 정해진 시각(`app.push.schedule.cron`, 기본 08:30 Asia/Seoul)에 `PushNotificationScheduler`가 전체 계정을 돌며:
  1. `LoanSyncService.sync(ownerUserId, null)`로 issl.go.kr 최신 대출현황 동기화
  2. `NotificationDispatchService.dispatchForOwner(ownerUserId)`로 반납예정(정확히 그 D-day) / 연체(매일) 대상을 계산해, 카테고리별로 묶어(최대 하루 2개) `PushNotificationService`가 실제 브라우저 푸시 발송
- 죽은 구독(404/410 응답)은 발송 시점에 자동 삭제.
