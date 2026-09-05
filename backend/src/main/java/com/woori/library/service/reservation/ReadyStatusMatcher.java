package com.woori.library.service.reservation;

import java.util.List;

/**
 * 일반예약/상호대차 신청 건의 원문 상태 텍스트로 "대출가능(수령 대기)" 여부를 판정한다.
 *
 * <p>⚠️ 일반예약현황 키워드는 아직 확정값이 아니다(docs/issl-site-notes.md 참고) — 진행 중인 예약이 없어
 * "대출가능" 상태의 원문을 확인 못 했고 "도착"/"대출가능"을 후보로 추정 중이다. 상호대차현황은 실사용
 * 확인 결과 "대출중"이 아니라 "대출가능"일 때가 수령 가능 시점으로 확정됐다. 실제 사례가 더 쌓이면
 * 이 키워드 목록만 교체하면 되도록, 판정 로직을 이 한 곳에 모아둔다.
 */
public final class ReadyStatusMatcher {

    private static final List<String> RESERVATION_READY_KEYWORDS = List.of("도착", "대출가능");
    private static final List<String> MUTUAL_LOAN_READY_KEYWORDS = List.of("대출가능");

    private ReadyStatusMatcher() {}

    public static boolean isReservationReady(String statusText) {
        return matchesAny(statusText, RESERVATION_READY_KEYWORDS);
    }

    public static boolean isMutualLoanReady(String statusText) {
        return matchesAny(statusText, MUTUAL_LOAN_READY_KEYWORDS);
    }

    private static boolean matchesAny(String statusText, List<String> keywords) {
        if (statusText == null) {
            return false;
        }
        return keywords.stream().anyMatch(statusText::contains);
    }
}
