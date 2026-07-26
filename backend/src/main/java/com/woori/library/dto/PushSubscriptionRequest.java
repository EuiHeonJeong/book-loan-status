package com.woori.library.dto;

public record PushSubscriptionRequest(String endpoint, Keys keys) {
    public record Keys(String p256dh, String auth) {}
}
