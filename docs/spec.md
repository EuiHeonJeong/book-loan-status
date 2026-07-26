# 개발 스펙

## 1. 시스템 아키텍처

```
woori-library/                      ← 모노레포 루트
├── frontend/    (React + TS + Vite)
├── backend/     (Spring Boot 3.x)
└── crawler/     (Playwright-Java, backend에 모듈로 포함 가능)

[React SPA] ──HTTPS/JSON──▶ [Spring Boot API] ──JDBC──▶ [PostgreSQL]
      ▲                            │
      │                            ├─▶ [Playwright(Java) 크롤링 모듈] ──▶ issl.go.kr
      └──────Web Push──────────────┴─▶ [PushNotificationScheduler(Spring @Scheduled, 매일 1회)] ──▶ 브라우저 Push 서비스(FCM/Mozilla/APNs)
```

## 2. 기술 스택

| 영역 | 스택 | 비고 |
|---|---|---|
| Frontend | React 18 + TypeScript 5 + Vite 5 | 디자인 토큰은 화면별 문서 참고, CSS 변수로 이식 |
| 상태관리 | React 내장 (useState/useReducer) | 화면 4개 규모라 Redux 등 불필요 |
| 라우팅 | react-router-dom | 로그인/메인/가족등록/알림설정 4-route |
| Backend | Spring Boot 3.x (Java 21) | Web, Validation, Data JPA, Security(OAuth2 Client) |
| DB | PostgreSQL 17 | 로컬 개발은 관리자 권한이 필요 없는 portable 바이너리(zip)로 `.local/pgsql`에 설치 |
| 크롤링 | Playwright for Java | Chromium headless, 세션 쿠키 재사용 |
| 인증(서비스) | OAuth2 (Google/Naver) | Spring Security OAuth2 Client |
| 암호화 | AES-256-GCM | 도서관 계정 비밀번호 저장용 |
| 푸시 알림 | Web Push (VAPID) + `nl.martijndwars:web-push` | 브라우저 구독을 `push_subscription`에 저장, 매일 스케줄러가 발송 |
| 인프라 | 로컬: portable PostgreSQL / 배포: 별도 결정 필요 | Docker는 아직 미도입 |

## 3. 프로젝트 구조

```
backend/
  src/main/java/com/woori/library/
    domain/           # FamilyMember, LibraryAccount, LoanRecord (Entity)
    repository/       # JPA Repository
    service/
      crawler/        # IsslLoginService, IsslLoanScrapeService
      crypto/         # AesGcmCipherService
      loan/           # LoanAggregationService
    controller/       # REST Controller
    config/           # SecurityConfig, OAuth2Config
    dto/
  src/main/resources/
    application.yml
    db/migration/     # Flyway 마이그레이션 SQL

frontend/
  src/
    pages/            # LoginPage, DashboardPage, FamilyPage, NotificationSettingsPage
    components/       # LoanCard, FilterPanel, MemberCard, NotifToggle ...
    api/              # axios 클라이언트, 타입
    styles/tokens/    # colors.css, typography.css, spacing.css, effects.css
```

## 4. DB 스키마 (DDL)

실제 마이그레이션: `backend/src/main/resources/db/migration/V1__init.sql`(초기 스키마), `V2__push_subscription.sql`(Web Push 구독), `V3__drop_overdue_alert_enabled.sql`(연체 알림 on/off를 별도 컬럼 대신 구독 존재 여부로 대체). 최초 스펙 초안에는 없었지만, OAuth2 로그인 계정을 실제로 연결하려면 `owner_user_id`가 참조할 사용자 테이블이 필요해서 `app_user`를 추가했다.

