package com.woori.library.dto;

import java.util.List;

public record MemberResponse(Long id, String name, boolean isSelf, List<LibraryAccountResponse> libraryAccounts) {}
