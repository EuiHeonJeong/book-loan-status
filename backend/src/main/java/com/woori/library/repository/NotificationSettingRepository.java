package com.woori.library.repository;

import com.woori.library.domain.NotificationSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByOwnerUserId(Long ownerUserId);
}
