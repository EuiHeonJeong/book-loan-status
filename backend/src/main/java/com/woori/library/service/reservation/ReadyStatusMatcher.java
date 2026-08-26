package com.woori.library.service.reservation;

import java.util.List;

/**
 * 일반예약/상호대차 신청 건의 원문 상태 텍스트로 "대출가능(수령 대기)" 여부를 판정한다.
 *
 * <p>⚠️ 두 키워드 목록 모두 확정값이 아니다(docs/issl-site-notes.md 참고). 일반예약현황은 진행 중인 예약이
 * 없어 "대출가능" 상태의 원문을 확인 못 했고("도착"/"대출가능"을 후보로 추정), 상호대차현황은 확인된 값이
 * "대출중" 하나뿐이라 이것이 정확히 "수령 가능해진 시점"인지 불확실하다. 실제 사례가 쌓이면 이 키워드
 * 목록만 교체하면 되도록, 판정 로직을 이 한 곳에 모아둔다.
 */
public final class ReadyStatusMatcher {

    private static final List<String> RESERVATION_READY_KEYWORDS = List.of("도착", "대출가능");
    private static final List<String> MUTUAL_LOAN_READY_KEYWORDS = List.of("대출중");

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
