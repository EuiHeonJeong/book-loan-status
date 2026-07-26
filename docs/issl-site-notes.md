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

## 조사에 사용한 스크립트/산출물

`.local/investigate/`(레포에는 없음, `.gitignore`에 `.local/` 추가됨)에 조사용 Playwright `.mjs` 스크립트와 실계정 로그인 후 덤프한 HTML 산출물을 보관. 실제 대출 데이터(도서명 등 개인정보)가 포함되어 있으므로 조사 종료 후 삭제 권장.
