package com.woori.library.service.notification;

import com.woori.library.domain.LoanRecord;
import com.woori.library.domain.PushSubscription;
import com.woori.library.repository.LoanRecordRepository;
import com.woori.library.repository.NotificationSettingRepository;
import com.woori.library.repository.PushSubscriptionRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 별도 빈으로 분리한 이유: {@code @Scheduled} 메서드가 같은 객체의 {@code @Transactional} 메서드를
 * 직접 호출하면 스프링 프록시를 우회해서 트랜잭션이 아예 안 걸린다(self-invocation). 이 프로젝트는
 * open-in-view=false라 LoanRecord.libraryAccount/familyMember 지연 로딩에 실제 트랜잭션이 필요하므로
 * {@link PushNotificationScheduler}와 반드시 분리해야 한다 — LoanAggregationService와 동일한 이유.
 */
@Service
public class NotificationDispatchService {

    private static final Map<String, Integer> DUE_TIMING_THRESHOLD = Map.of("d3", 3, "d2", 2, "d1", 1, "d0", 0);

    private final NotificationSettingRepository notificationSettingRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushNotificationService pushNotificationService;

    public NotificationDispatchService(
        NotificationSettingRepository notificationSettingRepository,
        LoanRecordRepository loanRecordRepository,
        PushSubscriptionRepository pushSubscriptionRepository,
        PushNotificationService pushNotificationService) {
        this.notificationSettingRepository = notificationSettingRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @Transactional(readOnly = true)
    public void dispatchForOwner(Long ownerUserId) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByOwnerUserId(ownerUserId);
        if (subscriptions.isEmpty()) {
            return;
        }

        var setting = notificationSettingRepository.findByOwnerUserId(ownerUserId);
        boolean dueAlertEnabled = setting.map(s -> s.isDueAlertEnabled()).orElse(true);
        String dueAlertTiming = setting.map(s -> s.getDueAlertTiming()).orElse("d3");
        int dueThreshold = DUE_TIMING_THRESHOLD.getOrDefault(dueAlertTiming, 3);

        LocalDate today = LocalDate.now();
        List<LoanRecord> loans = loanRecordRepository.findAllForOwner(ownerUserId);

        if (dueAlertEnabled) {
            List<String> dueSoon = loans.stream()
                .filter(l -> dday(l, today) == dueThreshold)
                .map(LoanRecord::getBookTitle)
                .toList();
            if (!dueSoon.isEmpty()) {
                sendToAll(subscriptions, "반납예정 도서 " + dueSoon.size() + "권", String.join(", ", dueSoon));
            }
        }

        // 연체는 별도 on/off 없이, 구독이 있는 한(위에서 이미 확인) 매일 자동으로 대상이다.
        List<String> overdue = loans.stream()
            .filter(l -> dday(l, today) < 0)
            .map(LoanRecord::getBookTitle)
            .toList();
        if (!overdue.isEmpty()) {
            sendToAll(subscriptions, "연체 도서 " + overdue.size() + "권", String.join(", ", overdue));
        }
    }

    private void sendToAll(List<PushSubscription> subscriptions, String title, String body) {
        for (PushSubscription subscription : subscriptions) {
            pushNotificationService.send(subscription, title, body, "/dashboard");
        }
    }

    private int dday(LoanRecord record, LocalDate today) {
        return (int) today.until(record.getDueDate(), ChronoUnit.DAYS);
    }
}
