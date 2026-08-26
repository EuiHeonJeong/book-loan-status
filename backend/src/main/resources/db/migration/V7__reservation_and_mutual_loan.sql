-- 일반예약현황(reservationList.do) / 상호대차현황 신청현황(mutualLoanList.do, type=0) 저장용.
-- docs/issl-site-notes.md "일반예약현황 / 상호대차현황(신청현황) 페이지" 참고.

CREATE TABLE reservation_record (
  id                 BIGSERIAL PRIMARY KEY,
  library_account_id BIGINT NOT NULL REFERENCES library_account(id) ON DELETE CASCADE,
  book_title         VARCHAR(300) NOT NULL,
  branch_name        VARCHAR(20) NOT NULL,
  reserved_at        DATE NOT NULL,
  expires_at         DATE,
  rank               INT,
  status_text        VARCHAR(50) NOT NULL,
  ready_notified_at  TIMESTAMP,
  fetched_at         TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (library_account_id, book_title, reserved_at)
);

CREATE INDEX idx_reservation_record_account ON reservation_record(library_account_id);

CREATE TABLE mutual_loan_record (
  id                 BIGSERIAL PRIMARY KEY,
  library_account_id BIGINT NOT NULL REFERENCES library_account(id) ON DELETE CASCADE,
  book_title         VARCHAR(300) NOT NULL,
  applied_at         DATE NOT NULL,
  branch_name        VARCHAR(20) NOT NULL,
  pickup_branch_name VARCHAR(20) NOT NULL,
  status_text        VARCHAR(50) NOT NULL,
  ready_notified_at  TIMESTAMP,
  fetched_at         TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (library_account_id, book_title, applied_at)
);

CREATE INDEX idx_mutual_loan_record_account ON mutual_loan_record(library_account_id);

COMMENT ON TABLE reservation_record IS '일반예약현황(issl.go.kr reservationList.do)에서 크롤링해온 예약 건. loan_record와 달리 상태가 유지되는 동안 신원(자연키)이 같으면 행을 갱신(upsert)하고, ready_notified_at으로 "대출가능" 알림 중복 발송을 막는다 — 매 동기화마다 전체 삭제 후 재삽입하지 않는다.';
COMMENT ON COLUMN reservation_record.library_account_id IS '이 예약 건이 속한 도서관 계정.';
COMMENT ON COLUMN reservation_record.book_title IS '도서명.';
COMMENT ON COLUMN reservation_record.branch_name IS '소장도서관.';
COMMENT ON COLUMN reservation_record.reserved_at IS '예약일.';
COMMENT ON COLUMN reservation_record.expires_at IS '예약만기일. 사이트에 값이 없을 수 있어 nullable.';
COMMENT ON COLUMN reservation_record.rank IS '예약 순위. 사이트 표기가 숫자가 아닐 수 있어 nullable.';
COMMENT ON COLUMN reservation_record.status_text IS 'issl.go.kr 원문 상태 텍스트 그대로 저장. "대출가능" 판정 문자열이 미확인 상태라(docs/issl-site-notes.md) 원문을 보존해 향후 판정 로직을 조정할 수 있게 한다.';
COMMENT ON COLUMN reservation_record.ready_notified_at IS '이 예약 건에 대해 "대출가능" 푸시를 이미 보냈으면 그 시각. NULL이면 아직 안 보낸 것 — 매 동기화 때마다 재알림하지 않기 위한 플래그.';
COMMENT ON COLUMN reservation_record.fetched_at IS '이 레코드를 마지막으로 크롤링해온 시각.';

COMMENT ON TABLE mutual_loan_record IS '상호대차현황 신청현황(issl.go.kr mutualLoanList.do, type=0 기본값)에서 크롤링해온 신청 건. 이력현황(type=1)은 범위 밖. reservation_record와 동일하게 upsert + ready_notified_at 패턴을 쓴다.';
COMMENT ON COLUMN mutual_loan_record.library_account_id IS '이 신청 건이 속한 도서관 계정.';
COMMENT ON COLUMN mutual_loan_record.book_title IS '도서명.';
COMMENT ON COLUMN mutual_loan_record.applied_at IS '신청일.';
COMMENT ON COLUMN mutual_loan_record.branch_name IS '소장도서관.';
COMMENT ON COLUMN mutual_loan_record.pickup_branch_name IS '수령처(도서를 받을 도서관).';
COMMENT ON COLUMN mutual_loan_record.status_text IS 'issl.go.kr 원문 상태 텍스트 그대로 저장(실계정에서 확인된 값: "대출중"). 이 값이 정확히 "대출 가능해진 시점"인지 "이미 대출 완료된 시점"인지 미확인(docs/issl-site-notes.md) — 향후 재조사 시 판정 로직만 바꿀 수 있도록 원문을 보존한다.';
COMMENT ON COLUMN mutual_loan_record.ready_notified_at IS '이 신청 건에 대해 "대출가능" 푸시를 이미 보냈으면 그 시각. NULL이면 아직 안 보낸 것.';
COMMENT ON COLUMN mutual_loan_record.fetched_at IS '이 레코드를 마지막으로 크롤링해온 시각.';
