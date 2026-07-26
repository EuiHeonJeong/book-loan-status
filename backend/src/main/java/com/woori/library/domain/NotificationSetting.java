package com.woori.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_setting")
@Getter
@Setter
@NoArgsConstructor
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false, unique = true)
    private Long ownerUserId;

    @Column(name = "due_alert_enabled", nullable = false)
    private boolean dueAlertEnabled = true;

    /** d3 | d2 | d1 | d0 */
    @Column(name = "due_alert_timing", nullable = false, length = 5)
    private String dueAlertTiming = "d3";

    public NotificationSetting(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
