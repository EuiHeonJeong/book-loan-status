package com.woori.library.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLibraryAccountRequest(@NotBlank String loginId, @NotBlank String password) {}
