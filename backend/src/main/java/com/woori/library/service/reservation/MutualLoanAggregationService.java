package com.woori.library.service.reservation;

import com.woori.library.domain.MutualLoanRecord;
import com.woori.library.dto.MutualLoanResponse;
import com.woori.library.repository.MutualLoanRecordRepository;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** open-in-view=false라 MutualLoanRecord.libraryAccount/familyMember 지연 로딩 접근에 트랜잭션이 필요하다. */
@Service
public class MutualLoanAggregationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM.dd");

    private final MutualLoanRecordRepository mutualLoanRecordRepository;

    public MutualLoanAggregationService(MutualLoanRecordRepository mutualLoanRecordRepository) {
        this.mutualLoanRecordRepository = mutualLoanRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<MutualLoanResponse> getMutualLoans(Long ownerUserId, List<Long> familyIds) {
        List<MutualLoanRecord> records = mutualLoanRecordRepository.findAllForOwner(ownerUserId);
        Set<Long> familyFilter = (familyIds == null || familyIds.isEmpty()) ? null : Set.copyOf(familyIds);

        return records.stream()
            .filter(r -> familyFilter == null || familyFilter.contains(r.getLibraryAccount().getFamilyMember().getId()))
            .sorted(Comparator.comparing(MutualLoanRecord::getAppliedAt))
            .map(this::toResponse)
            .toList();
    }

    private MutualLoanResponse toResponse(MutualLoanRecord r) {
        return new MutualLoanResponse(
            r.getBookTitle(),
            r.getBranchName(),
            r.getPickupBranchName(),
            r.getAppliedAt().format(DATE_FMT),
            r.getStatusText(),
            ReadyStatusMatcher.isMutualLoanReady(r.getStatusText()),
            r.getLibraryAccount().getFamilyMember().getName());
    }
}
