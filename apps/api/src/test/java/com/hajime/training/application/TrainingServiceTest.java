package com.hajime.training.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hajime.identity.CurrentUser;
import com.hajime.martialart.*;
import com.hajime.organization.OrganizationAccess;
import com.hajime.practitioner.Practitioners;
import com.hajime.shared.*;
import com.hajime.training.*;
import com.hajime.training.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class TrainingServiceTest {
  @Test
  void staleShareDoesNotWriteOrPublishEvents() {
    var repository = mock(TrainingRepository.class);
    var user = mock(CurrentUser.class);
    var practitioners = mock(Practitioners.class);
    var events = mock(ApplicationEventPublisher.class);
    var service =
        new TrainingService(
            repository,
            user,
            practitioners,
            mock(OrganizationAccess.class),
            mock(Catalog.class),
            mock(MartialArtPolicyRegistry.class),
            events,
            java.time.Clock.systemUTC());
    var id = UUID.randomUUID();
    var owner = UUID.randomUUID();
    var profile = UUID.randomUUID();
    var art = UUID.randomUUID();
    var now = Instant.now();
    when(user.id()).thenReturn(owner);
    when(practitioners.ownedBy(owner))
        .thenReturn(List.of(new Practitioners.Profile(profile, owner, art, null, null, "ACTIVE")));
    when(repository.owned(id, List.of(profile)))
        .thenReturn(
            Optional.of(
                com.hajime.TrainingFixtures.restore(
                    new TrainingView(
                        id,
                        profile,
                        art,
                        UUID.randomUUID(),
                        now,
                        60,
                        "TECHNICAL",
                        null,
                        null,
                        null,
                        RecordSource.PERSONAL,
                        "RECORDED",
                        now,
                        now,
                        2L,
                        List.of(),
                        List.of()))));
    assertThatThrownBy(() -> service.share(id, 1))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Reload");
    verify(repository, never()).save(any());
    verifyNoInteractions(events);
  }
}
