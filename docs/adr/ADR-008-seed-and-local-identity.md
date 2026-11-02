# ADR-008: Separate catalog configuration from personal demo data

Status: Accepted

## Context

Judo catalog data is useful in every environment; demo people and an impersonation adapter are development-only. Schema phases also need to migrate independently without enabling out-of-order migration.

## Decision

V1 is identity baseline, V2 catalog schema, V2.1 production-safe prototype Judo configuration, V3 organizations, V4 practitioners, V5 training, V6 coaching, V7 audit/outbox. Curriculum schema is in V2 with the organization foreign key added in V3. Development-only R__demo_identity holds idempotent demo people, organizations, and profiles in `db/dev`; production Flyway scans only `db/migration`. This grouping intentionally differs from the suggested V8 development seed.

The local authentication adapter exists only in a dev/test SecurityFilterChain. Default/production profiles remain stateless and fail closed until an issuer/decoder is configured. Known aliases are fixed rather than accepting arbitrary user IDs. No passwords are stored.

## Consequences

Production does not receive demo identities. Dev seed migration history must never be copied to production. The repeatable seed runs after versioned schema migrations and uses ON CONFLICT DO NOTHING to tolerate existing fixture IDs. Future schema versions remain sequential without a high dev-only version in their way. Never turn on out-of-order migration as a general workaround.
