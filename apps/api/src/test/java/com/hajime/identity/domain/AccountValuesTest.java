package com.hajime.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.hajime.shared.BusinessException;
import org.junit.jupiter.api.Test;

class AccountValuesTest {
  @Test
  void emailNormalizesAndRejectsWhitespaceOrMalformedValues() {
    assertThat(new EmailAddress(" Athlete@Example.test ").value())
        .isEqualTo("athlete@example.test");
    for (var value :
        new String[] {"bad", "a b@example.test", "a@", "a@example test", "a@@example.test"})
      assertThatThrownBy(() -> new EmailAddress(value)).isInstanceOf(BusinessException.class);
  }

  @Test
  void passwordAcceptsPassphrasesButDoesNotSilentlyTruncate() {
    PasswordPolicy.require("A long phrase with spaces");
    assertThatThrownBy(() -> PasswordPolicy.require("short")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> PasswordPolicy.require("x".repeat(129)))
        .isInstanceOf(BusinessException.class);
  }
}
