# issl.go.kr (인천서구구립도서관) 사이트 구조 조사 노트

Playwright(Chromium, headless)로 2026-07-24 조사. 크롤링 서비스 구현 시 아래 내용을 그대로 반영할 것.

## 봇 차단
기본 Playwright User-Agent로는 네비게이션 등 주요 콘텐츠가 빠진 빈 페이지가 반환됨. 일반 데스크톱 Chrome UA 문자열을 지정하면 정상 렌더링됨 — 단순 User-Agent 기반 필터링으로 추정(딥한 핑거프린팅/CAPTCHA 아님).

```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36
```

크롤링 서비스는 이 UA를 반드시 고정 지정해야 함.

## 로그인 플로우 (2단계, CAPTCHA 없음)

- 로그인 페이지: `GET /mbr/loginView.do?mnidx=1544`
- 폼: `#frm` (`name=userId` text maxlength 20, `name=password` password maxlength 30, hidden `referer`)

**1단계 — 자격증명 검증 (AJAX)**
```
POST /mbr/loginChkPro.do
Content-Type: application/json
Body: {"userId": "...", "password": "..."}
Response: {"result": "Y"}  // 또는 "N" (실패)
```

**2단계 — 세션 생성 (1단계 성공 시에만)**
```
$('#frm').attr('action', '/mbr/loginPro.do');
$('#frm').submit();   // 일반 form POST (AJAX 아님) → 세션 쿠키 발급
```

- 클라이언트 암호화 없음: 페이지 및 연결된 JS(`subCommon.js`, `comSubmit.js`, `checkForm.js`, `include.js`) 전체에서 RSA/SEED/가상키패드/nppfx/veraport/xecure/ahnlab/inisafe/touchen 등 암호화 모듈 키워드 미발견. 비밀번호는 HTTPS 위에서 평문 JSON으로 전송됨.
- CAPTCHA 없음: captcha/recaptcha/hcaptcha/보안문자 키워드 전무.
- **결론(확정, 2026-07-25 실계정 검증 완료)**: 완전 자동화 가능. 아래 "로그인 시 주의사항" 반드시 반영할 것.

### 로그인 시 주의사항 — Referer 필수

`#frm`의 hidden `referer` 필드가 **비어있으면 `loginPro.do`가 500 에러를 반환하고 로그인이 실패한다** (`loginChkPro.do`는 정상적으로 `Y`를 반환하므로 착각하기 쉬움). 로그인 페이지(`loginView.do`)에 진입할 때 반드시 이전 페이지(예: `/main.do`)를 거쳐 Referer가 실리도록 해야 하며, 그러면 사이트 자체 스크립트가 `referer` hidden 필드를 현재 페이지 URL(예: `https://www.issl.go.kr/main.do`)로 자동 채운다. 이 값이 채워진 채로 `loginPro.do`에 POST하면 `302 → /main.do` 리다이렉트와 함께 세션이 정상 생성된다.

정상 흐름 요약:
1. `GET /main.do` (또는 Referer가 실리는 다른 경로)
2. `GET /mbr/loginView.do?mnidx=1544` — 반드시 위 단계에서 이어서 이동(Referer 포함)
3. `POST /mbr/loginChkPro.do` — `{"userId","password"}` → `{"result":"Y"}`
4. `POST /mbr/loginPro.do` — form-urlencoded `referer`(2단계에서 채워진 값)+`userId`+`password` → **302 리다이렉트 to `/main.do`**, 세션 쿠키 발급

세션 쿠키: `JSESSIONID` (`httpOnly=true`, `secure=true`, `sameSite=Lax`).

## 지점 목록 (7개)

검암, 아라누리, 단봉늘봄, 검단, 심곡, 석남, 신석
(초기 가정이었던 "5개(검암/석남/검단/심곡/신)"는 부정확 — 아라누리·단봉늘봄 누락, "신"은 "신석"의 오기)

