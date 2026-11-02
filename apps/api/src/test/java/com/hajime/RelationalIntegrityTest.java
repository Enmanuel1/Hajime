package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

class RelationalIntegrityTest extends ApiTest {
  @Autowired JdbcClient db;

  @Test
  void aReviewCannotChangeTheTrainingOrganization() throws Exception {
    var session = UUID.randomUUID();
    ok("athlete", "POST", "/api/v1/me/training-sessions", training(session), 201);
    assertThatThrownBy(
            () ->
                db.sql(
                        "insert into training_review(id,training_session_id,practitioner_profile_id,organization_id,martial_art_id,status,created_at) values(:id,:session,:profile,:org,'10000000-0000-0000-0000-000000000001','PENDING',now())")
                    .param("id", UUID.randomUUID())
                    .param("session", session)
                    .param("profile", PROFILE)
                    .param("org", OTHER_DOJO)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void feedbackAndGoalsCannotReferenceTrainingInAnotherOrganization() throws Exception {
    var session = UUID.randomUUID();
    ok("athlete", "POST", "/api/v1/me/training-sessions", training(session), 201);
    ok(
        "athlete",
        "POST",
        "/api/v1/me/training-sessions/" + session + "/share",
        Map.of("version", 0),
        200);
    var review =
        db.sql("select id from training_review where training_session_id=:id")
            .param("id", session)
            .query(UUID.class)
            .single();
    assertThatThrownBy(
            () ->
                db.sql(
                        "insert into feedback(id,review_id,training_session_id,practitioner_profile_id,author_user_id,organization_id,body,created_at) values(:id,:review,:session,:profile,'20000000-0000-0000-0000-000000000002',:org,'Feedback',now())")
                    .param("id", UUID.randomUUID())
                    .param("review", review)
                    .param("session", session)
                    .param("profile", PROFILE)
                    .param("org", OTHER_DOJO)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                db.sql(
                        "insert into goal(id,practitioner_profile_id,organization_id,martial_art_id,assigned_by_user_id,source_training_session_id,title,objective,priority,status,created_at,updated_at) values(:id,:profile,:org,'10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002',:session,'Practice','Balance','NORMAL','ACTIVE',now(),now())")
                    .param("id", UUID.randomUUID())
                    .param("profile", PROFILE)
                    .param("org", OTHER_DOJO)
                    .param("session", session)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
