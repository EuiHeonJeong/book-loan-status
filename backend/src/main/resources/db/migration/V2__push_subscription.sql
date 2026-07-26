-- 브라우저 Push API 구독 정보. endpoint는 브라우저+기기+오리진마다 전역 유일한 URL이라
-- owner_user_id 범위가 아니라 테이블 전체에서 UNIQUE로 둔다(같은 브라우저가 재구독하면 upsert).
CREATE TABLE push_subscription (
  id            BIGSERIAL PRIMARY KEY,
  owner_user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  endpoint      TEXT NOT NULL UNIQUE,
  p256dh        VARCHAR(255) NOT NULL,
  auth          VARCHAR(255) NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_push_subscription_owner ON push_subscription(owner_user_id);
