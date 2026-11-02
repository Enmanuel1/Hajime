package com.hajime.organization.infrastructure;

import com.hajime.organization.MembershipRole;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipRepository {
  private final JdbcClient db;

  public MembershipRepository(JdbcClient db) {
    this.db = db;
  }

  public List<MembershipRole> roles(UUID org, UUID user) {
    return db
        .sql(
            "select role from organization_membership where organization_id=:org and user_id=:user and active")
        .param("org", org)
        .param("user", user)
        .query(String.class)
        .list()
        .stream()
        .map(MembershipRole::valueOf)
        .toList();
  }

  public boolean supports(UUID org, UUID art) {
    return db.sql(
            "select exists(select 1 from organization_martial_art where organization_id=:org and martial_art_id=:art)")
        .param("org", org)
        .param("art", art)
        .query(Boolean.class)
        .single();
  }
}
