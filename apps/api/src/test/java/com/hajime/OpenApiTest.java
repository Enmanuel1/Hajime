package com.hajime;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OpenApiTest extends ApiTest {
  @Test
  void documentationExposesTheVerticalSliceContract() throws Exception {
    var doc = ok("athlete", "GET", "/v3/api-docs", null, 200);
    assertThat(doc.path("info").path("title").asText()).isEqualTo("HAJIME API");
    assertThat(doc.path("paths").has("/api/v1/me/training-sessions")).isTrue();
    assertThat(doc.path("paths").has("/api/v1/organizations/{org}/training-reviews/{id}/feedback"))
        .isTrue();
    assertThat(doc.path("components").path("securitySchemes").has("devUser")).isTrue();
  }
}
