package com.woori.library.controller;

import com.woori.library.config.AppOAuth2User;
import com.woori.library.domain.NotificationSetting;
import com.woori.library.dto.NotificationSettingRequest;
import com.woori.library.dto.NotificationSettingResponse;
import com.woori.library.repository.NotificationSettingRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification-settings")
public class NotificationSettingController {

    private final NotificationSettingRepository repository;

    public NotificationSettingController(NotificationSettingRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public NotificationSettingResponse get(@AuthenticationPrincipal AppOAuth2User principal) {
        return toResponse(findOrCreate(principal.getAppUserId()));
    }

    @PutMapping
    public NotificationSettingResponse update(
        @AuthenticationPrincipal AppOAuth2User principal, @RequestBody NotificationSettingRequest request) {
        NotificationSetting setting = findOrCreate(principal.getAppUserId());
        if (request.dueAlertEnabled() != null) {
            setting.setDueAlertEnabled(request.dueAlertEnabled());
        }
        if (request.dueAlertTiming() != null) {
            setting.setDueAlertTiming(request.dueAlertTiming());
        }
        return toResponse(repository.save(setting));
    }

    private NotificationSetting findOrCreate(Long ownerUserId) {
        return repository.findByOwnerUserId(ownerUserId).orElseGet(() -> repository.save(new NotificationSetting(ownerUserId)));
    }

    private NotificationSettingResponse toResponse(NotificationSetting s) {
        return new NotificationSettingResponse(s.isDueAlertEnabled(), s.getDueAlertTiming());
    }
}
