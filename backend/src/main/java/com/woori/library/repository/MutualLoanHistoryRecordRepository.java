package com.woori.library.repository;

import com.woori.library.domain.MutualLoanHistoryRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MutualLoanHistoryRecordRepository extends JpaRepository<MutualLoanHistoryRecord, Long> {

    @org.springframework.data.jpa.repository.Query(
        "select m from MutualLoanHistoryRecord m where m.libraryAccount.familyMember.ownerUserId = :ownerUserId")
    List<MutualLoanHistoryRecord> findAllForOwner(Long ownerUserId);

    List<MutualLoanHistoryRecord> findByLibraryAccountId(Long libraryAccountId);
}
