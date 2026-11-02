package com.hajime.organization.infrastructure;

import com.hajime.organization.*;
import com.hajime.shared.PageResult;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepository {
  private final JdbcClient db;

  public OrganizationRepository(JdbcClient db) {
    this.db = db;
  }

  public void create(Organization org, UUID art) {
    db.sql("insert into organization(id,name) values(:id,:name)")
        .param("id", org.id())
        .param("name", org.name())
        .update();
    db.sql("insert into organization_martial_art values(:org,:art)")
        .param("org", org.id())
        .param("art", art)
        .update();
  }

  public PageResult<Organization> mine(UUID actor, int page, int size) {
    var rows =
        db.sql(
                "select o.id,o.name from organization o where exists(select 1 from organization_membership m where m.organization_id=o.id and m.user_id=:actor and m.active) order by o.id limit :size offset :offset")
            .param("actor", actor)
            .param("size", size)
            .param("offset", page * size)
            .query(Organization.class)
            .list();
    var total =
        db.sql(
                "select count(*) from organization o where exists(select 1 from organization_membership m where m.organization_id=o.id and m.user_id=:actor and m.active)")
            .param("actor", actor)
            .query(Long.class)
            .single();
    return new PageResult<>(rows, page, size, total);
  }

  public PageResult<OrganizationMember> members(UUID org, int page, int size) {
    var rows =
        db.sql(
                "select m.id,m.user_id,u.display_name,m.role,m.active from organization_membership m join app_user u on u.id=m.user_id where m.organization_id=:org and m.active order by m.id limit :size offset :offset")
            .param("org", org)
            .param("size", size)
            .param("offset", page * size)
            .query(OrganizationMember.class)
            .list();
    var total =
        db.sql("select count(*) from organization_membership where organization_id=:org and active")
            .param("org", org)
            .query(Long.class)
            .single();
    return new PageResult<>(rows, page, size, total);
  }

  public boolean userExists(UUID user) {
    return db.sql("select exists(select 1 from app_user where id=:id)")
        .param("id", user)
        .query(Boolean.class)
        .single();
  }

  public boolean activeRole(UUID org, UUID user, MembershipRole role) {
    return db.sql(
            "select exists(select 1 from organization_membership where organization_id=:org and user_id=:user and role=:role and active)")
        .param("org", org)
        .param("user", user)
        .param("role", role.name())
        .query(Boolean.class)
        .single();
  }

  public void cancelPending(UUID org, UUID recipient) {
    db.sql(
            "update organization_invitation set status='REVOKED' where organization_id=:org and recipient_user_id=:recipient and status='PENDING'")
        .param("org", org)
        .param("recipient", recipient)
        .update();
  }

  public void invite(OrganizationInvitation invitation, UUID actor, Instant created) {
    db.sql(
            "insert into organization_invitation(id,organization_id,recipient_user_id,role,created_by,created_at,expires_at,status) values(:id,:org,:recipient,:role,:actor,:created,:expires,'PENDING')")
        .param("id", invitation.id())
        .param("org", invitation.organizationId())
        .param("recipient", invitation.recipientUserId())
        .param("role", invitation.role().name())
        .param("actor", actor)
        .param("created", java.sql.Timestamp.from(created))
        .param("expires", java.sql.Timestamp.from(invitation.expiresAt()))
        .update();
  }

  public PageResult<OrganizationInvitation> invitations(
      UUID actor, Instant now, int page, int size) {
    var rows =
        db.sql(
                "select id,organization_id,recipient_user_id,role,expires_at,status from organization_invitation where recipient_user_id=:actor and status='PENDING' and expires_at>:now order by id limit :size offset :offset")
            .param("actor", actor)
            .param("now", java.sql.Timestamp.from(now))
            .param("size", size)
            .param("offset", page * size)
            .query(OrganizationInvitation.class)
            .list();
    var total =
        db.sql(
                "select count(*) from organization_invitation where recipient_user_id=:actor and status='PENDING' and expires_at>:now")
            .param("actor", actor)
            .param("now", java.sql.Timestamp.from(now))
            .query(Long.class)
            .single();
    return new PageResult<>(rows, page, size, total);
  }

  public Optional<OrganizationInvitation> invitation(UUID id, UUID actor) {
    return db.sql(
            "select id,organization_id,recipient_user_id,role,expires_at,status from organization_invitation where id=:id and recipient_user_id=:actor")
        .param("id", id)
        .param("actor", actor)
        .query(OrganizationInvitation.class)
        .optional();
  }

  public void accepted(UUID id) {
    db.sql("update organization_invitation set status='ACCEPTED' where id=:id")
        .param("id", id)
        .update();
  }

  public Optional<OrganizationMember> member(UUID org, UUID id) {
    return db.sql(
            "select m.id,m.user_id,u.display_name,m.role,m.active from organization_membership m join app_user u on u.id=m.user_id where m.organization_id=:org and m.id=:id")
        .param("org", org)
        .param("id", id)
        .query(OrganizationMember.class)
        .optional();
  }

  public void revoke(UUID member) {
    db.sql("update organization_membership set active=false where id=:id")
        .param("id", member)
        .update();
  }

  public boolean lock(UUID org) {
    return db.sql("select id from organization where id=:id for update")
        .param("id", org)
        .query(UUID.class)
        .optional()
        .isPresent();
  }

  public void add(UUID org, UUID recipient, MembershipRole role) {
    db.sql(
            "insert into organization_membership(id,organization_id,user_id,role,active) values(:id,:org,:user,:role,true) on conflict(organization_id,user_id,role) do update set active=true")
        .param("id", UUID.randomUUID())
        .param("org", org)
        .param("user", recipient)
        .param("role", role.name())
        .update();
  }
}
