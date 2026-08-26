package com.woori.library.dto;

public record MutualLoanResponse(
    String title,
    String branch,
    String pickupBranch,
    String appliedAt,
    String statusText,
    boolean ready,
    String memberName) {}
