package com.hajime.identity.infrastructure;

import com.hajime.identity.UserDirectory.User;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final JdbcClient db;

  public UserRepository(JdbcClient db) {
    this.db = db;
  }

  public Optional<User> bySubject(String subject) {
    return db.sql("select id,display_name from app_user where subject=:subject")
        .param("subject", subject)
        .query(User.class)
        .optional();
  }
}
