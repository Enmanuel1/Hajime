package com.hajime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hajime.notification.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
class OutboxIntegrationTest extends PostgresTest {
  @Autowired JdbcClient db;
  @Autowired OutboxProcessor processor;
  @MockitoBean NotificationDelivery delivery;

  @Test
  void failedDeliveryIsRetriedAndAcknowledgedOnlyAfterSuccess() {
    var id = UUID.randomUUID();
    db.sql(
            "insert into outbox_event(id,event_type,aggregate_id,payload) values(:id,'TRAINING_SHARED',:aggregate,'{}')")
        .param("id", id)
        .param("aggregate", UUID.randomUUID())
        .update();
    doThrow(new IllegalStateException("Temporary failure"))
        .when(delivery)
        .deliver(eq(id), anyString(), anyString());
    processor.processBatch();
    assertThat(
            db.sql(
                    "select attempts from outbox_event where id=:id and processed_at is null and next_attempt_at>now()")
                .param("id", id)
                .query(Integer.class)
                .single())
        .isEqualTo(1);
    doNothing().when(delivery).deliver(eq(id), anyString(), anyString());
    db.sql("update outbox_event set next_attempt_at=now() where id=:id").param("id", id).update();
    processor.processBatch();
    assertThat(
            db.sql(
                    "select attempts from outbox_event where id=:id and processed_at is not null and last_error is null")
                .param("id", id)
                .query(Integer.class)
                .single())
        .isEqualTo(2);
    verify(delivery, times(2)).deliver(eq(id), eq("TRAINING_SHARED"), eq("{}"));
  }
}
