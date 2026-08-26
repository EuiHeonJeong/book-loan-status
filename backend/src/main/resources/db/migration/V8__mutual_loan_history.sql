-- 상호대차현황 이력현황(mutualLoanList.do, type=1) 저장용. docs/issl-site-notes.md
-- "상호대차현황 이력현황 탭" 참고. 신청현황(mutual_loan_record)과 달리 이미 종결된 건이라
-- ready_notified_at 같은 알림 상태 관리가 필요 없는 단순 스냅샷 — loan_record와 동일하게
-- 동기화할 때마다 계정 몫을 전체 재작성한다.

CREATE TABLE mutual_loan_history_record (
  id                 BIGSERIAL PRIMARY KEY,
  library_account_id BIGINT NOT NULL REFERENCES library_account(id) ON DELETE CASCADE,
  book_title         VARCHAR(300) NOT NULL,
  applied_at         DATE NOT NULL,
  branch_name        VARCHAR(20) NOT NULL,
  pickup_branch_name VARCHAR(20) NOT NULL,
  status_text        VARCHAR(50) NOT NULL,
  fetched_at         TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (library_account_id, book_title, applied_at)
);

CREATE INDEX idx_mutual_loan_history_record_account ON mutual_loan_history_record(library_account_id);

COMMENT ON TABLE mutual_loan_history_record IS '상호대차현황 이력현황(issl.go.kr mutualLoanList.do, type=1)에서 크롤링해온, 이미 종결된 신청 건. mutual_loan_record(신청현황)와 달리 "대출가능" 알림 대상이 아니라 ready_notified_at이 없고, 매 동기화마다 계정 몫을 전체 삭제 후 재삽입하는 단순 스냅샷이다.';
COMMENT ON COLUMN mutual_loan_history_record.library_account_id IS '이 이력이 속한 도서관 계정.';
COMMENT ON COLUMN mutual_loan_history_record.book_title IS '도서명.';
COMMENT ON COLUMN mutual_loan_history_record.applied_at IS '신청일.';
COMMENT ON COLUMN mutual_loan_history_record.branch_name IS '소장도서관.';
COMMENT ON COLUMN mutual_loan_history_record.pickup_branch_name IS '수령처.';
COMMENT ON COLUMN mutual_loan_history_record.status_text IS '원문 상태 텍스트(실계정 확인된 값: "완료"). 신청현황 탭과 달리 "취소" 컬럼이 없어 취소 사유 등은 저장하지 않는다.';
COMMENT ON COLUMN mutual_loan_history_record.fetched_at IS '이 레코드를 마지막으로 크롤링해온 시각.';
