package com.woori.library.service.reservation;

import com.woori.library.domain.MutualLoanHistoryRecord;
import com.woori.library.dto.MutualLoanHistoryResponse;
import com.woori.library.repository.MutualLoanHistoryRecordRepository;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** open-in-view=false라 MutualLoanHistoryRecord.libraryAccount/familyMember 지연 로딩 접근에 트랜잭션이 필요하다. */
@Service
public class MutualLoanHistoryAggregationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM.dd");

    private final MutualLoanHistoryRecordRepository mutualLoanHistoryRecordRepository;

    public MutualLoanHistoryAggregationService(MutualLoanHistoryRecordRepository mutualLoanHistoryRecordRepository) {
        this.mutualLoanHistoryRecordRepository = mutualLoanHistoryRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<MutualLoanHistoryResponse> getMutualLoanHistory(Long ownerUserId, List<Long> familyIds) {
        List<MutualLoanHistoryRecord> records = mutualLoanHistoryRecordRepository.findAllForOwner(ownerUserId);
        Set<Long> familyFilter = (familyIds == null || familyIds.isEmpty()) ? null : Set.copyOf(familyIds);

        return records.stream()
            .filter(r -> familyFilter == null || familyFilter.contains(r.getLibraryAccount().getFamilyMember().getId()))
            .sorted(Comparator.comparing(MutualLoanHistoryRecord::getAppliedAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    private MutualLoanHistoryResponse toResponse(MutualLoanHistoryRecord r) {
        return new MutualLoanHistoryResponse(
            r.getBookTitle(),
            r.getBranchName(),
            r.getPickupBranchName(),
            r.getAppliedAt().format(DATE_FMT),
            r.getStatusText(),
            r.getLibraryAccount().getFamilyMember().getName());
    }
}
