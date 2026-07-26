package com.woori.library.service.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitUntilState;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * issl.go.kr(인천서구구립도서관) 로그인 자동화.
 *
 * <p>docs/issl-site-notes.md에서 확인한 실제 흐름을 그대로 따라간다: 로그인 페이지에서 아이디/비밀번호를 입력하고
 * #loginBtn을 클릭하면, 페이지 자체 JS가 (1) POST /mbr/loginChkPro.do 로 자격증명을 검증한 뒤 (2) 성공 시
 * /mbr/loginPro.do 로 실제 폼 POST를 보내 세션을 확립한다. 우리가 그 두 요청을 직접 재현하지 않고 실제 브라우저로
 * 버튼을 클릭하는 이유는, 사이트가 이 JS 로직을 바꿔도(엔드포인트명 변경 등) 깨지지 않게 하기 위함이다.
 *
 * <p><b>Referer 필수</b>: 로그인 페이지의 hidden {@code referer} 필드는 이전 페이지에서 자연스럽게 넘어왔을 때만
 * 채워지며, 비어있으면 {@code loginPro.do}가 500을 반환하고 로그인이 실패한다(docs/issl-site-notes.md 참고).
 * Playwright의 순차 navigate()는 실제 브라우저 링크 클릭과 달리 이전 페이지를 Referer로 자동 전달하지 않으므로,
 * 메인페이지를 거치는 것과 별개로 {@code setReferer()}를 명시해야 한다.
 *
 * <p><b>판정 방식</b>: {@code loginChkPro.do}/{@code loginPro.do} 응답을 가로채 본문/상태코드를 읽는 방식은
 * 포기했다 — 로그인이 빠르게 성공할수록 사이트 JS가 응답을 받자마자 바로 다음 페이지로 이동시켜버려서, 우리가
 * 응답을 읽으려는 시점엔 이미 늦어 캡처를 놓치는 경우가 실제로 있었다(2026-07-26 확인, "Response body is not
 * available for a response that was navigated away from"). 대신 유일하게 확실한 신호인 페이지 URL이
 * 로그인 페이지({@code loginView.do})를 벗어났는지를 직접 폴링해서 판정한다.
 */
@Service
public class IsslLoginService {

    private static final Logger log = LoggerFactory.getLogger(IsslLoginService.class);

    private static final String LOGIN_PATH = "/mbr/loginView.do?mnidx=1544";
    // issl.go.kr 응답이 느릴 때 실측 15~17초까지 걸린다(2026-07-26 확인) — 여유 있게 잡는다.
    private static final int LOGIN_TIMEOUT_MS = 30000;

    private final String baseUrl;
    private final String userAgent;

    public IsslLoginService(
        @Value("${app.crawler.issl.base-url}") String baseUrl,
        @Value("${app.crawler.issl.user-agent}") String userAgent) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
    }

    public IsslSession login(String loginId, String password) {
        long t0 = System.currentTimeMillis();
        Playwright playwright = Playwright.create();
        Browser browser = null;
        try {
            // 배포 컨테이너는 root로 실행돼 기본 샌드박스로 Chromium이 못 뜬다(--no-sandbox 필요).
            // --disable-dev-shm-usage: 컨테이너 기본 /dev/shm 용량 제한(보통 64MB)으로 인한 크래시 방지.
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent(userAgent));
            Page page = context.newPage();
            // navigate/click 자체의 안전망 타임아웃 — 판정은 아래 폴링 루프가 별도로 처리한다.
            page.setDefaultTimeout(LOGIN_TIMEOUT_MS);
            log.info(
                "[issl-debug] 브라우저 기동 완료 (+{}ms), navigator.webdriver={}",
                System.currentTimeMillis() - t0,
                page.evaluate("navigator.webdriver"));

            AtomicReference<String> failureMessage = new AtomicReference<>();
            page.onDialog(dialog -> {
                failureMessage.compareAndSet(null, dialog.message());
                dialog.accept();
            });

            // 모든 경우의 수 진단용: 콘솔 에러, 페이지 JS 예외, 네트워크 실패, 응답 상태코드(본문은 안 읽음 —
            // 그건 위 주석에서 설명한 이유로 페이지 이동과 경합한다)를 전부 남긴다.
            page.onConsoleMessage(msg -> {
                if ("error".equals(msg.type()) || "warning".equals(msg.type())) {
                    log.info("[issl-debug] console.{} (+{}ms): {}", msg.type(), System.currentTimeMillis() - t0, msg.text());
                }
            });
            page.onPageError(err -> log.info("[issl-debug] page error (+{}ms): {}", System.currentTimeMillis() - t0, err));
            page.onRequestFailed(req -> log.info(
                "[issl-debug] request failed (+{}ms): {} {} - {}",
                System.currentTimeMillis() - t0, req.method(), req.url(),
                req.failure() != null ? req.failure() : "?"));
            page.onResponse(res -> {
                String url = res.url();
                if (url.contains("loginChkPro.do") || url.contains("loginPro.do")
                    || url.contains("loginView.do") || url.contains("/main.do")) {
                    log.info("[issl-debug] response (+{}ms): {} {}", System.currentTimeMillis() - t0, res.status(), url);
                }
            });

            // networkidle(광고/트래킹 스크립트까지 전부 끝날 때까지 대기)은 실제 사람이 로그인 버튼을 누르는
            // 시점보다 훨씬 늦다 — #userId 등 필요한 요소가 뜨는 데는 DOM 로드만으로 충분하고, fill/click은
            // Playwright가 대상 요소가 준비될 때까지 알아서 기다리므로 여기서 더 기다릴 필요가 없다.
            String mainUrl = baseUrl + "/main.do";
            page.navigate(mainUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            log.info("[issl-debug] main.do 도착 (+{}ms)", System.currentTimeMillis() - t0);
            page.navigate(
                baseUrl + LOGIN_PATH,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setReferer(mainUrl));
            log.info("[issl-debug] loginView 도착 (+{}ms)", System.currentTimeMillis() - t0);
            page.fill("#userId", loginId);
            page.fill("#password", password);
            log.info("[issl-debug] 필드 입력 완료 (+{}ms)", System.currentTimeMillis() - t0);

            // 페이지 로딩이 느릴 때(오늘 실측 18초+) DOMCONTENTLOADED 직후 곧바로 클릭하면, 사이트 자체
            // 스크립트(jQuery 등)가 아직 클릭 핸들러를 바인딩하기 전이라 클릭이 씹히는 경우가 실제로
            // 있었다(2026-07-26, loginChkPro.do 요청 자체가 30초간 단 한 번도 안 나감). 응답이 없으면
            // 몇 초 간격으로 다시 클릭해서 이 레이스를 방어한다.
            long[] lastLog = {System.currentTimeMillis()};
            long[] lastClickAttempt = {0};
            waitUntil(
                () -> {
                    long now = System.currentTimeMillis();
                    if (failureMessage.get() != null || !page.url().contains("loginView.do")) {
                        return true;
                    }
                    if (now - lastClickAttempt[0] > 4000) {
                        lastClickAttempt[0] = now;
                        log.info("[issl-debug] #loginBtn 클릭 시도 (+{}ms)", now - t0);
                        try {
                            page.click("#loginBtn");
                        } catch (RuntimeException e) {
                            log.info("[issl-debug] 클릭 재시도 중 예외(무시): {}", e.getMessage());
                        }
                    }
                    if (now - lastLog[0] > 3000) {
                        lastLog[0] = now;
                        log.info("[issl-debug] 폴링 중 (+{}ms) url={}", now - t0, page.url());
                    }
                    return false;
                },
                LOGIN_TIMEOUT_MS);

            log.info(
                "[issl-debug] 폴링 종료 (+{}ms) url={} failureMessage={}",
                System.currentTimeMillis() - t0,
                page.url(),
                failureMessage.get());

            if (failureMessage.get() != null) {
                throw new IsslAuthException("로그인 실패: " + failureMessage.get());
            }
            if (page.url().contains("loginView.do")) {
                logPageSnapshot(page, t0);
                throw new IsslAuthException(
                    "로그인 실패: issl.go.kr 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
            }
            if (page.url().contains("loginPro.do")) {
                // loginPro.do 자체 주소에서 더 못 벗어났다 — 로그인 처리 중 서버 오류가 난 것으로 본다.
                logPageSnapshot(page, t0);
                throw new IsslAuthException(
                    "로그인 실패: 도서관 서버에서 오류가 발생했습니다. 최근 자동 로그인 시도가 많았다면 issl.go.kr에서 일시적으로 제한했을 수 있습니다.");
            }

            return new IsslSession(playwright, browser, context, page);
        } catch (TimeoutError e) {
            log.warn("[issl-debug] TimeoutError (+{}ms): {}", System.currentTimeMillis() - t0, e.getMessage());
            if (browser != null) {
                browser.close();
            }
            playwright.close();
            throw new IsslAuthException(
                "로그인 실패: issl.go.kr 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
        } catch (RuntimeException e) {
            log.warn("[issl-debug] RuntimeException (+{}ms)", System.currentTimeMillis() - t0, e);
            if (browser != null) {
                browser.close();
            }
            playwright.close();
            throw e;
        }
    }

    /** 실패 직전 화면에 실제로 뭐가 떠 있는지 남긴다 — 로그인 폼에 그대로 멈춰있는지, 다른 에러 화면인지 등. */
    private void logPageSnapshot(Page page, long t0) {
        try {
            String title = page.title();
            String bodyText = (String) page.evaluate("document.body ? document.body.innerText.slice(0, 300) : ''");
            log.info(
                "[issl-debug] 실패 시점 페이지 스냅샷 (+{}ms) title=\"{}\" body(300자)=\"{}\"",
                System.currentTimeMillis() - t0, title, bodyText.replaceAll("\\s+", " ").trim());
        } catch (RuntimeException e) {
            log.info("[issl-debug] 페이지 스냅샷 실패: {}", e.getMessage());
        }
    }

    private void waitUntil(BooleanSupplier condition, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
