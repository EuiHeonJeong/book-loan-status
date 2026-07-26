package com.woori.library.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(@NotBlank String name) {}