```sql
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

-- issl.go.kr는 계정 하나로 7개 지점 전체 대출현황을 조회할 수 있어(docs/issl-site-notes.md),
-- 가족 구성원당 도서관 계정은 하나면 충분하다. 지점(검암/아라누리/단봉늘봄/검단/심곡/석남/신석)은
-- 대출 건별로 다를 수 있어(상호대차) loan_record.branch_name에 건별로 기록한다.
CREATE TABLE library_account (
  id                 BIGSERIAL PRIMARY KEY,
  family_member_id   BIGINT NOT NULL UNIQUE REFERENCES family_member(id) ON DELETE CASCADE,
  login_id           VARCHAR(20) NOT NULL,
  encrypted_password TEXT NOT NULL,          -- AES-256-GCM (base64: iv+ciphertext+tag)
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
  UNIQUE (library_account_id, book_title, loan_date)  -- 중복 크롤링 방지
);

CREATE TABLE notification_setting (
  id                    BIGSERIAL PRIMARY KEY,
  owner_user_id         BIGINT NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
  due_alert_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
  due_alert_timing      VARCHAR(5) NOT NULL DEFAULT 'd3'  -- d3 | d2 | d1 | d0
  -- 연체 알림은 별도 컬럼 없음 — push_subscription 존재 여부로 게이팅(V3 마이그레이션)
);

-- 브라우저 Push API 구독. endpoint는 브라우저+기기+오리진마다 전역 유일.
CREATE TABLE push_subscription (
  id            BIGSERIAL PRIMARY KEY,
  owner_user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  endpoint      TEXT NOT NULL UNIQUE,
  p256dh        VARCHAR(255) NOT NULL,
  auth          VARCHAR(255) NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);
```

## 5. REST API 명세

| Method | Endpoint | 설명 | Request | Response |
|---|---|---|---|---|
| GET | `/api/auth/me` | 로그인된 서비스 계정 정보 | - | `{userId, name, provider}` |
| GET | `/api/members` | 가족 구성원 + 도서관 계정 목록 | - | `[{id,name,isSelf,libraryAccounts:[{id,loginId}]}]` |
| POST | `/api/members` | 가족 구성원 추가 | `{name}` | `{id,name}` |
| PUT | `/api/members/{id}` | 구성원 이름 수정 | `{name}` | `{id,name,...}` |
| DELETE | `/api/members/{id}` | 구성원 삭제 (isSelf 불가) | - | 204 |
| POST | `/api/members/{id}/library-accounts` | 도서관 계정 등록 | `{loginId,password}` | `{id, loginId}` (비밀번호 즉시 크롤링 검증 후 저장) |
| PUT | `/api/library-accounts/{id}` | 도서관 계정 수정 | `{loginId?,password?}` | `{id,...}` |
| DELETE | `/api/library-accounts/{id}` | 도서관 계정 삭제 | - | 204 |
| GET | `/api/loans` | 대출현황 조회 (필터/정렬) | query: `familyIds[], libraryCodes[], sort=due\|loan, dir=asc\|desc` | `[{title,loanDate,dueDate,library,memberName,dday,overdue}]` |
| POST | `/api/loans/sync` | 전체(또는 특정 계정) 즉시 재크롤링 | `{libraryAccountId?}` | `{synced: n, failed: [...]}` |
| GET | `/api/notifications` | 반납임박/연체 알림 목록 | - | `[{title,dueDate,library,memberName,dday,overdue}]` |
| GET/PUT | `/api/notification-settings` | 알림 설정 조회/변경 | `{dueAlertEnabled,dueAlertTiming}` | 동일 |
| POST | `/api/push-subscriptions` | 이 기기의 Web Push 구독 등록(endpoint 기준 upsert) | 브라우저 `PushSubscription.toJSON()` | 204 |
| DELETE | `/api/push-subscriptions` | 이 기기의 Web Push 구독 해지 | `{endpoint}` | 204 |

## 6. 크롤링 서비스 스펙 (Playwright-Java)

