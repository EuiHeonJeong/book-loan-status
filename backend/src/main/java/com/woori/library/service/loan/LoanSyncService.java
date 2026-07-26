package com.woori.library.service.loan;

import com.woori.library.domain.LibraryAccount;
import com.woori.library.domain.LoanRecord;
import com.woori.library.dto.SyncResponse;
import com.woori.library.repository.LibraryAccountRepository;
import com.woori.library.repository.LoanRecordRepository;
import com.woori.library.service.crawler.IsslAuthException;
import com.woori.library.service.crawler.IsslLoanScrapeService;
import com.woori.library.service.crawler.IsslLoginService;
import com.woori.library.service.crawler.IsslSession;
import com.woori.library.service.crawler.LoanRecordDraft;
import com.woori.library.service.crypto.AesGcmCipherService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 도서관 계정별로 실제 크롤링을 수행하고, 그 결과로 loan_record를 갱신한다. */
@Service
public class LoanSyncService {

    private static final Logger log = LoggerFactory.getLogger(LoanSyncService.class);

    private final LibraryAccountRepository libraryAccountRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final IsslLoginService isslLoginService;
    private final IsslLoanScrapeService isslLoanScrapeService;
    private final AesGcmCipherService cipherService;

    public LoanSyncService(
        LibraryAccountRepository libraryAccountRepository,
        LoanRecordRepository loanRecordRepository,
        IsslLoginService isslLoginService,
        IsslLoanScrapeService isslLoanScrapeService,
        AesGcmCipherService cipherService) {
        this.libraryAccountRepository = libraryAccountRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.isslLoginService = isslLoginService;
        this.isslLoanScrapeService = isslLoanScrapeService;
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
                List<LoanRecordDraft> drafts = crawl(account);
                replaceRecords(account, drafts);
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

    private List<LoanRecordDraft> crawl(LibraryAccount account) {
        String password = cipherService.decrypt(account.getEncryptedPassword());
        try (IsslSession session = isslLoginService.login(account.getLoginId(), password)) {
            return isslLoanScrapeService.fetchLoans(session);
        }
    }

    private void replaceRecords(LibraryAccount account, List<LoanRecordDraft> drafts) {
        loanRecordRepository.deleteAll(loanRecordRepository.findByLibraryAccountId(account.getId()));
        for (LoanRecordDraft draft : drafts) {
            loanRecordRepository.save(
                new LoanRecord(account, draft.bookTitle(), draft.branchName(), draft.loanDate(), draft.dueDate()));
        }
        account.setLastLoginOk(true);
        account.setLastSyncedAt(Instant.now());
        libraryAccountRepository.save(account);
    }
}
