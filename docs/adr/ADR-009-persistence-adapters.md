# ADR-009: JPA aggregates and explicit SQL adapters

Status: Accepted

## Context

Training has aggregate state transitions and child value collections, while catalog/membership reads and coaching records have straightforward relational queries. Mapping every table twice adds ceremony without improving domain clarity.

## Decision

Use a separate rich training aggregate and JPA persistence model behind TrainingRepository. Use JdbcClient adapters behind module/application contracts for catalog, identities, organizations, practitioner reads, and coaching. Coaching records remain immutable domain values. Application services, not repositories/controllers, define business transactions. SQL review updates include organization and expected-version predicates.

## Consequences

Spring's JPA transaction manager shares the datasource connection with JDBC, so mixed-module actions remain atomic; integration tests verify rollback across both adapters. JDBC adapters explicitly bind Instant values as SQL timestamps. Schema constraints cover all tables; Hibernate validates mapped training tables, while Flyway and PostgreSQL integration tests validate the complete schema.
