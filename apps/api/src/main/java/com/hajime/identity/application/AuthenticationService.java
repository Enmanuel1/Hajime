package com.hajime.identity.application;

import com.hajime.identity.UserDirectory.User;
import com.hajime.identity.domain.*;
import com.hajime.identity.infrastructure.*;
import com.hajime.shared.*;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthenticationService {
  public record Tokens(
      String accessToken, String refreshToken, String tokenType, long expiresIn, User user) {
    @Override
    public String toString() {
      return "Tokens[REDACTED]";
    }
  }

  private final AuthRepository accounts;
  private final PasswordEncoder passwords;
  private final JwtTokens jwt;
  private final AuthSettings settings;
  private final AuthRateLimiter limits;
  private final Clock clock;
  private final TransactionTemplate tx;
  private final String dummyPasswordHash;

  public AuthenticationService(
      AuthRepository accounts,
      PasswordEncoder passwords,
      JwtTokens jwt,
      AuthSettings settings,
      AuthRateLimiter limits,
      Clock clock,
      PlatformTransactionManager manager) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.jwt = jwt;
    this.settings = settings;
    this.limits = limits;
    this.clock = clock;
    this.tx = new TransactionTemplate(manager);
    this.dummyPasswordHash = passwords.encode(RefreshTokens.generate());
  }

  public Tokens register(String rawEmail, String password, String rawName) {
    var email = new EmailAddress(rawEmail).value();
    PasswordPolicy.require(password);
    var name = DomainValidation.text(rawName, 160, "Display name");
    var hash = passwords.encode(password);
    return tx.execute(
        status -> {
          var id = UUID.randomUUID();
          if (!accounts.createAccount(id, email, hash, name))
            throw BusinessException.conflict(
                "ACCOUNT_UNAVAILABLE", "An account cannot be created with these details.");
          return start(new AuthRepository.Account(id, email, hash, true, name));
        });
  }

  public Tokens login(String rawEmail, String password) {
    var email = new EmailAddress(rawEmail).value();
    limits.account(email);
    DomainValidation.require(
        password != null && !password.isEmpty() && password.length() <= 128,
        "Invalid password input.");
    var account = accounts.account(email);
    boolean valid =
        passwords.matches(
            password, account.map(AuthRepository.Account::passwordHash).orElse(dummyPasswordHash));
    if (!valid || account.isEmpty() || !account.get().active()) throw invalidCredentials();
    var before = account.get();
    return tx.execute(
        status -> {
          var current = accounts.lockAccount(before.id());
          if (!current.active() || !current.passwordHash().equals(before.passwordHash()))
            throw invalidCredentials();
          return start(current);
        });
  }

  private Tokens start(AuthRepository.Account account) {
    var now = clock.instant();
    var session = accounts.createSession(account.id(), now, now.plus(settings.sessionTtl()));
    return issue(account, session);
  }

  private Tokens issue(AuthRepository.Account account, AuthRepository.Session session) {
    var refresh = RefreshTokens.generate();
    var now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
    var expires = now.plus(settings.accessTtl());
    if (session.expiresAt().isBefore(expires))
      expires = session.expiresAt().truncatedTo(ChronoUnit.SECONDS);
    if (!expires.isAfter(now))
      throw BusinessException.unauthenticated("INVALID_REFRESH_TOKEN", "Session expired.");
    accounts.addRefresh(RefreshTokens.hash(refresh), session.id(), now);
    return new Tokens(
        jwt.issue(account.id(), session.id(), now, expires),
        refresh,
        "Bearer",
        Duration.between(now, expires).toSeconds(),
        new User(account.id(), account.displayName()));
  }

  public Tokens refresh(String refresh) {
    var hash = tokenHash(refresh);
    // Replay revocation must commit. Throw the public rejection AFTER this transaction returns.
    var result =
        tx.execute(
            status -> {
              var token = accounts.refresh(hash);
              if (token.isEmpty()) return null;
              var initial = accounts.session(token.get().sessionId()).orElse(null);
              if (initial == null) return null;
              // Credential first, then session: same order as password change/recovery.
              var account = accounts.lockAccount(initial.userId());
              var session = accounts.lockSession(initial.id()).orElse(null);
              if (session == null) return null;
              var current = accounts.refresh(hash).orElseThrow();
              var now = clock.instant();
              if (!session.activeAt(now)) return null;
              if (current.consumedAt() != null) {
                accounts.revoke(session.id(), now);
                return null;
              }
              if (!account.active()) {
                accounts.revoke(session.id(), now);
                return null;
              }
              accounts.consume(hash, now);
              return issue(account, session);
            });
    if (result == null)
      throw BusinessException.unauthenticated(
          "INVALID_REFRESH_TOKEN", "Invalid or expired refresh token.");
    return result;
  }

  public void logout(String refresh) {
    var hash = tokenHash(refresh);
    tx.executeWithoutResult(
        status ->
            accounts
                .refresh(hash)
                .ifPresent(
                    token -> {
                      accounts.lockSession(token.sessionId());
                      accounts.revoke(token.sessionId(), clock.instant());
                    }));
  }

  private String tokenHash(String token) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
      throw BusinessException.unauthenticated(
          "INVALID_REFRESH_TOKEN", "Invalid or expired refresh token.");
    return RefreshTokens.hash(token);
  }

  private BusinessException invalidCredentials() {
    return BusinessException.unauthenticated("INVALID_CREDENTIALS", "Invalid email or password.");
  }
}
