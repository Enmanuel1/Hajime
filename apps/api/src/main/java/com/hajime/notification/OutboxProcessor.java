package com.hajime.notification;

public interface OutboxProcessor {
  int processBatch();
}
