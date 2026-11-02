package com.hajime;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "hajime.web.cors-allowed-origins=https://app.example.test")
class ApiCorsTest extends ProductionApiSupport {
  @Test
  void allowedBrowserOriginGetsPreflightButForeignOriginDoesNot() throws Exception {
    for (var origin : new String[] {"https://app.example.test", "https://foreign.example.test"}) {
      var response =
          client.send(
              HttpRequest.newBuilder(
                      URI.create("http://127.0.0.1:" + port + "/api/v1/me/training-sessions"))
                  .header("Origin", origin)
                  .header("Access-Control-Request-Method", "PUT")
                  .header("Access-Control-Request-Headers", "authorization,content-type")
                  .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      if (origin.equals("https://app.example.test")) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(origin);
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
      } else {
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
      }
    }
    assertThat(request("GET", "/api/v1/me", null, null).status()).isEqualTo(401);
  }
}
