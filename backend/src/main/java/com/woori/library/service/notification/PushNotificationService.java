package com.woori.library.service.notification;

import com.woori.library.domain.PushSubscription;
import com.woori.library.repository.PushSubscriptionRepository;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Map;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 실제 브라우저 Web Push 발송. VAPID 서명 + RFC8291 페이로드 암호화는 nl.martijndwars:web-push가
 * 처리한다 — 직접 구현하면 리스크만 크고 얻는 게 없다.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    // PushService 생성자가 즉시 BC 프로바이더를 필요로 해서(Utils.loadPublicKey) @PostConstruct로는
    // 늦다 — 클래스 로딩 시점(빈 생성자보다 먼저)에 등록해야 한다.
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final PushSubscriptionRepository repository;
    private final ObjectMapper objectMapper;
    private final PushService pushService;

    public PushNotificationService(
        PushSubscriptionRepository repository,
        ObjectMapper objectMapper,
        @Value("${app.push.vapid.public-key}") String publicKey,
        @Value("${app.push.vapid.private-key}") String privateKey,
        @Value("${app.push.vapid.subject}") String subject) throws Exception {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    /** 실패해도 던지지 않는다 — 배치로 여러 구독/도서에 발송하는 도중 하나 실패가 전체를 막으면 안 된다. */
    public void send(PushSubscription subscription, String title, String body, String url) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", url));
            Notification notification = new Notification(
                subscription.getEndpoint(),
                subscription.getP256dh(),
                subscription.getAuth(),
                payload.getBytes(StandardCharsets.UTF_8));
            HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                log.info("[push] 구독 만료(상태 {}), 삭제: id={}", status, subscription.getId());
                repository.delete(subscription);
            } else if (status >= 300) {
                log.warn("[push] 발송 실패(상태 {}): id={}", status, subscription.getId());
            }
        } catch (Exception e) {
            log.warn("[push] 발송 중 예외: id={}", subscription.getId(), e);
        }
    }
}
