package com.woori.library.dto;

public record ReservationResponse(
    String title,
    String branch,
    String reservedAt,
    String expiresAt,
    Integer rank,
    String statusText,
    boolean ready,
    String memberName) {}
