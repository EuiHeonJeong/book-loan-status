package com.woori.library.controller;

import com.woori.library.config.AppOAuth2User;
import com.woori.library.dto.MutualLoanHistoryResponse;
import com.woori.library.dto.MutualLoanResponse;
import com.woori.library.service.reservation.MutualLoanAggregationService;
import com.woori.library.service.reservation.MutualLoanHistoryAggregationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MutualLoanController {

    private final MutualLoanAggregationService mutualLoanAggregationService;
    private final MutualLoanHistoryAggregationService mutualLoanHistoryAggregationService;

    public MutualLoanController(
        MutualLoanAggregationService mutualLoanAggregationService,
        MutualLoanHistoryAggregationService mutualLoanHistoryAggregationService) {
        this.mutualLoanAggregationService = mutualLoanAggregationService;
        this.mutualLoanHistoryAggregationService = mutualLoanHistoryAggregationService;
    }

    @GetMapping("/mutual-loans")
    public List<MutualLoanResponse> getMutualLoans(
        @AuthenticationPrincipal AppOAuth2User principal, @RequestParam(required = false) List<Long> familyIds) {
        return mutualLoanAggregationService.getMutualLoans(principal.getAppUserId(), familyIds);
    }

    @GetMapping("/mutual-loans/history")
    public List<MutualLoanHistoryResponse> getMutualLoanHistory(
        @AuthenticationPrincipal AppOAuth2User principal, @RequestParam(required = false) List<Long> familyIds) {
        return mutualLoanHistoryAggregationService.getMutualLoanHistory(principal.getAppUserId(), familyIds);
    }
}
