# 우리서재 (Woori Library)

인천서구구립도서관(issl.go.kr)은 가족회원이어도 본인 계정으로 다른 가족의 대출 현황을 볼 수 없습니다.
가족 구성원의 도서관 계정을 등록해두면 자동 로그인으로 대출 현황을 한 화면에서 조회하고, 반납예정/연체 도서를 브라우저 푸시 알림으로 받아볼 수 있는 개인용 서비스입니다.

## 주요 기능
- Google/Naver 소셜 로그인
- 가족 구성원별 도서관 계정 등록 (비밀번호는 AES-256-GCM으로 암호화 저장)
- 전 가족의 대출 현황을 한 화면에서 조회, 가족/도서관/정렬 필터
- 반납예정(D-3/D-2/D-1/당일)·연체 도서 브라우저 Web Push 알림

## 기술 스택
- **Frontend**: React + TypeScript + Vite
- **Backend**: Spring Boot 4.x (Java 21) + PostgreSQL 17
- **크롤링/자동로그인**: Playwright (Java 바인딩)
- **인증**: OAuth2 (Google/Naver)
- **비밀번호 암호화**: AES-256-GCM
- **푸시 알림**: Web Push (VAPID)

## 로컬 개발 환경 설정

### 사전 준비
- Java 21, Node.js, PostgreSQL 17 (로컬 포터블 설치도 가능)
- 저장소 루트에 `.env.example`을 복사해 `.env`로 만들고 값 채우기 (DB 접속정보, `LIBRARY_PW_ENC_KEY`, OAuth client id/secret, VAPID 키)
  - AES 키 생성: `openssl rand -base64 32`
  - VAPID 키 생성: `npx web-push generate-vapid-keys`

### 백엔드
```bash
cd backend
./gradlew installPlaywrightBrowsers   # 최초 1회, issl.go.kr 크롤링용 Chromium 설치
./gradlew bootRun
```
Flyway가 기동 시 `backend/src/main/resources/db/migration`의 마이그레이션을 자동 적용합니다.

### 프론트엔드
```bash
cd frontend
npm install
npm run dev
```

## 문서
- 전체 개발 스펙(아키텍처/DB/API/보안): [docs/spec.md](docs/spec.md)
- 화면별 요건·레이아웃: [docs/screens/](docs/screens/)
- issl.go.kr 사이트 구조 조사 결과: [docs/issl-site-notes.md](docs/issl-site-notes.md)

## 보안
- 도서관 계정 비밀번호는 AES-256-GCM으로 암호화 후 저장하며 평문으로 로그에 남기지 않습니다.
- `.env`, 암호화 키, 실제 가족 계정 자격증명은 커밋하지 않습니다.
