package com.hajime.practitioner.infrastructure;

import com.hajime.practitioner.Practitioners.Profile;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
  private final JdbcClient db;

  public ProfileRepository(JdbcClient db) {
    this.db = db;
  }

  public List<Profile> ownedBy(UUID user) {
    return db.sql(
            "select id,user_id,martial_art_id,current_rank_id,weight_kg,status from practitioner_profile where user_id=:user order by id")
        .param("user", user)
        .query(Profile.class)
        .list();
  }

  public Optional<Profile> find(UUID id) {
    return db.sql(
            "select id,user_id,martial_art_id,current_rank_id,weight_kg,status from practitioner_profile where id=:id")
        .param("id", id)
        .query(Profile.class)
        .optional();
  }
}
