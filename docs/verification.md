# Verification record

Verified locally on 2026-11-01 (America/Santo_Domingo).

## Command location

Run Maven commands from the repository root (`hajime/`), where the parent
`pom.xml`, `mvnw`, and `mvnw.cmd` are located. The backend module is `apps/api`.
To verify the complete project:

```sh
./mvnw verify
```

On Windows (PowerShell), from the same directory:

```powershell
.\mvnw.cmd verify
```

Backend startup commands are in the [README](../README.md#start-the-backend).
The results below record the earlier verification run; this documentation
correction does not rerun Maven or start services.

## Checkpoints

1. Empty monorepo → Maven reactor, Boot application, Flyway baseline, PostgreSQL Compose: build passed.
2. Feature packages → Spring Modulith verification: passed.
3. Judo configuration and policy registry → fresh Testcontainers PostgreSQL migration/catalog test: passed.
4. Dev/test identity, organization memberships, practitioner ownership → permission tests: passed.
5. Training aggregate, JPA adapter, create/history/detail/share → domain and HTTP tests: passed.
6. Pending review, feedback, optional/separate goal, athlete feedback/goals → HTTP slice and rollback tests: passed.
7. Audit and outbox → transaction rollback, retry acknowledgment, and optimistic-locking tests: passed.
8. Java 21, OpenAPI, production-profile authentication, formatting, packaged boot, and Dockerfile: verified below.

## Final checks

- Java runtime: Eclipse Temurin 21.0.13; compilation release 21.
- Maven wrapper: Apache Maven 3.9.12.
- Verification: 14 tests, zero failures/errors/skips; includes Modulith architecture verification and Spotless formatting. The recorded run also cleaned previous build outputs.
- Production-profile tests use a separate PostgreSQL container from dev-profile tests, so dev seed history cannot contaminate production migration validation.
- Standalone Flyway Maven migration against an empty Compose PostgreSQL 17.6 database applied eight versioned migrations and the development repeatable seed successfully.
- Packaged executable JAR booted with dev profile and Hibernate schema validation.
- Health returned UP; `/v3/api-docs` and `/swagger-ui/index.html` returned HTTP 200.
- `python3 scripts/smoke.py` passed against the packaged API: record → history/detail → share → pending review/detail → feedback plus goal → athlete feedback/goals/detail → foreign organization/goal access denied.
- Persisted audit records confirmed TRAINING_SHARED, FEEDBACK_CREATED, GOAL_ASSIGNED, one each for the smoke flow.
- The local outbox processor acknowledged all three corresponding persisted events. This is local delivery, not external push delivery.
- `docker build -f infra/docker/Dockerfile.api -t hajime-api:local .` succeeded on arm64. The resulting image runs Java 21 as a non-root user.

## Deliberate compatibility adjustment

Spring Framework 7 requires JUnit Jupiter 6+. Boot's managed Jupiter 6.0.3 is used instead of the requested JUnit 5. See [ADR-007](adr/ADR-007-compatible-test-platform.md).

## Scope limits

No mobile application, live Cognito tenant/provisioning integration, external notification provider, media adapter, attendance workflow, grading workflow, or competition workflow was built. Their boundaries/documentation are present as requested. Goal lifecycle/evidence and offline sync remain future work. No Redis, Kafka, microservices, or local password authentication is present.


## Domain hardening and clean-code refactor (2026-11-01)

The refactor is verified in an isolated source copy with Temurin 21, PostgreSQL Testcontainers
and `./mvnw -o -B -ntp -Dmaven.repo.local=/private/tmp/hajime-m2 -pl apps/api clean verify`.
The suite now contains 32 tests, including signed JWT authentication/rejection, concurrent
same-UUID creation, conflicting ownership, inactive/revoked authorization, two distinct coaches
racing to review, relational scope constraints and non-HTTP domain invariants. Existing rollback,
architecture, API, OpenAPI, outbox and optimistic-locking tests remain included. Spotless is part
of verify. See the delivery report for the final execution result.

The source changes do not migrate the developer's running database. V8 will be applied by Flyway
on the next configured application startup; existing inconsistent rows cause an explicit migration
failure. Historical migrations are unchanged. JWT verification uses local signing keys in tests;
live Cognito provisioning/pool verification and external notification delivery remain deferred.


## Business errors and leased outbox (2026-11-01)

Verified directly in the repository using Temurin 21 and `./mvnw -o -B -ntp
-Dmaven.repo.local=/private/tmp/hajime-m2 -pl apps/api clean verify`:
**46 tests, zero failures/errors/skips; packaging, Modulith/transport boundaries and Spotless passed**.

New coverage includes preserved business-to-HTTP mappings, competing queue workers, lease expiration,
stale-worker fencing, exhausted/crashed final attempts, explicit requeue, bounded timeout behavior even
when interruption is ignored, destination deduplication across concurrent delivery and delivery/ack
crashes, bounded retention, invalid configuration, delivery without held DB claim locks, and database
acknowledgment failure without false success/failure metrics. Recovery tests own a separate PostgreSQL
container so events from the vertical-slice tests cannot contaminate queue claims.

V9 was applied only in temporary test databases. Existing development data was not migrated. No cloud
service, external notification provider, authentication flow or workflow was added, and no commit was made.
See `outbox-operations.md` for configuration, dead-letter recovery and retention.
