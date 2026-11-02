package com.hajime.notification.infrastructure;

import com.hajime.notification.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hajime.outbox.local-enabled", havingValue = "true")
public class ScheduledOutboxProcessor implements OutboxProcessor {
  private final OutboxStore store;
  private final NotificationDelivery delivery;
  private final MeterRegistry metrics;
  private final OutboxSettings settings;
  private final Semaphore deliverySlots;
  private final java.util.Set<Thread> workers = ConcurrentHashMap.newKeySet();
  private volatile boolean stopping;

  public ScheduledOutboxProcessor(
      OutboxStore store,
      NotificationDelivery delivery,
      MeterRegistry metrics,
      OutboxSettings settings) {
    this.store = store;
    this.delivery = delivery;
    this.metrics = metrics;
    this.settings = settings;
    this.deliverySlots = new Semaphore(settings.maxConcurrentDeliveries());
  }

  @PreDestroy
  void stop() {
    stopping = true;
    workers.forEach(Thread::interrupt);
  }

  @Scheduled(
      fixedDelayString = "${hajime.outbox.delay-ms:5000}",
      initialDelayString = "${hajime.outbox.initial-delay-ms:5000}")
  public int processBatch() {
    int claimed = 0;
    while (!stopping && claimed < settings.batchSize() && !Thread.currentThread().isInterrupted()) {
      if (!deliverySlots.tryAcquire()) break;
      java.util.Optional<OutboxStore.Claim> pending;
      try {
        pending = store.claim();
      } catch (RuntimeException e) {
        deliverySlots.release();
        throw e;
      }
      if (pending.isEmpty()) {
        deliverySlots.release();
        break;
      }
      claimed++;
      process(pending.get());
    }
    return claimed;
  }

  private void process(OutboxStore.Claim event) {
    var task =
        new FutureTask<Void>(
            () -> {
              delivery.deliver(event.id(), event.eventType(), event.payload());
              return null;
            });
    // Capacity stays occupied until the real call exits, even when a provider ignores cancellation.
    var worker =
        Thread.ofVirtual()
            .unstarted(
                () -> {
                  try {
                    task.run();
                  } finally {
                    deliverySlots.release();
                    workers.remove(Thread.currentThread());
                  }
                });
    workers.add(worker);
    try {
      worker.start();
    } catch (RuntimeException e) {
      workers.remove(worker);
      deliverySlots.release();
      throw e;
    }
    String failure = null;
    try {
      task.get(settings.deliveryTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      task.cancel(true);
      failure = "DeliveryTimeout";
    } catch (InterruptedException e) {
      task.cancel(true);
      Thread.currentThread().interrupt();
      failure = "DeliveryInterrupted";
    } catch (ExecutionException e) {
      failure = e.getCause().getClass().getSimpleName();
    }
    // A storage/ack failure must escape: the lease enables recovery, not a false delivery failure.
    if (failure == null) {
      if (store.acknowledge(event))
        metrics.counter("hajime.outbox.delivered", "type", event.eventType()).increment();
    } else if (store.fail(event, failure)) {
      metrics.counter("hajime.outbox.failed", "type", event.eventType()).increment();
    }
  }
}
