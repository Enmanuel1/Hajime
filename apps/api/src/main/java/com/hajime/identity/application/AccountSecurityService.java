package com.hajime.identity.application;

import com.hajime.identity.*;
import com.hajime.identity.domain.*;
import com.hajime.identity.infrastructure.*;
import com.hajime.shared.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AccountSecurityService {
  public record RecoveryCode(String recoveryCode) {
    @Override
    public String toString() {
      return "RecoveryCode[REDACTED]";
    }
  }

  private final AuthRepository accounts;
  private final CurrentUser user;
  private final PasswordEncoder passwords;
  private final AuthRateLimiter limits;
  private final Clock clock;
  private final ApplicationEventPublisher events;
  private final TransactionTemplate tx;

  public AccountSecurityService(
      AuthRepository accounts,
      CurrentUser user,
      PasswordEncoder passwords,
      AuthRateLimiter limits,
      Clock clock,
      ApplicationEventPublisher events,
      PlatformTransactionManager manager) {
    this.accounts = accounts;
    this.user = user;
    this.passwords = passwords;
    this.limits = limits;
    this.clock = clock;
    this.events = events;
    tx = new TransactionTemplate(manager);
  }

  public void changePassword(String currentPassword, String newPassword) {
    var before = provePassword(currentPassword);
    PasswordPolicy.require(newPassword);
    DomainValidation.require(
        !newPassword.equals(currentPassword),
        "New password must differ from the current password.");
    var hash = passwords.encode(newPassword);
    tx.executeWithoutResult(
        status -> {
          recheck(before);
          accounts.changePassword(before.id(), hash);
          accounts.revokeAll(before.id(), clock.instant());
          changed(before.id(), "PASSWORD_CHANGED");
        });
  }

  public RecoveryCode generateRecoveryCode(String currentPassword) {
    var before = provePassword(currentPassword);
    var raw = RefreshTokens.generate();
    tx.executeWithoutResult(
        status -> {
          recheck(before);
          accounts.recoveryCode(before.id(), RefreshTokens.hash(raw));
          changed(before.id(), "RECOVERY_CODE_ROTATED");
        });
    return new RecoveryCode(raw);
  }

  public void recover(String rawEmail, String code, String newPassword) {
    var email = new EmailAddress(rawEmail).value();
    limits.account("recovery:" + email);
    PasswordPolicy.require(newPassword);
    if (code == null || !code.matches("[A-Za-z0-9_-]{43}")) throw invalidRecovery();
    var before =
        accounts
            .account(email)
            .filter(AuthRepository.Account::active)
            .orElseThrow(this::invalidRecovery);
    // Do the expensive password hash only after proving the code inside the credential lock.
    tx.executeWithoutResult(
        status -> {
          var current = accounts.lockAccount(before.id());
          if (!current.active()
              || !accounts.consumeRecoveryCode(current.id(), RefreshTokens.hash(code)))
            throw invalidRecovery();
          accounts.changePassword(current.id(), passwords.encode(newPassword));
          accounts.revokeAll(current.id(), clock.instant());
          changed(current.id(), "ACCOUNT_RECOVERED");
        });
  }

  private AuthRepository.Account provePassword(String password) {
    var actor = user.id();
    limits.account("security:" + actor);
    DomainValidation.require(
        password != null && !password.isEmpty() && password.length() <= 128,
        "Invalid password input.");
    var account =
        accounts
            .account(actor)
            .filter(AuthRepository.Account::active)
            .orElseThrow(this::invalidPassword);
    if (!passwords.matches(password, account.passwordHash())) throw invalidPassword();
    return account;
  }

  private void recheck(AuthRepository.Account before) {
    var current = accounts.lockAccount(before.id());
    if (!current.active() || !current.passwordHash().equals(before.passwordHash()))
      throw invalidPassword();
  }

  private BusinessException invalidPassword() {
    return BusinessException.unauthenticated("INVALID_CREDENTIALS", "Invalid current password.");
  }

  private BusinessException invalidRecovery() {
    return BusinessException.unauthenticated("INVALID_RECOVERY_CODE", "Invalid recovery details.");
  }

  private void changed(UUID actor, String action) {
    events.publishEvent(new AccountSecurityChanged(actor, action));
  }
}
