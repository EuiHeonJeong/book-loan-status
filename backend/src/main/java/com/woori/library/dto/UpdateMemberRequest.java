package com.woori.library.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRequest(@NotBlank String name) {}
