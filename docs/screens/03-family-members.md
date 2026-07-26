# 03. 가족등록 (가족 구성원 관리)

## 목적
가족 구성원을 등록하고, 구성원별 도서관(issl.go.kr) 계정(아이디/비밀번호)을 연결·수정·삭제.

## 요건
- 본인(`is_self=true`)은 로그인 시 자동 등록되며 **삭제 불가**, "기본 카드" 배지 표시
- 구성원 카드에 이름 / 아이디(도서관 로그인 ID) / 비밀번호(마스킹 표시) 노출
- 카드별 "수정" 진입 → 편집 모드(입력 필드 활성화, 저장/취소 버튼 노출)
- 편집 모드 진입 시 다른 카드는 비활성화(흐림 처리, 클릭 불가) — 동시에 한 카드만 편집
- "가족 구성원 추가" 액션으로 새 카드 생성
- 도서관 계정 저장/수정 시 **즉시 크롤링으로 로그인 검증** 후 성공해야 저장 확정 (`docs/spec.md` §6 크롤링 스펙 참고) — 실패 시 에러 메시지로 되돌림

## 레이아웃 구성

```
┌───────────────────────────────┐
│ ‹  가족 구성원 관리             │ ← appbar (back 버튼)
├───────────────────────────────┤
│ ┌───────────────────────────┐ │
│ │ [기본 카드]                │ │ ← 본인 카드 (삭제 X 버튼 없음)
│ │ 이름   김지훈               │ │
│ │ 아이디 jihoon_kim           │ │
│ │ 비밀번호 ••••••••           │ │
│ │                     수정   │ │
│ └───────────────────────────┘ │
│ ┌───────────────────────────┐ │
│ │ [수정 중]              ✕  │ │ ← 편집 중인 카드 (테두리 강조)
│ │ 이름   [김민수        ]    │ │
│ │ 아이디 [minsu_dad     ]    │ │
│ │ 비밀번호[•••••••      ]    │ │
│ │                 취소 저장  │ │
│ └───────────────────────────┘ │
│ ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐ │
│ ｜      ＋ 가족 구성원 추가   ｜ │ ← 점선 테두리
│ └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘ │
└───────────────────────────────┘
```

- 카드: `border-radius:md`, 기본 `border-color: border`, 편집 중엔 `primary-700` 2px
- 비활성 카드(다른 카드 편집 중일 때): `opacity:.5; pointer-events:none; background:neutral-50`
- 입력 필드(`input` 클래스): height 44px, radius md, 기본 `background:neutral-50`, 편집 모드에서 `border-color:primary-700; background:bg-surface`
- 추가 버튼: 점선(`dashed`) `neutral-400` 테두리, 중앙 정렬 텍스트

## 컴포넌트
- `FamilyMembersPage`
  - `AppBar` (title, back)
  - `MemberCard` (반복) — props: `isSelf`, `editing`, `disabled`
    - `EditBadge` / `SelfBadge`
    - `RemoveButton` (isSelf가 아닐 때만)
    - `LabeledField` × 3 (이름/아이디/비밀번호)
    - `EditActions` (편집 모드: 취소/저장 버튼, 평상시: 수정 링크)
  - `AddMemberButton` (점선 카드)

## 상태/인터랙션
- `editing: string | null` — 현재 편집 중인 구성원 key, 한 번에 하나만
- 카드 진입 `수정` 클릭 → `editing = member.key`
- `저장` 클릭 → API 호출(계정 검증 포함) 성공 시 `editing = null`, 실패 시 alert/inline 에러 후 편집 유지
- `취소` 클릭 → 변경사항 버리고 `editing = null`
- `✕` 클릭(비-본인 카드) → 삭제 확인 후 `DELETE /api/members/{id}` 또는 `/api/library-accounts/{id}`
- 비밀번호 필드는 항상 마스킹(`••••••••`) 표시, 실제 값은 저장 시에만 전송하고 클라이언트 상태에 평문 보관 최소화

## 디자인 토큰
```css
--color-primary-700:#4F2A85;
--color-neutral-50:#F7F6FA; --color-neutral-100:#F4F3F7; --color-neutral-400:#C3C1CB;
--color-neutral-600:#8B8993; --color-neutral-800:#4A4852; --color-neutral-900:#1F1F24;
--color-border:#E9E7ED; --color-link:#4F2A85;
--font-size-xs:13px; --font-size-sm:15px; --font-size-lg:24px;
--radius-md:10px; --radius-pill:999px; --radius-circle:50%;
```

## API 연동
- `GET /api/members`
- `POST /api/members` (구성원 추가), `DELETE /api/members/{id}`
- `POST /api/members/{id}/library-accounts` (계정 등록, 즉시 로그인 검증)
- `PUT /api/library-accounts/{id}`, `DELETE /api/library-accounts/{id}`
