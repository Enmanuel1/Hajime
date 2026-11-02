package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.*;

/** No dev/test profile: every protected request uses the real local authentication chain. */
class OrganizationOnboardingApiTest extends ProductionApiSupport {
  String create(String token) throws Exception {
    var result =
        request(
            "POST",
            "/api/v1/organizations",
            Map.of("name", "My dojo", "martialArtCode", "JUDO"),
            token);
    assertThat(result.status()).isEqualTo(201);
    return result.body().path("id").asText();
  }

  Result invite(String org, JsonNode recipient, String role, String token) throws Exception {
    return request(
        "POST",
        "/api/v1/organizations/" + org + "/invitations",
        Map.of("recipientUserId", recipient.path("user").path("id").asText(), "role", role),
        token);
  }

  Result accept(Result invitation, String token) throws Exception {
    return request(
        "POST",
        "/api/v1/me/organization-invitations/" + invitation.body().path("id").asText() + "/accept",
        null,
        token);
  }

  @Test
  void realAccountsCanJoinTrainAndReachCoachingWithoutSeedMemberships() throws Exception {
    var director = register(email());
    var athlete = register(email());
    var coach = register(email());
    var org = create(access(director));
    assertThat(
            request("GET", "/api/v1/me/organizations", null, access(director))
                .body()
                .path("content")
                .toString())
        .contains(org);
    assertThat(
            request("GET", "/api/v1/me/organizations", null, access(athlete))
                .body()
                .path("content")
                .toString())
        .doesNotContain(org);
    var invitation = invite(org, athlete, "PRACTITIONER", access(director));
    assertThat(invitation.status()).isEqualTo(201);
    assertThat(accept(invitation, access(coach)).status()).isEqualTo(404);
    assertThat(
            request("GET", "/api/v1/me/organization-invitations", null, access(athlete))
                .body()
                .path("content")
                .size())
        .isEqualTo(1);
    assertThat(accept(invitation, access(athlete)).status()).isEqualTo(204);
    assertThat(accept(invitation, access(athlete)).status()).isEqualTo(409);
    var coachInvite = invite(org, coach, "INSTRUCTOR", access(director));
    assertThat(accept(coachInvite, access(coach)).status()).isEqualTo(204);
    assertThat(
            request("GET", "/api/v1/organizations/" + org + "/members", null, access(coach))
                .status())
        .isEqualTo(200);
    assertThat(
            request("GET", "/api/v1/organizations/" + org + "/members", null, access(athlete))
                .status())
        .isEqualTo(403);
    var profile =
        request(
            "POST",
            "/api/v1/me/practitioner-profiles",
            Map.of("martialArtCode", "JUDO"),
            access(athlete));
    var training =
        request(
            "POST",
            "/api/v1/me/training-sessions",
            Map.of(
                "practitionerProfileId",
                profile.body().path("id").asText(),
                "organizationId",
                org,
                "occurredAt",
                "2026-01-01T12:00:00Z",
                "durationMinutes",
                60,
                "practiceType",
                "TECHNICAL",
                "techniques",
                List.of(),
                "sparring",
                List.of()),
            access(athlete));
    assertThat(training.status()).isEqualTo(201);
    var shared =
        request(
            "POST",
            "/api/v1/me/training-sessions/" + training.body().path("id").asText() + "/share",
            Map.of("version", training.body().path("version").asLong()),
            access(athlete));
    assertThat(shared.status()).isEqualTo(200);
    assertThat(
            request(
                    "GET",
                    "/api/v1/organizations/" + org + "/training-reviews",
                    null,
                    access(coach))
                .body()
                .path("content")
                .size())
        .isEqualTo(1);
    var goal =
        request(
            "POST",
            "/api/v1/organizations/"
                + org
                + "/practitioners/"
                + profile.body().path("id").asText()
                + "/goals",
            Map.of(
                "title", "Improve technique", "objective", "Practice safely", "priority", "NORMAL"),
            access(coach));
    assertThat(goal.status()).isEqualTo(201);
    assertThat(
            db.sql("select count(*) from audit_log where organization_id=:org")
                .param("org", UUID.fromString(org))
                .query(Long.class)
                .single())
        .isGreaterThanOrEqualTo(5);
  }

  @Test
  void organizationIsolationAndPrivilegeBoundariesHold() throws Exception {
    var owner = register(email());
    var outsider = register(email());
    var org = create(access(owner));
    var foreign = create(access(outsider));
    assertThat(invite(org, outsider, "ADMIN", access(owner)).status()).isEqualTo(400);
    assertThat(invite(org, outsider, "DIRECTOR", access(owner)).status()).isEqualTo(400);
    assertThat(invite(org, owner, "INSTRUCTOR", access(outsider)).status()).isEqualTo(403);
    assertThat(
            request("GET", "/api/v1/organizations/" + org + "/members", null, access(outsider))
                .status())
        .isEqualTo(403);
    var member =
        request("GET", "/api/v1/organizations/" + org + "/members", null, access(owner))
            .body()
            .path("content")
            .get(0)
            .path("id")
            .asText();
    assertThat(
            request(
                    "DELETE",
                    "/api/v1/organizations/" + org + "/members/" + member,
                    null,
                    access(owner))
                .status())
        .isEqualTo(403);
    assertThat(
            request(
                    "DELETE",
                    "/api/v1/organizations/" + foreign + "/members/" + member,
                    null,
                    access(outsider))
                .status())
        .isEqualTo(404);
    assertThat(
            request(
                    "POST",
                    "/api/v1/organizations",
                    Map.of("name", "Bad", "martialArtCode", "JUDO", "ownerId", UUID.randomUUID()),
                    access(owner))
                .status())
        .isEqualTo(400);
    assertThat(
            request(
                    "POST",
                    "/api/v1/organizations",
                    Map.of("name", " ", "martialArtCode", "JUDO"),
                    access(owner))
                .status())
        .isEqualTo(400);
    assertThat(request("GET", "/api/v1/me/organizations?size=101", null, access(owner)).status())
        .isEqualTo(400);
    assertThat(request("GET", "/api/v1/me/organizations", null, null).status()).isEqualTo(401);
  }