## 대출현황 / 대출이력 페이지 (실계정 검증 완료)

두 개의 별도 메뉴가 있다 — 헷갈리지 말 것:

| 구분 | URL | 비고 |
|---|---|---|
| 대출현황 (현재 대출 중) | `GET /mbr/mstd/loanList.do?mnidx=1548` | 반납예정일 컬럼 |
| 대출이력 (과거 이력, 반납 포함) | `GET /mbr/mstd/loanHistoryList.do?mnidx=1549` | 반납일 컬럼, 검색기간 필터(최대 3개월) |

로그인 세션(`JSESSIONID` 쿠키)만 있으면 두 URL 모두 GET으로 바로 접근 가능(추가 파라미터 불필요, 기본값으로 전체/최근 목록 노출).

### 테이블 구조

두 페이지 동일한 마크업 패턴:

```html
<table class="tableType tableType02">
  <thead><tr><th>번호</th><th>도서정보</th><th>대출일</th><th>반납예정일 또는 반납일</th>
    <th>소장도서관</th><th>수령도서관</th><th>상태</th><th>반납연기 또는 연체</th></tr></thead>
  <tbody class="textCenter">
    <tr>
      <td>번호</td>
      <td><a href="/sch/bkdt/{id1}/{id2}/{isbn}.do?mnidx=...">도서명</a></td>
      <td>YYYY/MM/DD</td>  <!-- 대출일, 앞뒤에 공백/탭 문자 섞여있어 trim 필요 -->
      <td>YYYY/MM/DD</td>  <!-- 반납예정일/반납일 -->
      <td>소장도서관명</td>
      <td>수령도서관명</td>
      <td>상태 텍스트 (예: 반납, 상호대차대출 반납연기)</td>
      <td>연체/반납연기 텍스트 (없으면 빈 문자열)</td>
    </tr>
    ...
  </tbody>
</table>
```

셀렉터: `table.tableType.tableType02 tbody tr` 순회, `td` 8개를 순서대로 매핑. 날짜 셀은 `textContent`에 개행/탭이 섞여 있으므로 정규화(trim + 공백 collapse) 필요.

### 페이지네이션

URL 파라미터 방식이 아니라 **같은 페이지에 form POST 재제출** 방식이다 (`/js/cmm/comPaging.js`):

```js
function fn_movePage(val){
    $("input[name=pageNo]").val(val);
    $("form[name=frm]").attr("method", "post");
    $("form[name=frm]").attr("action","").submit();
}
```

즉, 크롤러는 `form[name=frm]`의 기존 hidden 필드들을 유지한 채 `pageNo` 값만 바꿔서 같은 URL에 POST해야 다음 페이지를 가져올 수 있다. 전체 페이지 수는 `div.paging ol li` 개수로 파악(`javascript:fn_movePage(N)` 링크).

### 참고 — 엑셀 다운로드

`대출이력` 페이지에는 검색기간(`searchStartDate`,`searchEndDate`, YYYYMMDD, 최대 3개월) 지정 후 `/mbr/mstd/loanHistoryExcelDownload.do`로 엑셀 다운로드하는 기능도 있음(당장 스크래핑에는 불필요, 참고용).

## 일반예약현황 / 상호대차현황(신청현황) 페이지 (실계정 검증, 2026-08-26 조사)

"내서재" 사이드 메뉴 전체 링크(로그인 후 대출현황 페이지에서 확인):

