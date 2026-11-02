package com.hajime.organization.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.hajime.organization.*;
import com.hajime.organization.application.OrganizationService;
import com.hajime.shared.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OrganizationController {
  public record Create(@NotBlank @Size(max = 160) String name, @NotBlank String martialArtCode) {
    @JsonAnySetter
    public void unknown(String field, Object value) {
      throw new IllegalArgumentException("Unknown organization field.");
    }
  }

  public record Invite(@NotNull UUID recipientUserId, @NotNull MembershipRole role) {
    @JsonAnySetter
    public void unknown(String field, Object value) {
      throw new IllegalArgumentException("Unknown invitation field.");
    }
  }

  private final OrganizationService service;

  public OrganizationController(OrganizationService service) {
    this.service = service;
  }

  @PostMapping("/organizations")
  public ResponseEntity<Organization> create(@Valid @RequestBody Create request) {
    return ResponseEntity.status(201)
        .body(service.create(request.name(), request.martialArtCode()));
  }

  @GetMapping("/me/organizations")
  public PageResult<Organization> mine(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.mine(page, size);
  }

  @GetMapping("/organizations/{org}/members")
  public PageResult<OrganizationMember> members(
      @PathVariable UUID org,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.members(org, page, size);
  }

  @PostMapping("/organizations/{org}/invitations")
  public ResponseEntity<OrganizationInvitation> invite(
      @PathVariable UUID org, @Valid @RequestBody Invite request) {
    return ResponseEntity.status(201)
        .body(service.invite(org, request.recipientUserId(), request.role()));
  }

  @GetMapping("/me/organization-invitations")
  public PageResult<OrganizationInvitation> invitations(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.invitations(page, size);
  }

  @PostMapping("/me/organization-invitations/{id}/accept")
  public ResponseEntity<Void> accept(@PathVariable UUID id) {
    service.accept(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/organizations/{org}/members/{id}")
  public ResponseEntity<Void> revoke(@PathVariable UUID org, @PathVariable UUID id) {
    service.revoke(org, id);
    return ResponseEntity.noContent().build();
  }
}
