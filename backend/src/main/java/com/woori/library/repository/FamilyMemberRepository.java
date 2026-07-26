package com.woori.library.repository;

import com.woori.library.domain.FamilyMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByOwnerUserId(Long ownerUserId);
}
