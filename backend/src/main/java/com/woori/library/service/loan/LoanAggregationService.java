package com.woori.library.service.loan;

import com.woori.library.domain.LoanRecord;
import com.woori.library.dto.LoanResponse;
import com.woori.library.repository.LoanRecordRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** open-in-view=false라 LoanRecord.libraryAccount/familyMember 지연 로딩 접근에 트랜잭션이 필요하다. */
@Service
public class LoanAggregationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM.dd");

    private final LoanRecordRepository loanRecordRepository;

    public LoanAggregationService(LoanRecordRepository loanRecordRepository) {
        this.loanRecordRepository = loanRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoans(
        Long ownerUserId, List<Long> familyIds, List<String> libraryCodes, String sort, String dir) {
        List<LoanRecord> records = loanRecordRepository.findAllForOwner(ownerUserId);

        Set<Long> familyFilter = (familyIds == null || familyIds.isEmpty()) ? null : Set.copyOf(familyIds);
        Set<String> libraryFilter = (libraryCodes == null || libraryCodes.isEmpty()) ? null : Set.copyOf(libraryCodes);

        Comparator<LoanRecord> comparator =
            "loan".equals(sort) ? Comparator.comparing(LoanRecord::getLoanDate) : Comparator.comparing(LoanRecord::getDueDate);
        if ("desc".equals(dir)) {
            comparator = comparator.reversed();
        }

        LocalDate today = LocalDate.now();

        return records.stream()
            .filter(r -> familyFilter == null || familyFilter.contains(r.getLibraryAccount().getFamilyMember().getId()))
            .filter(r -> libraryFilter == null || libraryFilter.contains(r.getBranchName()))
            .sorted(comparator)
            .map(r -> toResponse(r, today))
            .toList();
    }

    private LoanResponse toResponse(LoanRecord r, LocalDate today) {
        int dday = (int) today.until(r.getDueDate(), java.time.temporal.ChronoUnit.DAYS);
        return new LoanResponse(
            r.getBookTitle(),
            r.getLoanDate().format(DATE_FMT),
            r.getDueDate().format(DATE_FMT),
            r.getBranchName(),
            r.getLibraryAccount().getFamilyMember().getName(),
            dday,
            dday < 0);
    }
}
