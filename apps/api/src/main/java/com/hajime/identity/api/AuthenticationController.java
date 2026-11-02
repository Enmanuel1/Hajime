package com.hajime.identity.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.hajime.identity.application.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
public class AuthenticationController {
  public record Register(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 15, max = 128) String password,
      @NotBlank @Size(max = 160) String displayName) {
    public Register {
      if (email != null) email = email.trim();
    }

    @JsonAnySetter
    public void unknown(String field, Object value) {
      throw new IllegalArgumentException("Unknown registration field.");
    }

    @Override
    public String toString() {
      return "Register[REDACTED]";
    }
  }

  public record Login(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {
    public Login {
      if (email != null) email = email.trim();
    }

    @JsonAnySetter
    public void unknown(String field, Object value) {
      throw new IllegalArgumentException("Unknown login field.");
    }

    @Override
    public String toString() {
      return "Login[REDACTED]";
    }
  }

  public record Refresh(@NotBlank @Size(max = 512) String refreshToken) {
    @Override
    public String toString() {
      return "Refresh[REDACTED]";
    }
  }

  private final AuthenticationService auth;

  public AuthenticationController(AuthenticationService auth) {
    this.auth = auth;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthenticationService.Tokens> register(@Valid @RequestBody Register body) {
    return tokens(
        auth.register(body.email(), body.password(), body.displayName()), HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthenticationService.Tokens> login(@Valid @RequestBody Login body) {
    return tokens(auth.login(body.email(), body.password()), HttpStatus.OK);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthenticationService.Tokens> refresh(@Valid @RequestBody Refresh body) {
    return tokens(auth.refresh(body.refreshToken()), HttpStatus.OK);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody Refresh body) {
    auth.logout(body.refreshToken());
    return ResponseEntity.noContent()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .build();
  }

  private ResponseEntity<AuthenticationService.Tokens> tokens(
      AuthenticationService.Tokens body, HttpStatus status) {
    return ResponseEntity.status(status)
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .body(body);
  }
}
