package com.woori.library.service.crawler;

import java.time.LocalDate;

/** 크롤링으로 얻은, 아직 DB에 저장되지 않은 대출 기록 한 건. */
public record LoanRecordDraft(String bookTitle, String branchName, LocalDate loanDate, LocalDate dueDate) {}