| 메뉴 | URL | 비고 |
|---|---|---|
| 대출현황 | `/mbr/mstd/loanList.do?mnidx=1548` | 기존 구현됨 |
| 대출이력 | `/mbr/mstd/loanHistoryList.do?mnidx=1549` | 기존 구현됨 |
| **일반예약현황** | `/mbr/mstd/reservationList.do?mnidx=1550` | 이번에 조사 |
| **상호대차현황** | `/mbr/mstd/mutualLoanList.do?mnidx=1551` | 이번에 조사 |
| 희망도서신청현황 | `/mbr/mstd/hopeList.do?mnidx=1552` | 범위 밖(참고용) |
| 북드림현황 | `/mbr/mstd/bkdrList.do?mnidx=1553` | 범위 밖(참고용) |
| 무인예약현황 | `/mbr/mstd/unmannedList.do?mnidx=1618` | 범위 밖(참고용) |
| 내책장 | `/mbr/mstd/bkcsMainList.do?mnidx=1555` | 범위 밖(참고용) |

로그인 세션만 있으면 GET으로 바로 접근 가능. 마크업 패턴은 대출현황/대출이력과 동일(`table.tableType.tableType02`, 데이터 없으면 `<td colspan="N">조회되는 도서가 없습니다.</td>` 단일 행).

### 일반예약현황 (`reservationList.do?mnidx=1550`)

```html
<table class="tableType tableType02">
  <thead><tr>
    <th>번호</th><th>도서정보</th><th>예약일</th><th>예약만기일</th>
    <th>소장도서관</th><th>순위</th><th>상태</th><th>예약취소</th><th>전환신청</th>
  </tr></thead>
  <tbody class="textCenter"><!-- 데이터 없으면 colspan=9 단일 행 --></tbody>
</table>
```

- 셀렉터: `table.tableType.tableType02 tbody tr`, `td` 9개 순서대로 매핑. 페이지네이션은 대출현황과 동일한 `form[name=frm]` + `pageNo` POST 재제출 방식.
- **예약취소** 버튼: `onclick="fn_rsvdCancel(this); return false;"` — 이번 기능 범위에서는 조회만 하므로 크롤링/구현 대상 아님(참고용).
- **전환신청** 버튼: `onclick="fn_mtulApply(this); return false;"` — 일반예약을 상호대차 신청으로 전환하는 모달(`#mtulApplyTb`, 도서명/저자/출판사/소장도서관/수령도서관/부록 입력)을 띄움. 이번 범위 밖(참고용).
- ✅ **대기 중 상태 텍스트 확인(2026-08-27, 실계정 크롤링 결과)**: `"예약중"`. 순위 1위인 건도 아직 도착 전이면 이 값. `ReadyStatusMatcher`의 대기중 판정과 충돌하지 않음(현재 매칭 키워드 "도착"/"대출가능"과 다른 문자열이라 오탐 없음).
- ⚠️ **"대출 가능(수령 대기)" 상태의 정확한 텍스트값은 여전히 미확인**. 실계정에 도착 상태까지 간 예약 건이 아직 없어 확인 못 함. "도착", "대출가능", "수령대기" 등이 후보로 추정되나 확정 아님 — 실제 예약 건이 순위 1위에서 도착 상태로 바뀔 때 재조사 필요(크롤러의 "대출가능 판정" 로직은 이 문자열이 확정될 때까지 가정으로 구현하고 주석에 명시할 것).

### 상호대차현황 (`mutualLoanList.do?mnidx=1551`)

이 페이지에는 탭이 2개 있다 — **신청현황**(`type=0`, 기본값) / **이력현황**(`type=1`, 링크의 `title` 속성은 실제로 "취소현황"이라 이력현황≒취소된 신청 이력으로 추정). 탭 전환은 페이지 이동이 아니라 hidden `form[name=frm]`의 `type` 값을 바꿔 같은 URL로 POST 재제출하는 방식(`fn_typeTabMove(gb)` → `ComSubmit`):

```js
function fn_typeTabMove(gb){
    var comSubmit = new ComSubmit();
    comSubmit.addParam("type", gb);
    comSubmit.setUrl("/mbr/mstd/mutualLoanList.do?mnidx=1551");
    comSubmit.submit();
}
```

