package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * No dev/test Spring profile. The test data may coexist, but its impersonation adapter must not.
 */
class ProductionSecurityTest extends ProductionPostgresTest {
  @LocalServerPort int port;

  @Test
  void developmentHeaderCannotAuthenticateOutsideDevelopmentProfiles() throws Exception {
    var client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/me"))
            .header("X-Dev-User", "sensei")
            .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.body()).contains("UNAUTHENTICATED");
    // Catalog reads do not resolve CurrentUser, so this proves the dev filter itself is absent.
    var catalogRequest =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/martial-arts"))
            .header("X-Dev-User", "sensei")
            .build();
    assertThat(client.send(catalogRequest, HttpResponse.BodyHandlers.ofString()).statusCode())
        .isEqualTo(401);

    assertThat(
            client
                .send(
                    HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + "/actuator/health"))
                        .build(),
                    HttpResponse.BodyHandlers.ofString())
                .statusCode())
        .isEqualTo(200);
  }
}
