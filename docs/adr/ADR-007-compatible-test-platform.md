# ADR-007: Use the Boot-managed JUnit Jupiter platform

Status: Accepted

## Context

The request specifies both Spring Boot 4.1.x and JUnit 5. Boot 4.1.2 uses Spring Framework 7 and manages JUnit Jupiter 6.0.3. [SpringExtension requires Jupiter 6 or higher](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/support-classes.html).

## Decision

Retain Boot's managed JUnit Jupiter 6 dependency instead of forcing an incompatible JUnit 5 override. Keep the familiar `org.junit.jupiter.api` API, Testcontainers, Mockito, and AssertJ. Spring Modulith 2.1.2 follows Boot 4.1; [springdoc's matrix](https://springdoc.org/faq.html) supports springdoc 3.1.x for Boot 4.1.

## Consequences

The test framework major version differs from the request for a documented compatibility reason. Managed platform alignment preserves working Spring integration tests and avoids unnecessary dependency overrides. Production Java compilation targets 21.
