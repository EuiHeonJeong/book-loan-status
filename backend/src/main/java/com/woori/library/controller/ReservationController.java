package com.woori.library.controller;

import com.woori.library.config.AppOAuth2User;
import com.woori.library.dto.ReservationResponse;
import com.woori.library.service.reservation.ReservationAggregationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationAggregationService reservationAggregationService;

    public ReservationController(ReservationAggregationService reservationAggregationService) {
        this.reservationAggregationService = reservationAggregationService;
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> getReservations(
        @AuthenticationPrincipal AppOAuth2User principal, @RequestParam(required = false) List<Long> familyIds) {
        return reservationAggregationService.getReservations(principal.getAppUserId(), familyIds);
    }
}
