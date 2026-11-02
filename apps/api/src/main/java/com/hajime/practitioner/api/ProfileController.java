package com.hajime.practitioner.api;

import com.hajime.identity.CurrentUser;
import com.hajime.practitioner.Practitioners;
import com.hajime.practitioner.application.CreatePractitionerProfile;
import com.hajime.practitioner.application.ProfileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {
  private final Practitioners practitioners;
  private final CurrentUser user;
  private final ProfileService service;

  public ProfileController(Practitioners practitioners, CurrentUser user, ProfileService service) {
    this.practitioners = practitioners;
    this.user = user;
    this.service = service;
  }

  @GetMapping("/api/v1/me/practitioner-profiles")
  public List<Practitioners.Profile> profiles() {
    return practitioners.ownedBy(user.id());
  }

  @PostMapping("/api/v1/me/practitioner-profiles")
  public ResponseEntity<Practitioners.Profile> create(
      @Valid @RequestBody CreatePractitionerProfile body) {
    var result = service.create(body);
    return ResponseEntity.created(URI.create("/api/v1/me/practitioner-profiles/" + result.id()))
        .body(result);
  }

  @GetMapping("/api/v1/me/practitioner-profiles/{id}")
  public Practitioners.Profile detail(@PathVariable UUID id) {
    return service.detail(id);
  }
}
