package com.woori.library.dto;

public record LoanResponse(
    String title, String loanDate, String dueDate, String library, String memberName, int dday, boolean overdue) {}
