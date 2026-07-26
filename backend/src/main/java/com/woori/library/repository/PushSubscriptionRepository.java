package com.woori.library.repository;

import com.woori.library.domain.PushSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    List<PushSubscription> findByOwnerUserId(Long ownerUserId);
    Optional<PushSubscription> findByEndpoint(String endpoint);
}
