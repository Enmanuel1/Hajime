package com.hajime.audit;

import java.util.*;

public interface AuditTrail {
  void record(
      UUID actorUserId,
      UUID organizationId,
      String action,
      String entityType,
      UUID entityId,
      Map<String, String> metadata);
}
