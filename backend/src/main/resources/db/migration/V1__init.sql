CREATE TABLE app_user (
  id               BIGSERIAL PRIMARY KEY,
  provider         VARCHAR(20) NOT NULL,       -- google | naver
  provider_user_id VARCHAR(100) NOT NULL,
  name             VARCHAR(50),
  created_at       TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (provider, provider_user_id)
);

CREATE TABLE family_member (
  id            BIGSERIAL PRIMARY KEY,
  owner_user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  name          VARCHAR(30) NOT NULL,
  is_self       BOOLEAN NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_family_member_owner ON family_member(owner_user_id);

-- issl.go.kr는 계정 하나로 7개 지점 전체의 대출현황을 조회할 수 있어(docs/issl-site-notes.md),
-- 가족 구성원당 도서관 계정은 하나면 충분하다. 지점은 대출 건별로 loan_record.branch_name에 기록된다.
CREATE TABLE library_account (
  id                 BIGSERIAL PRIMARY KEY,
  family_member_id   BIGINT NOT NULL UNIQUE REFERENCES family_member(id) ON DELETE CASCADE,
  login_id           VARCHAR(20) NOT NULL,
  encrypted_password TEXT NOT NULL,
  last_login_ok      BOOLEAN,
  last_synced_at     TIMESTAMP
);

CREATE TABLE loan_record (
  id                 BIGSERIAL PRIMARY KEY,
  library_account_id BIGINT NOT NULL REFERENCES library_account(id) ON DELETE CASCADE,
  book_title         VARCHAR(300) NOT NULL,
  branch_name        VARCHAR(20) NOT NULL,
  loan_date          DATE NOT NULL,
  due_date           DATE NOT NULL,
  fetched_at         TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (library_account_id, book_title, loan_date)
);

CREATE INDEX idx_loan_record_account ON loan_record(library_account_id);

CREATE TABLE notification_setting (
  id                    BIGSERIAL PRIMARY KEY,
  owner_user_id         BIGINT NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
  due_alert_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
  due_alert_timing      VARCHAR(5) NOT NULL DEFAULT 'd3',
  overdue_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE
);
