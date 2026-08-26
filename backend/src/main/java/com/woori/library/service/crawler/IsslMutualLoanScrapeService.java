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
 * 로그인된 세션에서 상호대차현황의 "신청현황"/"이력현황" 두 탭을 긁어온다.
 *
 * <p>docs/issl-site-notes.md "상호대차현황(신청현황)" / "상호대차현황 이력현황 탭" 절 참고. 페이지에는
 * 신청현황(type=0, 기본값)/이력현황(type=1) 두 탭이 있다. {@code type} 파라미터 없이 GET하면 기본값(신청현황)이
 * 그대로 내려와 별도 폼 제출이 필요 없지만, 이력현황은 페이지 자체 JS 함수 {@code fn_typeTabMove(1)}을 호출해
 * hidden form을 type=1로 재제출해야 한다. 두 탭 모두 도서정보/신청일/소장도서관/수령처/상태 컬럼 순서는 같고,
 * 이력현황에는 마지막 "취소" 컬럼만 없다(이미 종결된 건이라 취소 액션 자체가 없음). 신청취소 버튼은 이 서비스
 * 범위 밖(조회 전용).
 *
 * <p>⚠️ "대출가능" 판정(신청현황에만 해당): 실계정에서 확인된 상태 텍스트는 "대출중" 하나뿐이며, 이것이
 * 정확히 "수령 가능해진 시점"인지 "이미 대출 완료된 시점"인지 불확실하다. status_text는 원문 그대로 저장하고,
 * {@link com.woori.library.service.reservation.ReadyStatusMatcher}에서 키워드 매칭으로 처리한다.
 */
@Service
public class IsslMutualLoanScrapeService {

    private static final String MUTUAL_LOAN_LIST_PATH = "/mbr/mstd/mutualLoanList.do?mnidx=1551";
    private static final String ROW_SELECTOR = "table.tableType.tableType02 tbody tr";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int APPLIED_TAB_MIN_CELLS = 7; // 번호/도서정보/신청일/소장도서관/수령처/상태/취소
    private static final int HISTORY_TAB_MIN_CELLS = 6; // 취소 컬럼 없음

    private static final String MOVE_PAGE_JS =
        "n => { document.querySelector('input[name=pageNo]').value = n; "
            + "document.querySelector('form[name=frm]').method = 'post'; "
            + "document.querySelector('form[name=frm]').submit(); }";
    private static final String MOVE_TO_HISTORY_TAB_JS = "() => { window.fn_typeTabMove(1); }";

    private final String baseUrl;

    public IsslMutualLoanScrapeService(@Value("${app.crawler.issl.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<MutualLoanRecordDraft> fetchMutualLoans(IsslSession session) {
        Page page = session.page();
        page.navigate(
            baseUrl + MUTUAL_LOAN_LIST_PATH, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        return scrapeAllPages(page, APPLIED_TAB_MIN_CELLS);
    }

    public List<MutualLoanRecordDraft> fetchMutualLoanHistory(IsslSession session) {
        Page page = session.page();
        page.navigate(
            baseUrl + MUTUAL_LOAN_LIST_PATH, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForNavigation(
            new Page.WaitForNavigationOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED),
            () -> page.evaluate(MOVE_TO_HISTORY_TAB_JS));
        return scrapeAllPages(page, HISTORY_TAB_MIN_CELLS);
    }

    private List<MutualLoanRecordDraft> scrapeAllPages(Page page, int minCells) {
        List<MutualLoanRecordDraft> drafts = new ArrayList<>();
        int totalPages = countPages(page);
        for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
            if (pageNo > 1) {
                goToPage(page, pageNo);
            }
            drafts.addAll(scrapeCurrentPage(page, minCells));
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

    private List<MutualLoanRecordDraft> scrapeCurrentPage(Page page, int minCells) {
        List<MutualLoanRecordDraft> drafts = new ArrayList<>();
        Locator rows = page.locator(ROW_SELECTOR);
        int rowCount = rows.count();
        for (int i = 0; i < rowCount; i++) {
            Locator cells = rows.nth(i).locator("td");
            // 신청/이력 없음("조회되는 도서가 없습니다")은 colspan 단일 td라 minCells 미만 — 자연스럽게 건너뜀.
            if (cells.count() < minCells) {
                continue;
            }
            String bookTitle = normalizeText(cells.nth(1).textContent());
            LocalDate appliedAt = parseDate(cells.nth(2).textContent());
            String branchName = normalizeText(cells.nth(3).textContent());
            String pickupBranchName = normalizeText(cells.nth(4).textContent());
            String statusText = normalizeText(cells.nth(5).textContent());
            drafts.add(new MutualLoanRecordDraft(bookTitle, appliedAt, branchName, pickupBranchName, statusText));
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
