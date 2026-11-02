package com.hajime.shared.web;

import static org.assertj.core.api.Assertions.*;

import com.hajime.shared.BusinessException;
import java.util.*;
import org.junit.jupiter.api.Test;

class ApiErrorsTest {
  @Test
  void businessFailuresKeepTheirHttpContractOnlyAtTheWebBoundary() {
    var cases =
        Map.of(
            BusinessException.invalid("INVALID_RECORD", "Invalid record"), 400,
            BusinessException.unauthenticated("UNAUTHENTICATED", "Authentication required"), 401,
            BusinessException.denied(), 403,
            BusinessException.missing("GOAL_NOT_FOUND"), 404,
            BusinessException.conflict("CONCURRENT_MODIFICATION", "Reload"), 409,
            BusinessException.unsupported("MARTIAL_ART_NOT_SUPPORTED", "Unsupported art"), 422);
    cases.forEach(
        (error, status) -> {
          var problem = new ApiErrors().domain(error);
          assertThat(problem.getStatus()).isEqualTo(status);
          assertThat(problem.getProperties()).containsEntry("code", error.code());
          assertThat(problem.getDetail()).isEqualTo(error.getMessage());
        });
  }
}
