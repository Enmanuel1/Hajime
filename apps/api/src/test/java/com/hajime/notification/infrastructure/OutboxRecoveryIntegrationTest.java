package com.hajime.notification.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hajime.notification.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "hajime.outbox.batch-size=1", "hajime.outbox.max-attempts=2",
      "hajime.outbox.delivery-timeout=PT0.1S", "hajime.outbox.lease-duration=PT2S",
      "hajime.outbox.max-concurrent-deliveries=1"
    })
class OutboxRecoveryIntegrationTest extends OutboxPostgresTest {
  @Autowired JdbcClient db;
  @Autowired OutboxStore store;
  @Autowired OutboxProcessor processor;
  @Autowired OutboxMaintenance maintenance;
  @MockitoBean NotificationDelivery delivery;

  @BeforeEach
  void emptyQueue() {
    db.sql("delete from outbox_event").update();
  }

  UUID event() {
    var id = UUID.randomUUID();
    db.sql(
            "insert into outbox_event(id,event_type,aggregate_id,payload) values(:id,'TEST',:id,'{}')")
        .param("id", id)
        .update();
    return id;
  }

  void expire(UUID id) {
    db.sql("update outbox_event set lease_until=now()-interval '1 second' where id=:id")
        .param("id", id)
        .update();
  }

  void due(UUID id) {
    db.sql("update outbox_event set next_attempt_at=now()-interval '1 second' where id=:id")
        .param("id", id)
        .update();
  }

  @Test
  void expiredClaimCanBeRecoveredButOldWorkerCannotAcknowledgeOrFailIt() {
    var id = event();
    var first = store.claim().orElseThrow();
    assertThat(first.id()).isEqualTo(id);
    assertThat(store.claim()).isEmpty();
    expire(id);
    var recovered = store.claim().orElseThrow();
    assertThat(recovered.token()).isNotEqualTo(first.token());
    assertThat(store.acknowledge(first)).isFalse();
    assertThat(store.fail(first, "OldFailure")).isFalse();
    assertThat(store.acknowledge(recovered)).isTrue();
  }

  @Test
  void exhaustedEventIsParkedUntilExplicitRequeue() {
    var id = event();
    var first = store.claim().orElseThrow();
    store.fail(first, "ProviderFailure");
    due(id);
    var second = store.claim().orElseThrow();
    store.fail(second, "ProviderFailure");
    assertThat(store.claim()).isEmpty();
    assertThat(
            db.sql("select dead_lettered_at is not null from outbox_event where id=:id")
                .param("id", id)
                .query(Boolean.class)
                .single())
        .isTrue();
    assertThat(maintenance.requeue(id)).isTrue();
    assertThat(maintenance.requeue(id)).isFalse();
    var retried = store.claim().orElseThrow();
    assertThat(retried.attempts()).isEqualTo(1);
    store.acknowledge(retried);
  }

  @Test
  void crashOnFinalAttemptAlsoBecomesADeadLetter() {
    var id = event();
    store.claim().orElseThrow();
    expire(id);
    store.claim().orElseThrow();
    expire(id);
    assertThat(store.claim()).isEmpty();
    assertThat(
            db.sql("select dead_lettered_at is not null from outbox_event where id=:id")
                .param("id", id)
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  @Test
  void competingWorkersCannotClaimTheSameActiveEvent() throws Exception {
    var id = event();
    var gate = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Optional<OutboxStore.Claim>> claim =
          () -> {
            gate.await(10, TimeUnit.SECONDS);
            return store.claim();
          };
      var a = executor.submit(claim);
      var b = executor.submit(claim);
      var claims =
          java.util.stream.Stream.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS))
              .flatMap(Optional::stream)
              .toList();
      assertThat(claims).hasSize(1);
      assertThat(claims.getFirst().id()).isEqualTo(id);
      store.acknowledge(claims.getFirst());
    }
  }

  @Test
  void timeoutIsBoundedAndUncooperativeProviderCannotSpawnUnlimitedCalls() throws Exception {
    var id = event();
    var release = new CountDownLatch(1);
    var exited = new CountDownLatch(1);
    var calls = new AtomicInteger();
    doAnswer(
            invocation -> {
              calls.incrementAndGet();
              try {
                while (release.getCount() > 0) {
                  try {
                    release.await();
                  } catch (InterruptedException ignored) {
                  }
                }
              } finally {
                exited.countDown();
              }
              return null;
            })
        .when(delivery)
        .deliver(eq(id), anyString(), anyString());
    try {
      var started = System.nanoTime();
      assertThat(processor.processBatch()).isEqualTo(1);
      assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
      assertThat(
              db.sql("select last_error from outbox_event where id=:id")
                  .param("id", id)
                  .query(String.class)
                  .single())
          .isEqualTo("DeliveryTimeout");
      due(id);
      assertThat(processor.processBatch()).isZero();
      assertThat(calls.get()).isEqualTo(1);
    } finally {
      release.countDown();
      assertThat(exited.await(5, TimeUnit.SECONDS)).isTrue();
    }
    // Park the timed-out event so other tests in this context cannot claim it.
    db.sql("update outbox_event set dead_lettered_at=now() where id=:id").param("id", id).update();
  }

  @Test
  void cleanupIsBoundedAndNeverDeletesPendingOrDeadEvents() {
    var done = event();
    var claimed = store.claim().orElseThrow();
    store.acknowledge(claimed);
    db.sql("update outbox_event set processed_at=now()-interval '40 days' where id=:id")
        .param("id", done)
        .update();
    var pending = event();
    var dead = event();
    db.sql("update outbox_event set dead_lettered_at=now() where id=:id")
        .param("id", dead)
        .update();
    assertThat(maintenance.purgeProcessed(Instant.now().minus(Duration.ofDays(30)), 1))
        .isEqualTo(1);
    assertThat(
            db.sql("select count(*) from outbox_event where id in (:ids)")
                .param("ids", List.of(pending, dead))
                .query(Long.class)
                .single())
        .isEqualTo(2);
    assertThatThrownBy(() -> maintenance.purgeProcessed(Instant.now().plusSeconds(3600), 10))
        .isInstanceOf(com.hajime.shared.BusinessException.class);
    // Do not leave a due event for the next test.
    db.sql("update outbox_event set dead_lettered_at=now() where id=:id")
        .param("id", pending)
        .update();
  }
}
