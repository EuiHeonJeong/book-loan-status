package com.woori.library.service.crawler;

import java.time.LocalDate;

/** 크롤링으로 얻은, 아직 DB에 저장되지 않은 일반예약 건 한 건. */
public record ReservationRecordDraft(
    String bookTitle, String branchName, LocalDate reservedAt, LocalDate expiresAt, Integer rank, String statusText) {}
