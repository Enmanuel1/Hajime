package com.hajime.identity.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {
  public record Account(
      UUID id, String email, String passwordHash, boolean active, String displayName) {
    @Override
    public String toString() {
      return "Account[redacted]";
    }
  }

  public record Session(UUID id, UUID userId, Instant expiresAt, Instant revokedAt) {
    public boolean activeAt(Instant now) {
      return revokedAt == null && expiresAt.isAfter(now);
    }
  }

  public record Refresh(String tokenHash, UUID sessionId, Instant consumedAt) {}

  private final JdbcClient db;

  public AuthRepository(JdbcClient db) {
    this.db = db;
  }

  public Optional<Account> account(String email) {
    return db.sql(
            "select c.user_id as id,c.email,c.password_hash,c.active,u.display_name from local_credential c join app_user u on u.id=c.user_id where c.email=:email")
        .param("email", email)
        .query(Account.class)
        .optional();
  }

  public Account lockAccount(UUID id) {
    return db.sql(
            "select c.user_id as id,c.email,c.password_hash,c.active,u.display_name from local_credential c join app_user u on u.id=c.user_id where c.user_id=:id for update of c")
        .param("id", id)
        .query(Account.class)
        .single();
  }

  public boolean createAccount(UUID id, String email, String hash, String name) {
    db.sql("insert into app_user(id,subject,display_name) values(:id,:subject,:name)")
        .param("id", id)
        .param("subject", "local:" + id)
        .param("name", name)
        .update();
    return db.sql(
                "insert into local_credential(user_id,email,password_hash) values(:id,:email,:hash) on conflict(email) do nothing")
            .param("id", id)
            .param("email", email)
            .param("hash", hash)
            .update()
        == 1;
  }

  public Session createSession(UUID user, Instant now, Instant expires) {
    var id = UUID.randomUUID();
    db.sql(
            "insert into auth_session(id,user_id,created_at,expires_at) values(:id,:user,:now,:expires)")
        .param("id", id)
        .param("user", user)
        .param("now", Timestamp.from(now))
        .param("expires", Timestamp.from(expires))
        .update();
    return new Session(id, user, expires, null);
  }

  public Optional<Refresh> refresh(String hash) {
    return db.sql(
            "select token_hash,session_id,consumed_at from refresh_token where token_hash=:hash")
        .param("hash", hash)
        .query(Refresh.class)
        .optional();
  }

  public Optional<Session> lockSession(UUID id) {
    return db.sql(
            "select id,user_id,expires_at,revoked_at from auth_session where id=:id for update")
        .param("id", id)
        .query(Session.class)
        .optional();
  }

  public void addRefresh(String hash, UUID session, Instant now) {
    db.sql(
            "insert into refresh_token(token_hash,session_id,created_at) values(:hash,:session,:now)")
        .param("hash", hash)
        .param("session", session)
        .param("now", Timestamp.from(now))
        .update();
  }

  public void consume(String hash, Instant now) {
    db.sql(
            "update refresh_token set consumed_at=:now where token_hash=:hash and consumed_at is null")
        .param("now", Timestamp.from(now))
        .param("hash", hash)
        .update();
  }

  public void revoke(UUID session, Instant now) {
    db.sql("update auth_session set revoked_at=coalesce(revoked_at,:now) where id=:id")
        .param("now", Timestamp.from(now))
        .param("id", session)
        .update();
  }

  public boolean activeSession(UUID session, UUID user, Instant now) {
    return db.sql(
            "select exists(select 1 from auth_session s join local_credential c on c.user_id=s.user_id where s.id=:session and s.user_id=:user and s.revoked_at is null and s.expires_at>:now and c.active)")
        .param("session", session)
        .param("user", user)
        .param("now", Timestamp.from(now))
        .query(Boolean.class)
        .single();
  }

  public int purgeExpiredSessions() {
    return db.sql(
            "delete from auth_session where id in (select id from auth_session where expires_at<clock_timestamp()-interval '1 day' order by expires_at,id limit 1000 for update skip locked)")
        .update();
  }
}
