package com.woori.library.service.loan;

import com.woori.library.domain.LibraryAccount;
import com.woori.library.domain.LoanRecord;
import com.woori.library.domain.MutualLoanHistoryRecord;
import com.woori.library.domain.MutualLoanRecord;
import com.woori.library.domain.ReservationRecord;
import com.woori.library.dto.SyncResponse;
import com.woori.library.repository.LibraryAccountRepository;
import com.woori.library.repository.LoanRecordRepository;
import com.woori.library.repository.MutualLoanHistoryRecordRepository;
import com.woori.library.repository.MutualLoanRecordRepository;
import com.woori.library.repository.ReservationRecordRepository;
import com.woori.library.service.crawler.IsslAuthException;
import com.woori.library.service.crawler.IsslLoanScrapeService;
import com.woori.library.service.crawler.IsslLoginService;
import com.woori.library.service.crawler.IsslMutualLoanScrapeService;
import com.woori.library.service.crawler.IsslReservationScrapeService;
import com.woori.library.service.crawler.IsslSession;
import com.woori.library.service.crawler.LoanRecordDraft;
import com.woori.library.service.crawler.MutualLoanRecordDraft;
import com.woori.library.service.crawler.ReservationRecordDraft;
import com.woori.library.service.crypto.AesGcmCipherService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 도서관 계정별로 실제 크롤링을 수행하고, 대출현황/일반예약현황/상호대차현황(신청현황)을 한 로그인 세션 안에서
 * 함께 갱신한다. 세 화면을 매번 따로 로그인해서 긁으면 issl.go.kr에 불필요한 부하를 주므로(계정 잠금 위험) 한
 * 세션을 재사용한다.
 */
@Service
public class LoanSyncService {

    private static final Logger log = LoggerFactory.getLogger(LoanSyncService.class);

    private final LibraryAccountRepository libraryAccountRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final ReservationRecordRepository reservationRecordRepository;
    private final MutualLoanRecordRepository mutualLoanRecordRepository;
    private final MutualLoanHistoryRecordRepository mutualLoanHistoryRecordRepository;
    private final IsslLoginService isslLoginService;
    private final IsslLoanScrapeService isslLoanScrapeService;
    private final IsslReservationScrapeService isslReservationScrapeService;
    private final IsslMutualLoanScrapeService isslMutualLoanScrapeService;
    private final AesGcmCipherService cipherService;

    public LoanSyncService(
        LibraryAccountRepository libraryAccountRepository,
        LoanRecordRepository loanRecordRepository,
        ReservationRecordRepository reservationRecordRepository,
        MutualLoanRecordRepository mutualLoanRecordRepository,
        MutualLoanHistoryRecordRepository mutualLoanHistoryRecordRepository,
        IsslLoginService isslLoginService,
        IsslLoanScrapeService isslLoanScrapeService,
        IsslReservationScrapeService isslReservationScrapeService,
        IsslMutualLoanScrapeService isslMutualLoanScrapeService,
        AesGcmCipherService cipherService) {
        this.libraryAccountRepository = libraryAccountRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.reservationRecordRepository = reservationRecordRepository;
        this.mutualLoanRecordRepository = mutualLoanRecordRepository;
        this.mutualLoanHistoryRecordRepository = mutualLoanHistoryRecordRepository;
        this.isslLoginService = isslLoginService;
        this.isslLoanScrapeService = isslLoanScrapeService;
        this.isslReservationScrapeService = isslReservationScrapeService;
        this.isslMutualLoanScrapeService = isslMutualLoanScrapeService;
        this.cipherService = cipherService;
    }

    public SyncResponse sync(Long ownerUserId, Long onlyLibraryAccountId) {
        List<LibraryAccount> accounts = libraryAccountRepository.findByFamilyMember_OwnerUserId(ownerUserId);
        if (onlyLibraryAccountId != null) {
            accounts = accounts.stream().filter(a -> a.getId().equals(onlyLibraryAccountId)).toList();
        }

        int synced = 0;
        List<String> failed = new ArrayList<>();
        for (LibraryAccount account : accounts) {
            try {
                CrawlResult result = crawl(account);
                replaceLoanRecords(account, result.loans());
                mergeReservationRecords(account, result.reservations());
                mergeMutualLoanRecords(account, result.mutualLoans());
                replaceMutualLoanHistoryRecords(account, result.mutualLoanHistory());
                account.setLastLoginOk(true);
                account.setLastSyncedAt(Instant.now());
                libraryAccountRepository.save(account);
                synced++;
            } catch (IsslAuthException e) {
                log.warn("동기화 중 issl.go.kr 로그인 실패 (loginId={}): {}", account.getLoginId(), e.getMessage());
                account.setLastLoginOk(false);
                libraryAccountRepository.save(account);
                failed.add(account.getLoginId() + ": " + e.getMessage());
            } catch (RuntimeException e) {
                log.error("동기화 중 예상 못한 오류 (loginId={})", account.getLoginId(), e);
                failed.add(account.getLoginId() + ": " + e.getMessage());
            }
        }
        return new SyncResponse(synced, failed);
    }

