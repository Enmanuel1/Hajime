package com.hajime.practitioner.api;

import com.hajime.identity.CurrentUser;
import com.hajime.practitioner.Practitioners;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {
  private final Practitioners practitioners;
  private final CurrentUser user;

  public ProfileController(Practitioners practitioners, CurrentUser user) {
    this.practitioners = practitioners;
    this.user = user;
  }

  @GetMapping("/api/v1/me/practitioner-profiles")
  public List<Practitioners.Profile> profiles() {
    return practitioners.ownedBy(user.id());
  }
}
