package com.hajime.notification.infrastructure;

import com.hajime.notification.NotificationDelivery;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Free local sink: a durable deduplicated receipt, with best-effort diagnostic logging. */
@Component
@Profile("dev | test")
@ConditionalOnProperty(name = "hajime.outbox.local-enabled", havingValue = "true")
public class LocalNotificationDelivery implements NotificationDelivery {
  private final JdbcClient db;

  public LocalNotificationDelivery(JdbcClient db) {
    this.db = db;
  }

  public void deliver(UUID id, String type, String payload) {
    int inserted =
        db.sql(
                "insert into local_notification_receipt(event_id,event_type) values(:id,:type) on conflict(event_id) do nothing")
            .param("id", id)
            .param("type", type)
            .update();
    if (inserted == 1)
      org.slf4j.LoggerFactory.getLogger(getClass())
          .info("Local notification: event={} type={}", id, type);
  }
}
