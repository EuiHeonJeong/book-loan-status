package com.woori.library.repository;

import com.woori.library.domain.LibraryAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryAccountRepository extends JpaRepository<LibraryAccount, Long> {
    List<LibraryAccount> findByFamilyMember_OwnerUserId(Long ownerUserId);

    @org.springframework.data.jpa.repository.Query(
        "select distinct la.familyMember.ownerUserId from LibraryAccount la")
    List<Long> findDistinctOwnerUserIds();
}
