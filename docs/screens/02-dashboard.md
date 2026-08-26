# 02. 현황조회 (메인)

## 목적
등록된 모든 가족 구성원의 도서관 대출 현황을 한 화면에서 필터/정렬하여 조회. 서비스의 핵심 화면.

## 요건
- 반납임박(D-3 이내) 도서를 상단에 가로 스크롤로 강조 노출 (최대 3건)
- 가족 구성원별, 도서관(지점)별 체크박스 필터 — **기본값 전체 체크**
- 정렬 기준: 반납예정일 / 대출일, 오름차순·내림차순 토글
- 필터링된 대출 목록: 도서명, 대출일, 반납예정일, 지점명, 가족 구성원명, D-day 또는 연체 배지
- 결과 없음 상태 문구 처리
- 상단 알림(벨) 아이콘: 새 알림 있으면 red dot 표시, 클릭 시 알림 목록 드롭다운 (반납임박/연체 + 일반예약·상호대차 "대출가능" 알림 포함, "대출가능" 배지는 보라색(primary) 채움 — 연체와 같은 빨간색이 아님. 클릭 시 해당 화면으로 이동)
- 상단 메뉴(햄버거) 아이콘: 클릭 시 "가족 관리 / 상호대차현황 / 일반예약현황 / 알림 설정 / 로그아웃" 드롭다운(Claude Design 핸드오프 순서)

## 레이아웃 구성

```
┌───────────────────────────────┐
│ 도서대여 현황        🔔● ☰    │ ← appbar
├───────────────────────────────┤
│ ⚠ 반납임박 도서                │
│ [도서A  D-2] [도서B D-1] [..] │ ← 가로 스크롤 카드
├───────────────────────────────┤
│ 필터 · 정렬              ▾접기│ ← 클릭 시 펼침/접힘
│  가족: ☑아빠 ☑엄마 ☑나 ☑동생  │
│  도서관: ☑아라누리 ☑단봉늘봄..│ ← 실제 7개 지점 전체 노출
│  정렬: [반납예정일][대출일] 오름차순▲│
├───────────────────────────────┤
│ ┌───────────────────────────┐ │
│ │ 도서명            [D-2]   │ │ ← 대출 카드 (반복)
│ │ 대출일 · 반납예정 · 지점 · 이름│ │
│ └───────────────────────────┘ │
│ ...                            │
└───────────────────────────────┘
```

- appbar: `justify-content:space-between`, 아이콘 버튼 36px 원형
- 반납임박 카드: `min-width:108px`, 배경 `primary-50`, 도서명 1줄 말줄임
- 필터 패널: 가족/도서관은 `pillbtn` 형태 체크(✓ 마크), 정렬은 pill 버튼(선택 시 `primary-700` 배경 + 흰 글씨, 미선택은 `neutral-100` 배경)
- 대출 카드: 배지 - 정상(D-day)은 `primary-700` outline, 연체는 `accent-red` 배경 채움
- 메뉴/알림 드롭다운: `position:absolute`, 반투명 오버레이(`rgba(31,31,36,.25)`) + 카드(box-shadow-md)

## 컴포넌트
- `DashboardPage`
  - `AppBar` (title, `NotifBellButton`, `MenuButton`)
  - `UrgentLoansRail` (가로 스크롤 카드 리스트)
  - `FilterSortPanel` (접기/펼치기, `FamilyCheckboxGroup`, `LibraryCheckboxGroup`, `SortPillGroup`, `SortDirToggle`)
  - `LoanList` → `LoanCard` (반복), `EmptyState`
  - `MenuDropdown` (오버레이 + 메뉴 아이템: 가족 관리/상호대차현황/일반예약현황/알림 설정/로그아웃)
  - `NotifDropdown` (오버레이 + `NotifItem` 반복, `EmptyState`) — `NotifItem`은 반납임박/연체/예약 대출가능/상호대차 대출가능 4종을 아이콘·문구로 구분

## 상태/인터랙션
- `filterOpen: boolean` — 필터 패널 접기/펼치기
- `family: Record<memberName, boolean>`, `libraries: Record<libraryCode, boolean>` — 체크박스 상태, 토글 시 즉시 목록 재계산
- `sort: 'due' | 'loan'`, `sortDir: 'asc' | 'desc'`
- `menuOpen`, `notifOpen: boolean` — 배타적으로 하나만 열림, 오버레이 클릭 시 닫힘
- 배지 규칙: `dday < 0` → 연체(`연체 D+{n}`, red), `dday >= 0` → `D-{n}` (primary 아웃라인)
- 반납임박 rail 노출 조건: `dday <= 3`, 최대 3건 (필터와 무관하게 전체 계정 기준인지, 필터 반영인지는 백엔드 정책 확정 필요 — 기본은 필터 무관 전체 기준으로 구현)
- 반납임박 섹션 표시 규칙(대출 데이터 유무 / 알림설정의 `dueAlertEnabled`에 따라 분기):
  - 대출 데이터가 아예 없으면 섹션 자체를 숨김
  - 대출 데이터는 있고 `dueAlertEnabled=false`면 "반납예정 알림이 꺼져있어요." 한 줄만 표시(실제 임박 도서 유무와 무관 — 데이터가 있는데 "없다"고 거짓 표시하면 안 됨)
  - `dueAlertEnabled=true`이고 임박 도서가 0건이면 "반납임박 도서가 없어요." 표시
  - `dueAlertEnabled=true`이고 임박 도서가 있으면 캐러셀 표시

## 디자인 토큰
```css
--color-primary-700:#4F2A85; --color-primary-50:#F6F3FA;
--color-accent-red:#D6483C;
--color-neutral-0:#FFFFFF; --color-neutral-50:#F7F6FA; --color-neutral-100:#F4F3F7;
--color-neutral-300:#DEDCE3; --color-neutral-600:#8B8993; --color-neutral-900:#1F1F24;
--color-border:#E9E7ED; --color-bg-surface:#FFFFFF;
--font-size-xs:13px; --font-size-sm:15px; --font-size-lg:24px;
--font-weight-medium:500; --font-weight-bold:700; --font-weight-black:900;
--radius-md:10px; --radius-pill:999px; --radius-circle:50%;
--shadow-md:0 4px 12px rgba(31,31,36,0.08);
```

## API 연동
- `GET /api/loans?familyIds[]=...&libraryCodes[]=...&sort=due|loan&dir=asc|desc`
- `GET /api/notifications`
- (선택) `POST /api/loans/sync` — 당겨서 새로고침 등에 연결 가능
