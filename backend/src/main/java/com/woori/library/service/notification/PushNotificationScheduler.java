package com.woori.library.service.notification;

import com.woori.library.repository.LibraryAccountRepository;
import com.woori.library.service.loan.LoanSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 정해진 시각에 전 계정을 돌며 issl.go.kr 동기화 후 반납임박/연체 푸시를 발송한다. */
@Component
public class PushNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationScheduler.class);

    private final LibraryAccountRepository libraryAccountRepository;
    private final LoanSyncService loanSyncService;
    private final NotificationDispatchService notificationDispatchService;

    public PushNotificationScheduler(
        LibraryAccountRepository libraryAccountRepository,
        LoanSyncService loanSyncService,
        NotificationDispatchService notificationDispatchService) {
        this.libraryAccountRepository = libraryAccountRepository;
        this.loanSyncService = loanSyncService;
        this.notificationDispatchService = notificationDispatchService;
    }

    @Scheduled(cron = "${app.push.schedule.cron}", zone = "Asia/Seoul")
    public void run() {
        for (Long ownerUserId : libraryAccountRepository.findDistinctOwnerUserIds()) {
            try {
                loanSyncService.sync(ownerUserId, null);
            } catch (RuntimeException e) {
                // sync()가 계정별 IsslAuthException은 이미 내부에서 처리하지만, Playwright 기동 실패 같은
                // 예상 못한 예외는 여기서 막아야 다른 계정 처리가 안 끊긴다.
                log.error("[push-scheduler] ownerUserId={} 동기화 실패", ownerUserId, e);
                continue;
            }
            try {
                notificationDispatchService.dispatchForOwner(ownerUserId);
            } catch (RuntimeException e) {
                log.error("[push-scheduler] ownerUserId={} 알림 발송 실패", ownerUserId, e);
            }
        }
    }
}
