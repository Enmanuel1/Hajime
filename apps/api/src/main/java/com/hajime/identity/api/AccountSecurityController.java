package com.hajime.identity.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.hajime.identity.application.AccountSecurityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AccountSecurityController {
  public record ChangePassword(
      @NotBlank @Size(max = 128) String currentPassword,
      @NotBlank @Size(min = 15, max = 128) String newPassword) {
    @Override
    public String toString() {
      return "ChangePassword[REDACTED]";
    }

    @JsonAnySetter
    public void unknown(String field, Object value) {
      throw new IllegalArgumentException("Unknown account security field.");
    }
  }

  public record PasswordProof(@NotBlank @Size(max = 128) String currentPassword) {
    @Override
    public String toString() {
      return "PasswordProof[REDACTED]";
    }

    @JsonAnySetter
    public void unknown(String field, Object value) {
      throw new IllegalArgumentException("Unknown account security field.");
    }
  }

  public record Recover(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 128) String recoveryCode,
      @NotBlank @Size(min = 15, max = 128) String newPassword) {
    public Recover {
      if (email != null) email = email.trim();
    }

    @Override
    public String toString() {
      return "Recover[REDACTED]";
    }

    @JsonAnySetter
    public void unknown(String field, Object value) {
      throw new IllegalArgumentException("Unknown account recovery field.");
    }
  }

  private final AccountSecurityService service;

  public AccountSecurityController(AccountSecurityService service) {
    this.service = service;
  }

  @PostMapping("/me/password")
  public ResponseEntity<Void> change(@Valid @RequestBody ChangePassword body) {
    service.changePassword(body.currentPassword(), body.newPassword());
    return empty();
  }

  @PostMapping("/me/recovery-code")
  public ResponseEntity<AccountSecurityService.RecoveryCode> code(
      @Valid @RequestBody PasswordProof body) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .body(service.generateRecoveryCode(body.currentPassword()));
  }

  @SecurityRequirements
  @PostMapping("/auth/recover")
  public ResponseEntity<Void> recover(@Valid @RequestBody Recover body) {
    service.recover(body.email(), body.recoveryCode(), body.newPassword());
    return empty();
  }

  private ResponseEntity<Void> empty() {
    return ResponseEntity.noContent()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .build();
  }
}
