-- 연체 알림은 더 이상 별도 on/off가 없다 — "이 기기에서 푸시 알림 받기"(push_subscription 존재 여부)만으로
-- 게이팅한다. 구독이 있는 한 연체 도서는 매일 자동으로 알림 대상이다.
ALTER TABLE notification_setting DROP COLUMN overdue_alert_enabled;
