package com.hajime.audit.infrastructure;

import com.hajime.audit.AuditTrail;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Repository
public class SqlAuditTrail implements AuditTrail {
  private final JdbcClient db;
  private final ObjectMapper json;

  public SqlAuditTrail(JdbcClient db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      UUID actor, UUID org, String action, String type, UUID id, Map<String, String> metadata) {
    db.sql(
            "insert into audit_log(id,actor_user_id,organization_id,action,entity_type,entity_id,metadata) values(:id,:actor,:org,:action,:type,:entity,cast(:metadata as jsonb))")
        .param("id", UUID.randomUUID())
        .param("actor", actor)
        .param("org", org)
        .param("action", action)
        .param("type", type)
        .param("entity", id)
        .param("metadata", json.writeValueAsString(metadata))
        .update();
  }
}
