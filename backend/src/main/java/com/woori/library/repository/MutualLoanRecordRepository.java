package com.woori.library.repository;

import com.woori.library.domain.MutualLoanRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MutualLoanRecordRepository extends JpaRepository<MutualLoanRecord, Long> {

    @org.springframework.data.jpa.repository.Query(
        "select m from MutualLoanRecord m where m.libraryAccount.familyMember.ownerUserId = :ownerUserId")
    List<MutualLoanRecord> findAllForOwner(Long ownerUserId);

    List<MutualLoanRecord> findByLibraryAccountId(Long libraryAccountId);
}
