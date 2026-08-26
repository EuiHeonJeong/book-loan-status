# 06. 상호대차현황

## 목적
등록된 모든 가족 구성원의 상호대차 신청 현황을 한 화면에서 조회. 신청한 도서가 대출 가능 상태가 되면 알림으로 알려준다. issl.go.kr과 동일하게 **신청현황/이력현황 두 탭**을 제공한다(Claude Design 핸드오프 반영 — 최초 계획은 신청현황만이었으나 목업에 이력현황 탭이 포함되어 함께 구현). 조회 전용 화면 — 신청취소 같은 액션은 이 서비스 범위 밖.

## 요건
- 상단 탭: **신청현황** / **이력현황**(issl.go.kr 원문 탭 이름 그대로). 선택된 탭은 `primary-800` 배경 + 흰 글씨, 미선택은 `bg-surface` 배경 + `neutral-600` 글씨.
- 가족 필터 없음 — 두 탭 모두 등록된 모든 구성원의 건을 한 목록에 그대로 보여준다.
- **신청현황 탭** 항목: 도서명, 상태 배지(우상단), 신청일·소장도서관·수령처·가족 구성원명
  - 상태 배지: `ready === true`(실계정 확인값 "대출중")면 보라색(primary-700) 채움, 아니면 보라색 아웃라인. 텍스트는 항상 원문 `statusText`.
  - ⚠️ "대출중"이 정확히 "수령 가능해진 시점"인지 "이미 대출 완료된 시점"인지 불확실(`docs/issl-site-notes.md` 참고) — 중간 상태가 추가로 발견되면 배지 종류가 늘어날 수 있음. 프론트는 상태 문자열을 하드코딩 매칭하지 말고 `statusText`를 그대로 표시 + `ready`로만 배지 색 결정.
- **이력현황 탭** 항목: 도서명, 상태 배지(우상단, 항상 뉴트럴 — 이미 종결된 건이라 강조하지 않음), 신청일·소장도서관·수령처·가족 구성원명. 실계정 확인값: "완료".
- 각 탭 상단에 "총 N건" 카운터(항목이 1개 이상일 때만)
- 결과 없음 상태: "조회되는 도서가 없습니다"(issl.go.kr 원문 문구와 통일)
- 대시보드 상단 알림(벨) 드롭다운에 신청현황의 "대출가능" 알림만 노출(이력현황은 이미 종결된 건이라 알림 대상 아님), 클릭 시 이 화면(신청현황 탭)으로 이동

## 레이아웃 구성

```
┌───────────────────────────────┐
│ ‹  상호대차현황                │ ← appbar (back 버튼)
├───────────────────────────────┤
│ [ 신청현황 ]  이력현황         │ ← 탭(선택된 쪽 보라 배경)
├───────────────────────────────┤
│ 총 1건                         │
│ ┌───────────────────────────┐ │
│ │ 도서명              [대출중] │ │ ← 신청 카드 (ready=true면 보라 채움)
│ │ 신청일 · 소장도서관 · 수령처 · 이름 │ │
│ └───────────────────────────┘ │
└───────────────────────────────┘
```

- appbar: 대시보드와 동일 패턴, back 버튼으로 메인 복귀
- 카드: 대시보드 `LoanCard`와 동일한 카드 골격 재사용, 배지 색/텍스트만 교체

## 컴포넌트
- `MutualLoansPage`
  - `AppBar` (title, back)
  - 탭 바 (신청현황/이력현황)
  - `MutualLoanList` → `MutualLoanCard`(신청현황) / `MutualLoanHistoryCard`(이력현황), `EmptyState`

## 상태/인터랙션
- `tab: 'current' | 'history'` — 탭 전환 시 이미 불러온 두 목록 중 하나만 표시(탭마다 재요청하지 않음, 페이지 진입 시 둘 다 미리 로드)
- 신청현황 배지: `ready === true` → `ready` 스타일, `ready === false` → `due` 스타일
- 이력현황 배지: 항상 `neutral` 스타일

## 디자인 토큰
```css
--color-primary-700:#4F2A85; --color-primary-800:#3E1F6B;
--color-neutral-0:#FFFFFF; --color-neutral-100:#F4F3F7;
--color-neutral-300:#DEDCE3; --color-neutral-600:#8B8993; --color-neutral-800:#4A4852; --color-neutral-900:#1F1F24;
--color-border:#E9E7ED; --color-bg-surface:#FFFFFF;
--font-size-xs:13px; --font-size-sm:15px; --font-size-lg:24px;
--font-weight-medium:500; --font-weight-bold:700;
--radius-md:10px;
--shadow-md:0 4px 12px rgba(31,31,36,0.08);
```

## API 연동
- `GET /api/mutual-loans` → `[{title,branch,pickupBranch,appliedAt,statusText,ready,memberName}]` (신청현황)
- `GET /api/mutual-loans/history` → `[{title,branch,pickupBranch,appliedAt,statusText,memberName}]` (이력현황, `ready` 없음)
