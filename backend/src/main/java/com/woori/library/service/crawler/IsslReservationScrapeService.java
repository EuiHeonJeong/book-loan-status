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
 * 로그인된 세션에서 일반예약현황을 긁어온다.
 *
 * <p>docs/issl-site-notes.md "일반예약현황" 절의 실계정 검증 결과를 그대로 반영. 예약취소/전환신청 버튼은
 * 이 서비스 범위 밖(조회 전용)이라 스크래핑하지 않는다.
 *
 * <p>⚠️ "대출가능(수령 대기)" 상태의 정확한 원문 텍스트는 미확인 — 테스트 계정에 진행 중인 예약이 없어
 * 확인 못 했다. status_text는 원문 그대로 저장하고, 판정은 {@link com.woori.library.service.reservation.ReadyStatusMatcher}에서
 * 키워드 매칭으로 처리한다(재조사 후 매칭 로직만 교체하면 됨).
 */
@Service
public class IsslReservationScrapeService {

    private static final String RESERVATION_LIST_PATH = "/mbr/mstd/reservationList.do?mnidx=1550";
    private static final String ROW_SELECTOR = "table.tableType.tableType02 tbody tr";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String MOVE_PAGE_JS =
        "n => { document.querySelector('input[name=pageNo]').value = n; "
            + "document.querySelector('form[name=frm]').method = 'post'; "
            + "document.querySelector('form[name=frm]').submit(); }";

    private final String baseUrl;

    public IsslReservationScrapeService(@Value("${app.crawler.issl.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<ReservationRecordDraft> fetchReservations(IsslSession session) {
        Page page = session.page();
        page.navigate(
            baseUrl + RESERVATION_LIST_PATH, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        List<ReservationRecordDraft> drafts = new ArrayList<>();
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

    private List<ReservationRecordDraft> scrapeCurrentPage(Page page) {
        List<ReservationRecordDraft> drafts = new ArrayList<>();
        Locator rows = page.locator(ROW_SELECTOR);
        int rowCount = rows.count();
        for (int i = 0; i < rowCount; i++) {
            Locator cells = rows.nth(i).locator("td");
            // 예약 없음("조회되는 도서가 없습니다")은 colspan 단일 td라 9개 미만 — 자연스럽게 건너뜀.
            if (cells.count() < 9) {
                continue;
            }
            String bookTitle = normalizeText(cells.nth(1).textContent());
            LocalDate reservedAt = parseDate(cells.nth(2).textContent());
            LocalDate expiresAt = parseDateOrNull(cells.nth(3).textContent());
            String branchName = normalizeText(cells.nth(4).textContent());
            Integer rank = parseIntOrNull(cells.nth(5).textContent());
            String statusText = normalizeText(cells.nth(6).textContent());
            drafts.add(new ReservationRecordDraft(bookTitle, branchName, reservedAt, expiresAt, rank, statusText));
        }
        return drafts;
    }

    private String normalizeText(String raw) {
        return raw.replaceAll("[\\s\\u200B]+", " ").trim();
    }

    private LocalDate parseDate(String raw) {
        return LocalDate.parse(raw.replaceAll("[\\s\\u200B]+", ""), DATE_FMT);
    }

    private LocalDate parseDateOrNull(String raw) {
        String cleaned = raw.replaceAll("[\\s\\u200B]+", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(cleaned, DATE_FMT);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String raw) {
        String cleaned = raw.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? null : Integer.parseInt(cleaned);
    }
}
