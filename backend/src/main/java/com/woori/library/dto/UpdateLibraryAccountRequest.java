package com.woori.library.dto;

/** loginId/password 둘 다 선택 — 보낸 필드만 갱신한다. */
public record UpdateLibraryAccountRequest(String loginId, String password) {}
