# 05. 일반예약현황

## 목적
등록된 모든 가족 구성원의 일반예약(대출 대기) 현황을 한 화면에서 조회. 예약한 도서가 순번이 돌아와 대출 가능(수령 대기) 상태가 되면 알림으로 알려준다. 조회 전용 화면 — 예약취소/전환신청 같은 액션은 이 서비스 범위 밖(issl.go.kr에서 직접 처리).

## 요건
- 대시보드(현황조회)와 동일한 접기/펼치기 필터 패널 — 가족 체크박스 + 도서관 체크박스(둘 다 기본값 전체 체크). Claude Design 핸드오프 목업엔 필터가 없었으나, 대출현황과 동일한 조작감을 위해 사용자 요청으로 추가함(2026-08-27).
- 목록 항목: 도서명, 순위 배지(우상단, 뉴트럴), 예약일·만기일·소장도서관·가족 구성원명, 상태 배지(하단 별도 줄)
- 상태 배지 2종:
  - **대출가능(수령 가능) 상태** — `accent-red`가 아니라 **보라색(primary-700) 채움**. issl.go.kr 원문 상태 텍스트가 "대출가능 판정" 키워드에 매칭된 경우. ⚠️ 정확한 원문 텍스트가 미확인 상태라(`docs/issl-site-notes.md` 참고) 백엔드가 최초 판정 로직을 바꿀 수 있음 — 프론트는 서버가 내려주는 `ready: boolean`만 보고 배지 색을 결정, 원문 텍스트 매칭 로직을 프론트에 직접 넣지 않는다. 배지 텍스트는 원문 `statusText`를 그대로 표시(하드코딩한 "대출가능" 문자열이 아님).
  - **대기 중** — 보라색(primary-700) 아웃라인. 텍스트는 원문 `statusText` 그대로.
- 순위 배지는 `rank`가 있을 때만 표시, 뉴트럴(연회색 배경 + 진회색 글씨) 스타일로 상태 배지와 시각적으로 구분한다.
- 목록 상단에 "총 N건" 카운터(항목이 1개 이상일 때만)
- 결과 없음 상태: "조회되는 도서가 없습니다"(issl.go.kr 원문 문구와 통일)
- 대시보드 상단 알림(벨) 드롭다운에 "『도서명』" 알림이 함께 노출되고(보라색 "대출가능" 배지), 클릭 시 이 화면으로 이동

## 레이아웃 구성

```
┌───────────────────────────────┐
│ ‹  일반예약현황                │ ← appbar (back 버튼)
├───────────────────────────────┤
│ 필터                    ▸펼치기│ ← 클릭 시 펼침/접힘
│  (펼치면 가족/도서관 체크박스)  │
├───────────────────────────────┤
│ 총 2건                         │
│ ┌───────────────────────────┐ │
│ │ 도서명              [1순위] │ │ ← 예약 카드 (반복)
│ │ 예약일 · 만기일 · 소장도서관 · 이름 │ │
│ │ [대출가능]                  │ │ ← 상태 배지(별도 줄)
│ └───────────────────────────┘ │
│ ...                            │
└───────────────────────────────┘
```

- appbar: 대시보드와 동일 패턴, back 버튼으로 메인 복귀
- 카드: 대시보드 `LoanCard`와 동일한 카드 골격(테두리/라운드/패딩) 재사용, 상단은 도서명+순위 배지, 중간은 상세 정보, 하단은 상태 배지 한 줄

## 컴포넌트
- `ReservationsPage`
  - `AppBar` (title, back)
  - `FilterPanel` (접기/펼치기, `FamilyCheckboxGroup`, `LibraryCheckboxGroup` — 대시보드와 동일 컴포넌트 재사용)
  - `ReservationList` → `ReservationCard` (반복), `EmptyState`

## 상태/인터랙션
- `filterOpen: boolean` — 필터 패널 접기/펼치기
- `family: Record<memberName, boolean>`, `libraries: Record<libraryCode, boolean>` — 체크박스 상태, 토글 시 즉시 목록 재계산(클라이언트 필터링, 서버는 항상 가족 전체를 내려줌)
- 정렬은 서버가 예약일 오름차순으로 내려줌 — 프론트 별도 정렬 없음
- 순위 배지: `rank != null`일 때만 노출, `neutral` 스타일
- 상태 배지: `ready === true` → `ready` 스타일(보라 채움), `ready === false` → `due` 스타일(보라 아웃라인). 텍스트는 항상 서버가 내려준 `statusText` 원문

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
- `GET /api/reservations` → `[{title,branch,reservedAt,expiresAt,rank,statusText,ready,memberName}]`
