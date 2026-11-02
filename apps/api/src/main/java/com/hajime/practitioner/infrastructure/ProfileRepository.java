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

  public Optional<Profile> create(UUID id, UUID user, UUID art, java.math.BigDecimal weight) {
    return db.sql(
            "insert into practitioner_profile(id,user_id,martial_art_id,weight_kg,status) values(:id,:user,:art,:weight,'ACTIVE') on conflict(user_id,martial_art_id) do nothing returning id,user_id,martial_art_id,current_rank_id,weight_kg,status")
        .param("id", id)
        .param("user", user)
        .param("art", art)
        .param("weight", weight)
        .query(Profile.class)
        .optional();
  }

  public Optional<Profile> owned(UUID id, UUID user) {
    return db.sql(
            "select id,user_id,martial_art_id,current_rank_id,weight_kg,status from practitioner_profile where id=:id and user_id=:user")
        .param("id", id)
        .param("user", user)
        .query(Profile.class)
        .optional();
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
