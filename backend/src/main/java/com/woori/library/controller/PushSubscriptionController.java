package com.woori.library.controller;

import com.woori.library.config.AppOAuth2User;
import com.woori.library.domain.PushSubscription;
import com.woori.library.dto.PushSubscriptionRequest;
import com.woori.library.dto.UnsubscribeRequest;
import com.woori.library.repository.PushSubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push-subscriptions")
public class PushSubscriptionController {

    private final PushSubscriptionRepository repository;

    public PushSubscriptionController(PushSubscriptionRepository repository) {
        this.repository = repository;
    }

    /** endpoint 기준 upsert — 같은 브라우저가 재구독해도 새 행을 만들지 않는다. */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal AppOAuth2User principal, @RequestBody PushSubscriptionRequest request) {
        PushSubscription subscription = repository.findByEndpoint(request.endpoint()).orElseGet(PushSubscription::new);
        subscription.setOwnerUserId(principal.getAppUserId());
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        repository.save(subscription);
    }

    /**
     * endpoint가 본인 소유가 아니거나 이미 없으면 조용히 무시한다 — 스케줄러가 죽은 구독을 먼저
     * 지웠을 수도 있어(PushNotificationService 참고) 이걸 에러로 취급할 이유가 없다.
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@AuthenticationPrincipal AppOAuth2User principal, @RequestBody UnsubscribeRequest request) {
        repository.findByEndpoint(request.endpoint())
            .filter(s -> s.getOwnerUserId().equals(principal.getAppUserId()))
            .ifPresent(repository::delete);
    }
}
