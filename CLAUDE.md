# 우리서재 (Woori Library)

인천서구구립도서관(issl.go.kr)은 가족회원이어도 본인 계정으로 다른 가족의 대출 현황을 볼 수 없다. 가족 구성원의 도서관 계정을 등록해두면 자동 로그인으로 대출 현황을 한 화면에서 조회하는 서비스.

## 기술 스택
- Frontend: React + TypeScript + Vite
- Backend: Spring Boot 3.x (Java 21) + PostgreSQL 16
- 크롤링/자동로그인: Playwright (Java 바인딩)
- 서비스 로그인: OAuth2 (Google/Naver)
- 비밀번호 암호화: AES-256-GCM (`.env`로 키 분리, 절대 커밋 금지)

## 문서
- 전체 개발 스펙(아키텍처/DB/API/보안): [docs/spec.md](docs/spec.md)
- 화면별 요건·레이아웃: [docs/screens/](docs/screens/)
  - [01-login.md](docs/screens/01-login.md) — 로그인
  - [02-dashboard.md](docs/screens/02-dashboard.md) — 현황조회(메인)
  - [03-family-members.md](docs/screens/03-family-members.md) — 가족등록
  - [04-notification-settings.md](docs/screens/04-notification-settings.md) — 알림설정
  - [05-reservations.md](docs/screens/05-reservations.md) — 일반예약현황
  - [06-mutual-loans.md](docs/screens/06-mutual-loans.md) — 상호대차현황(신청현황)
- issl.go.kr 사이트 구조 조사 결과: [docs/issl-site-notes.md](docs/issl-site-notes.md)

## 디자인 소스
Claude Design 프로젝트 "Library Loan Status"에서 핸드오프. 디자인 토큰(색상/타이포/spacing/effects)은 `docs/screens/` 각 문서의 "디자인 토큰" 절 참고 — frontend 구현 시 CSS 변수로 그대로 이식할 것.

## 보안 규칙
- 도서관 계정 비밀번호는 반드시 AES-256-GCM으로 암호화 후 저장. 평문 저장/로그 금지.
- `.env`, 암호화 키, 실제 가족 계정 자격증명은 절대 커밋하지 않는다.
- issl.go.kr 자동 로그인 시 User-Agent를 실제 브라우저 값으로 설정할 것 (UA 기반 차단 있음, [docs/issl-site-notes.md](docs/issl-site-notes.md) 참고).

## DB 마이그레이션 규칙
- 테이블/컬럼을 새로 만들거나 이름·의미를 바꾸는 마이그레이션에는 반드시 `COMMENT ON TABLE` / `COMMENT ON COLUMN`을 같이 작성한다(예: `V4__add_comments.sql`). 무엇을 저장하는 컬럼인지, 다른 테이블과의 관계·제약(UNIQUE 이유 등)을 한국어로 짧게 남길 것.
