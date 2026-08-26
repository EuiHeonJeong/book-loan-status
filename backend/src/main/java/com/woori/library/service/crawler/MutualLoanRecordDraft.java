package com.woori.library.service.crawler;

import java.time.LocalDate;

/** 크롤링으로 얻은, 아직 DB에 저장되지 않은 상호대차 신청 건 한 건(신청현황 탭). */
public record MutualLoanRecordDraft(
    String bookTitle, LocalDate appliedAt, String branchName, String pickupBranchName, String statusText) {}
