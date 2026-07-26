package com.woori.library.service.crawler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 로그인된 세션에서 대출현황(현재 대출 중인 도서)을 긁어온다.
 *
 * <p>docs/issl-site-notes.md "대출현황 / 대출이력 페이지" 절의 실계정 검증 결과를 그대로 반영. "대출현황"
 * ({@code loanList.do})과 "대출이력"({@code loanHistoryList.do})은 별개 메뉴이며, 반납예정일이 있는
 * 대출현황이 이 앱의 "대출 현황 조회" 목적에 맞다.
 *
 * <p>지점명은 소장도서관이 아닌 <b>수령도서관</b>을 쓴다 — 상호대차대출의 경우 두 값이 달라질 수 있는데,
 * 사용자가 실제로 반납하러 가야 하는 곳은 수령도서관이다.
 */
@Service
public class IsslLoanScrapeService {

    private static final String LOAN_LIST_PATH = "/mbr/mstd/loanList.do?mnidx=1548";
    private static final String ROW_SELECTOR = "table.tableType.tableType02 tbody tr";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String MOVE_PAGE_JS =
        "n => { document.querySelector('input[name=pageNo]').value = n; "
            + "document.querySelector('form[name=frm]').method = 'post'; "
            + "document.querySelector('form[name=frm]').submit(); }";

    private final String baseUrl;

    public IsslLoanScrapeService(@Value("${app.crawler.issl.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<LoanRecordDraft> fetchLoans(IsslSession session) {
        Page page = session.page();
        // 대출현황 테이블은 서버 렌더링이라(조사 시 확인) DOM 로드만 기다리면 되고, networkidle까지
        // 기다릴 필요가 없다 — 광고/트래킹 스크립트 로딩 때문에 불필요하게 느려진다.
        page.navigate(baseUrl + LOAN_LIST_PATH, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        List<LoanRecordDraft> drafts = new ArrayList<>();
        int totalPages = countPages(page);
        for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
            if (pageNo > 1) {
                goToPage(page, pageNo);
            }
            drafts.addAll(scrapeCurrentPage(page));
        }
        return drafts;
    }

    private int countPages(Page page) {
        return Math.max(page.locator("div.paging ol li").count(), 1);
    }

    private void goToPage(Page page, int pageNo) {
        page.waitForNavigation(
            new Page.WaitForNavigationOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED),
            () -> page.evaluate(MOVE_PAGE_JS, pageNo));
    }

    private List<LoanRecordDraft> scrapeCurrentPage(Page page) {
        List<LoanRecordDraft> drafts = new ArrayList<>();
        Locator rows = page.locator(ROW_SELECTOR);
        int rowCount = rows.count();
        for (int i = 0; i < rowCount; i++) {
            Locator cells = rows.nth(i).locator("td");
            if (cells.count() < 6) {
                continue;
            }
            String bookTitle = normalizeText(cells.nth(1).textContent());
            LocalDate loanDate = parseDate(cells.nth(2).textContent());
            LocalDate dueDate = parseDate(cells.nth(3).textContent());
            String branchName = normalizeText(cells.nth(5).textContent());
            drafts.add(new LoanRecordDraft(bookTitle, branchName, loanDate, dueDate));
        }
        return drafts;
    }

    private String normalizeText(String raw) {
        return raw.replaceAll("[\\s\\u200B]+", " ").trim();
    }

    private LocalDate parseDate(String raw) {
        return LocalDate.parse(raw.replaceAll("[\\s\\u200B]+", ""), DATE_FMT);
    }
}
