package com.hajime;

import static org.assertj.core.api.Assertions.*;

import com.hajime.organization.*;
import com.hajime.practitioner.Practitioners;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class IdentityIntegrationTest extends PostgresTest {
  @Autowired OrganizationAccess access;
  @Autowired Practitioners practitioners;

  @Test
  void rolesAreScopedToMembershipAndProfilesToOwner() {
    var dojo = UUID.fromString("30000000-0000-0000-0000-000000000001");
    var athlete = UUID.fromString("20000000-0000-0000-0000-000000000001");
    var coach = UUID.fromString("20000000-0000-0000-0000-000000000002");
    var other = UUID.fromString("30000000-0000-0000-0000-000000000002");
    access.requirePermission(dojo, coach, Permission.REVIEW_TRAINING);
    assertThatThrownBy(() -> access.requirePermission(other, coach, Permission.REVIEW_TRAINING))
        .hasMessageContaining("not permitted");
    assertThatThrownBy(() -> access.requirePermission(dojo, athlete, Permission.REVIEW_TRAINING))
        .hasMessageContaining("not permitted");
    assertThat(practitioners.ownedBy(athlete)).hasSize(1);
    assertThat(practitioners.ownedBy(coach)).isEmpty();
  }
}
