package com.woori.library.service.reservation;

import com.woori.library.domain.ReservationRecord;
import com.woori.library.dto.ReservationResponse;
import com.woori.library.repository.ReservationRecordRepository;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** open-in-view=false라 ReservationRecord.libraryAccount/familyMember 지연 로딩 접근에 트랜잭션이 필요하다. */
@Service
public class ReservationAggregationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM.dd");

    private final ReservationRecordRepository reservationRecordRepository;

    public ReservationAggregationService(ReservationRecordRepository reservationRecordRepository) {
        this.reservationRecordRepository = reservationRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservations(Long ownerUserId, List<Long> familyIds) {
        List<ReservationRecord> records = reservationRecordRepository.findAllForOwner(ownerUserId);
        Set<Long> familyFilter = (familyIds == null || familyIds.isEmpty()) ? null : Set.copyOf(familyIds);

        return records.stream()
            .filter(r -> familyFilter == null || familyFilter.contains(r.getLibraryAccount().getFamilyMember().getId()))
            .sorted(Comparator.comparing(ReservationRecord::getReservedAt))
            .map(this::toResponse)
            .toList();
    }

    private ReservationResponse toResponse(ReservationRecord r) {
        return new ReservationResponse(
            r.getBookTitle(),
            r.getBranchName(),
            r.getReservedAt().format(DATE_FMT),
            r.getExpiresAt() != null ? r.getExpiresAt().format(DATE_FMT) : null,
            r.getRank(),
            r.getStatusText(),
            ReadyStatusMatcher.isReservationReady(r.getStatusText()),
            r.getLibraryAccount().getFamilyMember().getName());
    }
}
