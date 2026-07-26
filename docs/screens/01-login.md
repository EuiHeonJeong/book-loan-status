# 01. 로그인

## 목적
서비스 자체 계정으로 로그인. **도서관 계정과는 별개** — 이 화면은 소셜 로그인(서비스 계정)만 다루고, 도서관 계정(가족 구성원별 issl.go.kr 계정)은 [03-family-members.md](03-family-members.md)에서 등록.

## 요건
- 지원 로그인 수단: Google, Naver (2종, 소셜 로그인만 지원 — 자체 회원가입/비밀번호 로그인 없음)
- 로그인 성공 시 `/api/auth/me` 조회 후 현황조회(메인) 화면으로 이동
- 최초 로그인(가족 구성원 미등록 상태)이면 본인이 `family_member`에 `is_self=true`로 자동 등록되어야 함 (가족등록 화면에서 삭제 불가한 "기본 카드")

## 레이아웃 구성 (모바일 1화면, 중앙 정렬)

```
┌─────────────────────────┐
│                          │
│        우리서재           │  ← font-display(Jua), 2xl, primary-700 (서비스 브랜드명)
│    대출 현황 조회          │  ← lg, bold, text
│                          │
│  [G] 구글로 로그인         │  ← height 52px, pill, white bg + border
│  [N] 네이버로 로그인       │  ← height 52px, pill, #03C75A bg, white text
│  [K] 카카오로 로그인       │  ← height 52px, pill, #FEE500 bg, #391B1B text
│                          │
│  ※ 소셜 로그인만 지원      │  ← xs, neutral-600
│                          │
└─────────────────────────┘
```

- 로고 영역과 버튼 영역 사이 간격 36px(space-9)
- 버튼 간 간격 14px
- 버튼 내부: 아이콘(20px) + gap 10px + 텍스트(base, medium/bold)

## 컴포넌트
- `LoginPage`
  - `BrandHeader` (서비스명 "우리서재" + 태그라인 "대출 현황 조회")
  - `SocialLoginButton` (variant: google/naver — 색상/아이콘/폰트웨이트가 provider별로 다름, 공통 컴포넌트로 분기)

## 인터랙션
- 버튼 클릭 → 해당 OAuth2 provider 인증 플로우로 리다이렉트 (Spring Security `/oauth2/authorization/{provider}`)
- 인증 완료 후 백엔드가 세션 쿠키 발급 → 프론트는 `/api/auth/me` 성공 시 메인으로 라우팅

## 디자인 토큰
```css
--font-display: 'Jua','Noto Sans KR',sans-serif;
--font-size-2xl: 44px;
--font-size-lg: 24px;
--font-size-base: 17px;
--font-size-xs: 13px;
--color-primary-700: #4F2A85;
--color-neutral-900: #1F1F24;
--color-neutral-600: #8B8993;
--color-neutral-300: #DEDCE3;
--color-neutral-0: #FFFFFF;
--radius-pill: 999px;
```
provider 고유색(디자인 시스템 토큰 아님, 브랜드 고정색): Google 로고는 멀티컬러 svg 그대로, Naver `#03C75A`.

## API 연동
- `GET /api/auth/me` (로그인 상태 확인)
- OAuth2 리다이렉트: `/oauth2/authorization/google|naver`