**`type` 기본값이 0(신청현황)이므로, 단순 `GET /mbr/mstd/mutualLoanList.do?mnidx=1551`만으로 신청현황 탭 데이터가 그대로 내려온다** — 이력현황 탭은 우리 기능 범위 밖이라 별도 POST 처리 불필요.

```html
<table class="tableType tableType02">
  <thead><tr>
    <th>번호</th><th>도서정보</th><th>신청일</th><th>소장도서관</th><th>수령처</th><th>상태</th><th>취소</th>
  </tr></thead>
  <tbody class="textCenter">
    <tr>
      <td>1</td>
      <td><a href="/sch/bkdt/{id1}/{id2}/{isbn}.do?mnidx=1551">도서명</a></td>
      <td>YYYY/MM/DD</td>  <!-- 신청일 -->
      <td>소장도서관명</td>
      <td>수령처(도서관명)</td>
      <td>상태 텍스트 (실계정 확인된 값: "대출중")</td>
      <td>신청취소 버튼</td>
    </tr>
  </tbody>
</table>
```

- 셀렉터: `table.tableType.tableType02 tbody tr`, `td` 7개 순서대로 매핑. 페이지네이션·데이터없음 마크업은 대출현황과 동일.
- **신청취소** 버튼: `onclick="fn_mtulCancel(this); return false;"` — 이번 범위 밖(참고용).
- ⚠️ **실계정에서 확인된 상태 텍스트는 "대출중" 하나뿐**(이미 실물을 수령해 대출이 시작된 건 — 2026-08-27 기준 실계정 3건 모두 "대출중"). 신청 후 도착 전까지 중간 상태(예: "신청중", "배송중", "도착")가 있는지, "대출중"이 곧 "대출 가능해진 시점"인지 아니면 "이미 대출 처리까지 끝난 시점"(알림 실효성이 떨어짐)인지 확정 불가. 여러 상태를 가진 실제 신청 건이 생기면 재조사 필요.

### 상호대차현황 이력현황 탭 (type=1, 2026-08-27 조사)

이력현황은 `fn_typeTabMove(1)`로 같은 URL에 `type=1`을 POST해서 가져온다(신청현황과 동일한 `form[name=frm]` 제출 방식). 신청현황과 컬럼이 다르다 — **"취소" 컬럼이 없다**(이미 종결된 건이라 취소 액션 자체가 없음):

```html
<table class="tableType tableType02">
  <thead><tr>
    <th>번호</th><th>도서정보</th><th>신청일</th><th>소장도서관</th><th>수령처</th><th>상태</th>
  </tr></thead>
  <!-- 신청현황과 달리 마지막 "취소" th/td가 없음 — td 6개 -->
</table>
```

실계정 확인 데이터: `{도서정보:"내 마음 아무도 몰라요~", 신청일:"2026/06/17", 소장도서관:"검단", 수령처:"아라누리", 상태:"완료"}`. 완료된 건의 상태 텍스트는 **"완료"**(Claude Design 목업이 가정했던 "반납완료"가 아님 — 목업 텍스트를 그대로 쓰지 말 것). 이력현황 항목은 이미 종결된 상태라 "대출가능" 알림 대상이 아니다.

**추가 확인(2026-08-27, 실계정 크롤링 20+1건)**: 이력현황 상태 텍스트는 "완료" 외에 **"신청취소된자료"**(사용자가 직접 취소한 건)도 있음. 둘 다 종결 상태라 프론트는 구분 없이 뉴트럴 배지로 원문 그대로 표시(`MutualLoanHistoryCard`).

## 조사에 사용한 스크립트/산출물

`.local/investigate/`(레포에는 없음, `.gitignore`에 `.local/` 추가됨)에 조사용 Playwright `.mjs` 스크립트(`investigate-loan-history.mjs`, `investigate-reservation-interlibrary.mjs`)와 실계정 로그인 후 덤프한 HTML 산출물을 보관. 실제 대출 데이터(도서명 등 개인정보)가 포함되어 있으므로 조사 종료 후 삭제 권장.
