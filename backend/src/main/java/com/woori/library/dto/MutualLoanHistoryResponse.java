package com.woori.library.dto;

public record MutualLoanHistoryResponse(
    String title, String branch, String pickupBranch, String appliedAt, String statusText, String memberName) {}
