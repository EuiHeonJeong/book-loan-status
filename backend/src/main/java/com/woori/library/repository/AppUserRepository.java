package com.woori.library.repository;

import com.woori.library.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByProviderAndProviderUserId(String provider, String providerUserId);
}