    private record CrawlResult(
        List<LoanRecordDraft> loans,
        List<ReservationRecordDraft> reservations,
        List<MutualLoanRecordDraft> mutualLoans,
        List<MutualLoanRecordDraft> mutualLoanHistory) {}

    private CrawlResult crawl(LibraryAccount account) {
        String password = cipherService.decrypt(account.getEncryptedPassword());
        try (IsslSession session = isslLoginService.login(account.getLoginId(), password)) {
            List<LoanRecordDraft> loans = isslLoanScrapeService.fetchLoans(session);
            List<ReservationRecordDraft> reservations = isslReservationScrapeService.fetchReservations(session);
            List<MutualLoanRecordDraft> mutualLoans = isslMutualLoanScrapeService.fetchMutualLoans(session);
            List<MutualLoanRecordDraft> mutualLoanHistory = isslMutualLoanScrapeService.fetchMutualLoanHistory(session);
            return new CrawlResult(loans, reservations, mutualLoans, mutualLoanHistory);
        }
    }

    /** 대출현황은 상태를 유지할 필요가 없는 스냅샷이라 매번 전체 재작성한다. */
    private void replaceLoanRecords(LibraryAccount account, List<LoanRecordDraft> drafts) {
        loanRecordRepository.deleteAll(loanRecordRepository.findByLibraryAccountId(account.getId()));
        for (LoanRecordDraft draft : drafts) {
            loanRecordRepository.save(
                new LoanRecord(account, draft.bookTitle(), draft.branchName(), draft.loanDate(), draft.dueDate()));
        }
    }

    /** 이력현황은 이미 종결된 건이라 loan_record와 동일하게 알림 상태를 보존할 필요 없이 전체 재작성한다. */
    private void replaceMutualLoanHistoryRecords(LibraryAccount account, List<MutualLoanRecordDraft> drafts) {
        mutualLoanHistoryRecordRepository.deleteAll(mutualLoanHistoryRecordRepository.findByLibraryAccountId(account.getId()));
        for (MutualLoanRecordDraft draft : drafts) {
            mutualLoanHistoryRecordRepository.save(
                new MutualLoanHistoryRecord(
                    account, draft.bookTitle(), draft.appliedAt(), draft.branchName(), draft.pickupBranchName(),
                    draft.statusText()));
        }
    }

    /**
     * 예약/상호대차 건은 {@code ready_notified_at}(대출가능 알림 중복 발송 방지 플래그)을 보존해야 하므로,
     * loan_record처럼 전체 삭제 후 재삽입하지 않고 자연키(도서명+날짜) 기준으로 갱신/삽입/삭제한다.
     */
    private void mergeReservationRecords(LibraryAccount account, List<ReservationRecordDraft> drafts) {
        Map<String, ReservationRecord> existing = new HashMap<>();
        for (ReservationRecord r : reservationRecordRepository.findByLibraryAccountId(account.getId())) {
            existing.put(r.getBookTitle() + "|" + r.getReservedAt(), r);
        }
        for (ReservationRecordDraft draft : drafts) {
            String key = draft.bookTitle() + "|" + draft.reservedAt();
            ReservationRecord record = existing.remove(key);
            if (record == null) {
                record = new ReservationRecord(
                    account, draft.bookTitle(), draft.branchName(), draft.reservedAt(), draft.expiresAt(), draft.rank(),
                    draft.statusText());
            } else {
                record.setBranchName(draft.branchName());
                record.setExpiresAt(draft.expiresAt());
                record.setRank(draft.rank());
                record.setStatusText(draft.statusText());
                record.setFetchedAt(Instant.now());
            }
            reservationRecordRepository.save(record);
        }
        // drafts에 더 이상 없는 예약(취소/만료/대출 전환 등)은 삭제.
        reservationRecordRepository.deleteAll(existing.values());
    }

    private void mergeMutualLoanRecords(LibraryAccount account, List<MutualLoanRecordDraft> drafts) {
        Map<String, MutualLoanRecord> existing = new HashMap<>();
        for (MutualLoanRecord r : mutualLoanRecordRepository.findByLibraryAccountId(account.getId())) {
            existing.put(r.getBookTitle() + "|" + r.getAppliedAt(), r);
        }
        for (MutualLoanRecordDraft draft : drafts) {
            String key = draft.bookTitle() + "|" + draft.appliedAt();
            MutualLoanRecord record = existing.remove(key);
            if (record == null) {
                record = new MutualLoanRecord(
                    account, draft.bookTitle(), draft.appliedAt(), draft.branchName(), draft.pickupBranchName(),
                    draft.statusText());
            } else {
                record.setBranchName(draft.branchName());
                record.setPickupBranchName(draft.pickupBranchName());
                record.setStatusText(draft.statusText());
                record.setFetchedAt(Instant.now());
            }
            mutualLoanRecordRepository.save(record);
        }
        mutualLoanRecordRepository.deleteAll(existing.values());
    }
}