```
IsslLoginService.login(loginId, password) -> BrowserContext(세션 쿠키 포함)
  1) UA를 실제 Chrome로 설정 (UA 필터링 우회 필요, docs/issl-site-notes.md 참고)
  2) POST /mbr/loginChkPro.do (JSON) 로 자격증명 확인 → result Y 확인
  3) 성공 시 /mbr/loginPro.do form-submit → 세션 확립
  4) 실패(N) 시 IsslAuthException 발생 → library_account.last_login_ok=false 기록

IsslLoanScrapeService.fetchLoans(context) -> List<LoanRecordDto>
  - 대출이력 페이지 URL/셀렉터는 미확인 → 추가 조사 필요 (docs/issl-site-notes.md의 "미확인" 항목)
  - 페이지네이션 있으면 전체 순회
```

크롤링은 계정 단위로 try-catch 하여 한 계정이 실패해도 서비스 전체가 죽지 않게 한다. 실패한 계정은 `last_login_ok=false`로 표시하고 나머지는 정상 진행.

## 7. 보안 스펙

- 도서관 계정 비밀번호: `AesGcmCipherService` — AES-256-GCM, IV는 매번 랜덤 생성 후 ciphertext 앞에 prefix, 키는 `LIBRARY_PW_ENC_KEY` 환경변수(32byte, base64)로 주입. `.env`/`.gitignore` 처리, 저장소 커밋 금지.
- 서비스 로그인은 OAuth2(Google/Naver)만 사용 — 자체 비밀번호 보관 없음.
- 세션: Spring Security 세션 쿠키(`HttpOnly`, `Secure`, `SameSite=Lax`).
- CSRF: `CookieCsrfTokenRepository.withHttpOnlyFalse()` — SPA가 `XSRF-TOKEN` 쿠키를 읽어 `X-XSRF-TOKEN` 헤더로 되돌려 보내는 표준 방식. 세션 쿠키 인증이라 CSRF를 끄지 않는다.
- CORS: 개발 중에는 `http://localhost:5173`만 허용(`SecurityConfig.corsConfigurationSource`), 자격 증명 포함(`allowCredentials=true`).
- google/naver는 사용자 정보 응답 구조가 서로 달라 `CustomOAuth2UserService`가 provider별로 `provider_user_id`/`name`을 정규화한다. 최초 로그인 시 `app_user` + 본인 `family_member`(`is_self=true`) + 기본 `notification_setting`을 함께 생성한다.

## 8. 환경변수 (`.env` 예시)

```
DB_URL=jdbc:postgresql://localhost:5432/woori_library
DB_USER=...
DB_PASSWORD=...
LIBRARY_PW_ENC_KEY=<base64 32byte key>
OAUTH_GOOGLE_CLIENT_ID=...
OAUTH_GOOGLE_CLIENT_SECRET=...
OAUTH_NAVER_CLIENT_ID=...
OAUTH_NAVER_CLIENT_SECRET=...
OAUTH_KAKAO_CLIENT_ID=...
OAUTH_KAKAO_CLIENT_SECRET=...
```

## 9. 화면 ↔ API 매핑

| 화면 | 호출 API |
|---|---|
| 로그인 | OAuth2 리다이렉트 → `/api/auth/me` |
| 현황조회(메인) | `GET /api/loans`, `GET /api/notifications` |
| 가족등록 | `GET/POST/DELETE /api/members`, `POST/PUT/DELETE /api/library-accounts` |
| 알림설정 | `GET/PUT /api/notification-settings`, `POST/DELETE /api/push-subscriptions` |

## 10. 개발 순서

1. Playwright 로그인→대출이력 흐름 스크립트로 검증 (단일 계정)
2. Spring Boot 크롤링 서비스로 이식
3. DB 스키마 + 계정 등록 API
4. 프론트엔드 (핸드오프된 디자인 그대로, 필터/정렬 로직 포함)
5. Web Push: VAPID 키, `push_subscription` 테이블, 매일 스케줄러(`PushNotificationScheduler`)로 반납예정/연체 알림 발송

화면별 상세 요건과 레이아웃은 [screens/](screens/) 폴더 참고.
