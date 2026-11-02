package com.hajime.shared.web;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfiguration {
  @Bean
  OpenAPI hajimeApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("HAJIME API")
                .version("v1")
                .description(
                    "Judo-first martial arts training journal and organization-scoped coaching."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  @Bean
  @Profile("dev | test")
  OpenApiCustomizer developmentIdentity() {
    return api -> {
      api.getComponents()
          .addSecuritySchemes(
              "devUser",
              new SecurityScheme()
                  .type(SecurityScheme.Type.APIKEY)
                  .in(SecurityScheme.In.HEADER)
                  .name("X-Dev-User")
                  .description("Development only: athlete (default), sensei, outsider."));
      api.setSecurity(java.util.List.of(new SecurityRequirement().addList("devUser")));
    };
  }
}