  @Test
  void revokingACoachRemovesPermissionsAndCancelsPendingInvitations() throws Exception {
    var owner = register(email());
    var coach = register(email());
    var org = create(access(owner));
    var invitation = invite(org, coach, "INSTRUCTOR", access(owner));
    assertThat(accept(invitation, access(coach)).status()).isEqualTo(204);
    var pending = invite(org, coach, "SENSEI", access(owner));
    var members =
        request("GET", "/api/v1/organizations/" + org + "/members", null, access(owner))
            .body()
            .path("content");
    String member = null;
    for (var row : members)
      if (row.path("role").asText().equals("INSTRUCTOR")) member = row.path("id").asText();
    assertThat(member).isNotNull();
    assertThat(
            request(
                    "DELETE",
                    "/api/v1/organizations/" + org + "/members/" + member,
                    null,
                    access(owner))
                .status())
        .isEqualTo(204);
    assertThat(
            request(
                    "DELETE",
                    "/api/v1/organizations/" + org + "/members/" + member,
                    null,
                    access(owner))
                .status())
        .isEqualTo(204);
    assertThat(
            request(
                    "GET",
                    "/api/v1/organizations/" + org + "/training-reviews",
                    null,
                    access(coach))
                .status())
        .isEqualTo(403);
    assertThat(accept(pending, access(coach)).status()).isEqualTo(409);
    var fresh = invite(org, coach, "INSTRUCTOR", access(owner));
    assertThat(accept(fresh, access(coach)).status()).isEqualTo(204);
  }

  @Test
  void expiredAndReplacedInvitationsCannotBeAccepted() throws Exception {
    var owner = register(email());
    var athlete = register(email());
    var org = create(access(owner));
    var first = invite(org, athlete, "PRACTITIONER", access(owner));
    var second = invite(org, athlete, "INSTRUCTOR", access(owner));
    assertThat(accept(first, access(athlete)).status()).isEqualTo(409);
    db.sql(
            "update organization_invitation set created_at=now()-interval '9 days',expires_at=now()-interval '2 days' where id=:id")
        .param("id", UUID.fromString(second.body().path("id").asText()))
        .update();
    assertThat(accept(second, access(athlete)).status()).isEqualTo(409);
    assertThat(
            request("GET", "/api/v1/me/organization-invitations", null, access(athlete))
                .body()
                .path("content"))
        .isEmpty();
  }

  @Test
  void concurrentAcceptanceConsumesInvitationOnlyOnce() throws Exception {
    var owner = register(email());
    var athlete = register(email());
    var org = create(access(owner));
    var invitation = invite(org, athlete, "PRACTITIONER", access(owner));
    var gate = new CyclicBarrier(2);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Result> action =
          () -> {
            gate.await(10, TimeUnit.SECONDS);
            return accept(invitation, access(athlete));
          };
      var a = pool.submit(action);
      var b = pool.submit(action);
      assertThat(
              List.of(a.get(20, TimeUnit.SECONDS).status(), b.get(20, TimeUnit.SECONDS).status()))
          .containsExactlyInAnyOrder(204, 409);
    }
    assertThat(
            db.sql(
                    "select count(*) from organization_membership where organization_id=:org and user_id=:user and active")
                .param("org", UUID.fromString(org))
                .param("user", UUID.fromString(athlete.path("user").path("id").asText()))
                .query(Long.class)
                .single())
        .isEqualTo(1);
  }

  @Test
  void concurrentRevocationAndAcceptanceCannotReactivateTheRevokedRole() throws Exception {
    var owner = register(email());
    var coach = register(email());
    var org = create(access(owner));
    var first = invite(org, coach, "INSTRUCTOR", access(owner));
    assertThat(accept(first, access(coach)).status()).isEqualTo(204);
    var member =
        db.sql(
                "select id from organization_membership where organization_id=:org and user_id=:user")
            .param("org", UUID.fromString(org))
            .param("user", UUID.fromString(coach.path("user").path("id").asText()))
            .query(UUID.class)
            .single();
    String path = "/api/v1/organizations/" + org + "/members/" + member;
    assertThat(request("DELETE", path, null, access(owner)).status()).isEqualTo(204);
    var fresh = invite(org, coach, "INSTRUCTOR", access(owner));
    var gate = new CyclicBarrier(2);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var accepting =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return accept(fresh, access(coach));
              });
      var revoking =
          pool.submit(
              () -> {
                gate.await(10, TimeUnit.SECONDS);
                return request("DELETE", path, null, access(owner));
              });
      assertThat(accepting.get(20, TimeUnit.SECONDS).status()).isIn(204, 409);
      assertThat(revoking.get(20, TimeUnit.SECONDS).status()).isEqualTo(204);
    }
    assertThat(
            request(
                    "GET",
                    "/api/v1/organizations/" + org + "/training-reviews",
                    null,
                    access(coach))
                .status())
        .isEqualTo(403);
  }
}
