package com.woori.library.controller;

import com.woori.library.config.AppOAuth2User;
import com.woori.library.dto.MeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppOAuth2User principal) {
        return new MeResponse(principal.getAppUserId(), principal.getDisplayName(), principal.getProvider());
    }
}
