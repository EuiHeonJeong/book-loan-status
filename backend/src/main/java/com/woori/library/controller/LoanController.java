package com.woori.library.controller;

import com.woori.library.config.AppOAuth2User;
import com.woori.library.dto.LoanResponse;
import com.woori.library.dto.SyncRequest;
import com.woori.library.dto.SyncResponse;
import com.woori.library.service.loan.LoanAggregationService;
import com.woori.library.service.loan.LoanSyncService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoanController {

    private final LoanAggregationService loanAggregationService;
    private final LoanSyncService loanSyncService;

    public LoanController(LoanAggregationService loanAggregationService, LoanSyncService loanSyncService) {
        this.loanAggregationService = loanAggregationService;
        this.loanSyncService = loanSyncService;
    }

    @GetMapping("/loans")
    public List<LoanResponse> getLoans(
        @AuthenticationPrincipal AppOAuth2User principal,
        @RequestParam(required = false) List<Long> familyIds,
        @RequestParam(required = false) List<String> libraryCodes,
        @RequestParam(defaultValue = "due") String sort,
        @RequestParam(defaultValue = "asc") String dir) {
        return loanAggregationService.getLoans(principal.getAppUserId(), familyIds, libraryCodes, sort, dir);
    }

    /** 즉시 재크롤링. libraryAccountId를 지정하면 해당 계정만, 아니면 보유한 모든 도서관 계정을 동기화한다. */
    @PostMapping("/loans/sync")
    public SyncResponse sync(
        @AuthenticationPrincipal AppOAuth2User principal, @RequestBody(required = false) SyncRequest request) {
        Long libraryAccountId = request != null ? request.libraryAccountId() : null;
        return loanSyncService.sync(principal.getAppUserId(), libraryAccountId);
    }
}
