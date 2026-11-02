package com.hajime.notification.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class LocalNotificationDeliveryIntegrationTest extends OutboxPostgresTest {
  @Autowired LocalNotificationDelivery delivery;
  @Autowired JdbcClient db;

  @Test
  void simultaneousDuplicateDeliveryCreatesOneDurableReceipt() throws Exception {
    var id = UUID.randomUUID();
    var gate = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Void> send =
          () -> {
            gate.await(10, TimeUnit.SECONDS);
            delivery.deliver(id, "TEST", "{}");
            return null;
          };
      var a = executor.submit(send);
      var b = executor.submit(send);
      a.get(10, TimeUnit.SECONDS);
      b.get(10, TimeUnit.SECONDS);
    }
    assertThat(
            db.sql("select count(*) from local_notification_receipt where event_id=:id")
                .param("id", id)
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }

  @Autowired OutboxStore store;

  @Test
  void crashAfterDeliveryBeforeAcknowledgmentDoesNotDuplicateTheLocalEffect() {
    var id = UUID.randomUUID();
    db.sql("delete from outbox_event").update();
    db.sql(
            "insert into outbox_event(id,event_type,aggregate_id,payload) values(:id,'TEST',:id,'{}')")
        .param("id", id)
        .update();
    var first = store.claim().orElseThrow();
    delivery.deliver(id, "TEST", "{}");
    db.sql("update outbox_event set lease_until=now()-interval '1 second' where id=:id")
        .param("id", id)
        .update();
    var recovered = store.claim().orElseThrow();
    delivery.deliver(id, "TEST", "{}");
    store.acknowledge(recovered);
    assertThat(store.acknowledge(first)).isFalse();
    assertThat(
            db.sql("select count(*) from local_notification_receipt where event_id=:id")
                .param("id", id)
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }
}
