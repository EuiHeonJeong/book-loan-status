package com.woori.library.repository;

import com.woori.library.domain.ReservationRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRecordRepository extends JpaRepository<ReservationRecord, Long> {

    @org.springframework.data.jpa.repository.Query(
        "select r from ReservationRecord r where r.libraryAccount.familyMember.ownerUserId = :ownerUserId")
    List<ReservationRecord> findAllForOwner(Long ownerUserId);

    List<ReservationRecord> findByLibraryAccountId(Long libraryAccountId);
}
