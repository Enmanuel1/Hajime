package com.hajime.notification.infrastructure;

import com.hajime.notification.NotificationDelivery;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Development sink: no external delivery and no private journal text in logs. */
@Component
@Profile("dev | test")
@ConditionalOnProperty(name = "hajime.outbox.local-enabled", havingValue = "true")
public class LocalNotificationDelivery implements NotificationDelivery {
  public void deliver(UUID id, String type, String payload) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .info("Local notification: event={} type={}", id, type);
  }
}
