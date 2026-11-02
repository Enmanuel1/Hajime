package com.hajime.martialart.infrastructure;

import com.hajime.martialart.Catalog.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
  private final JdbcClient db;

  public CatalogRepository(JdbcClient db) {
    this.db = db;
  }

  public List<Art> arts() {
    return db.sql("select id,code,name from martial_art where active order by code")
        .query(Art.class)
        .list();
  }

  public Optional<Art> art(String code) {
    return db.sql("select id,code,name from martial_art where code=:code and active")
        .param("code", code)
        .query(Art.class)
        .optional();
  }

  public Optional<Art> art(UUID id) {
    return db.sql("select id,code,name from martial_art where id=:id and active")
        .param("id", id)
        .query(Art.class)
        .optional();
  }

  public List<Rank> ranks(UUID id) {
    return db.sql(
            "select r.id,r.rank_system_id,r.code,r.display_name,r.order_index,r.belt_color from rank r join rank_system s on s.id=r.rank_system_id where s.martial_art_id=:id order by s.name,r.order_index")
        .param("id", id)
        .query(Rank.class)
        .list();
  }

  public List<Technique> techniques(UUID id, int page, int size) {
    return db.sql(
            "select id,category_id,code,name,native_name,description from technique where martial_art_id=:id and active order by code limit :size offset :offset")
        .param("id", id)
        .param("size", size)
        .param("offset", page * size)
        .query(Technique.class)
        .list();
  }

  public long activeTechniqueCount(UUID art, Set<UUID> ids) {
    return db.sql(
            "select count(*) from technique where martial_art_id=:art and id in (:ids) and active")
        .param("art", art)
        .param("ids", ids)
        .query(Long.class)
        .single();
  }
}
