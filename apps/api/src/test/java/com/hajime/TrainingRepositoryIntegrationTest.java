package com.hajime;

import static org.assertj.core.api.Assertions.*;

import com.hajime.shared.RecordSource;
import com.hajime.training.*;
import com.hajime.training.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
class TrainingRepositoryIntegrationTest extends PostgresTest {
  @Autowired TrainingRepository repository;
  @Autowired PlatformTransactionManager manager;

  @Test
  void staleAggregateCannotOverwriteCommittedTransition() {
    var tx = new TransactionTemplate(manager);
    var now = Instant.now();
    var data =
        new TrainingView(
            UUID.randomUUID(),
            ApiTest.PROFILE,
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            ApiTest.DOJO,
            now,
            60,
            "TECHNICAL",
            5,
            4,
            "Gripping",
            RecordSource.PERSONAL,
            "RECORDED",
            now,
            now,
            null,
            List.of(),
            List.of());
    var original = tx.execute(s -> repository.save(com.hajime.TrainingFixtures.restore(data)));
    tx.executeWithoutResult(s -> repository.save(original.share(Instant.now())));
    assertThatThrownBy(
            () -> tx.executeWithoutResult(s -> repository.save(original.share(Instant.now()))))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }
}
