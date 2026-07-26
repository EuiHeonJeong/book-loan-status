package com.woori.library.repository;

import com.woori.library.domain.LoanRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRecordRepository extends JpaRepository<LoanRecord, Long> {

    @org.springframework.data.jpa.repository.Query(
        "select l from LoanRecord l where l.libraryAccount.familyMember.ownerUserId = :ownerUserId")
    List<LoanRecord> findAllForOwner(Long ownerUserId);

    List<LoanRecord> findByLibraryAccountId(Long libraryAccountId);
}
