-- 스키마 문서화용 — 동작에는 영향 없음(테이블/컬럼 COMMENT만 추가).

COMMENT ON TABLE app_user IS '서비스 로그인 계정(OAuth2). 도서관 계정과는 별개로, 이 앱에 로그인하는 주체.';
COMMENT ON COLUMN app_user.provider IS '로그인 제공자. google 또는 naver.';
COMMENT ON COLUMN app_user.provider_user_id IS '제공자 쪽 고유 사용자 ID.';
COMMENT ON COLUMN app_user.name IS '표시용 이름(제공자에서 받아온 값).';
COMMENT ON COLUMN app_user.created_at IS '최초 로그인(가입) 시각.';

COMMENT ON TABLE family_member IS '대출 현황을 조회할 가족 구성원. 서비스 계정(app_user) 1개당 여러 명 등록 가능.';
COMMENT ON COLUMN family_member.owner_user_id IS '이 구성원을 등록한 서비스 계정.';
COMMENT ON COLUMN family_member.name IS '구성원 이름(가족 목록 표시용, 도서관 로그인 아이디와 무관).';
COMMENT ON COLUMN family_member.is_self IS '로그인한 본인 여부 — 가입 시 기본으로 생성되는 항목 구분용.';
COMMENT ON COLUMN family_member.created_at IS '구성원 등록 시각.';

COMMENT ON TABLE library_account IS '가족 구성원 1명당 issl.go.kr(인천서구구립도서관) 로그인 계정. 계정 하나로 7개 지점 전체를 조회할 수 있어 구성원당 1개만 허용한다(family_member_id UNIQUE).';
COMMENT ON COLUMN library_account.family_member_id IS '이 도서관 계정이 속한 가족 구성원.';
COMMENT ON COLUMN library_account.login_id IS 'issl.go.kr 로그인 아이디.';
COMMENT ON COLUMN library_account.encrypted_password IS 'AES-256-GCM으로 암호화된 도서관 계정 비밀번호. 평문 저장 금지.';
COMMENT ON COLUMN library_account.last_login_ok IS '마지막 자동 로그인 성공 여부.';
COMMENT ON COLUMN library_account.last_synced_at IS '마지막으로 대출현황을 동기화한 시각.';

COMMENT ON TABLE loan_record IS '도서관 계정별로 크롤링해온 대출 중인 도서 목록. 동기화할 때마다 해당 계정 몫이 전체 재작성되는 스냅샷.';
COMMENT ON COLUMN loan_record.library_account_id IS '이 대출 건이 속한 도서관 계정.';
COMMENT ON COLUMN loan_record.book_title IS '도서명.';
COMMENT ON COLUMN loan_record.branch_name IS '대출한 지점명(issl.go.kr 소속 지점).';
COMMENT ON COLUMN loan_record.loan_date IS '대출일.';
COMMENT ON COLUMN loan_record.due_date IS '반납예정일.';
COMMENT ON COLUMN loan_record.fetched_at IS '이 레코드를 크롤링해온 시각.';

COMMENT ON TABLE notification_setting IS '서비스 계정 단위 알림 설정. 가족 구성원별 개별 설정이 아니라 계정당 1행(owner_user_id UNIQUE)인 전역 설정.';
COMMENT ON COLUMN notification_setting.owner_user_id IS '이 설정의 소유 서비스 계정.';
COMMENT ON COLUMN notification_setting.due_alert_enabled IS '반납예정 알림 on/off.';
COMMENT ON COLUMN notification_setting.due_alert_timing IS '반납예정 알림을 보낼 시점. d3|d2|d1|d0(반납예정일 기준 D-3~당일), 정확히 그 날짜에 한 번만 발송된다.';

COMMENT ON TABLE push_subscription IS '브라우저 Web Push 구독 정보(기기별). 이 테이블에 구독이 있으면 "이 기기에서 푸시 알림 받기"가 켜진 상태 — 연체 알림은 별도 on/off 없이 구독 존재 여부만으로 게이팅된다.';
COMMENT ON COLUMN push_subscription.owner_user_id IS '이 구독을 등록한 서비스 계정.';
COMMENT ON COLUMN push_subscription.endpoint IS '브라우저/기기별 푸시 발송 대상 URL. 전역 유일(같은 브라우저 재구독 시 upsert).';
COMMENT ON COLUMN push_subscription.p256dh IS 'Web Push 페이로드 암호화용 공개키(구독 시 브라우저가 발급).';
COMMENT ON COLUMN push_subscription.auth IS 'Web Push 페이로드 암호화용 인증 시크릿(구독 시 브라우저가 발급).';
COMMENT ON COLUMN push_subscription.created_at IS '구독 등록 시각.';
