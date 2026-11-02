# Backend MVP readiness — 2026-11-02

## Verdict

The core backend flow is functionally ready for a closed pilot: real accounts, profiles, dojo enrollment,
training correction/sharing, coaching, goal closure and password recovery without cloud services.
Automated tests and live checks pass. This is not a public deployment sign-off: the chosen host, real HTTPS/proxy
configuration, secret storage, backup scheduling/off-machine retention and frontend integration still need validation. Email ownership verification is not implemented.

## KEEP

- Modular monolith with Spring Modulith boundaries: fewer deployment pieces, module exports verified by tests. Organization depends on identity/catalog contracts; audit consumes organization events without a reverse dependency.
- Parent `pom.xml` and Maven wrapper at repository root, API module in `apps/api`: appropriate Maven reactor layout; moving them adds no product value.
- PostgreSQL/Flyway as the single persistence platform. Existing migrations remain unchanged; V11 adds invitations.
- Immutable training aggregate and explicit state transitions, separate read views, optimistic versions and creation retry behavior. Domain validation also applies outside HTTP.
- Local authentication with password hashing, opaque rotated refresh tokens and session revocation. JWT requests check active sessions in PostgreSQL: immediate revocation on subsequent checks intentionally needs database state.
- Transport-independent BusinessException and consistent problem responses. SQL in persistence adapters, HTTP mapping in controllers.
- Leased, bounded outbox with destination deduplication and explicit dead-letter recovery. Local receipts are technical delivery evidence, not a user inbox or external notification provider.
- Composition, small records and explicit invariants. Add polymorphism when a second behavior actually needs it; inheritance and generic base services would not improve these slices.

## MVP work completed

1. **Dojo onboarding.** Organization creation, scoped director role, paginated own organizations/members/invitations, recipient acceptance, seven-day expiry and nonprotected role deactivation. Transactional audit and concurrency tests included.
2. **Correct training before sharing.** Owner-only full-content update while RECORDED with required expected version. Preserves ID, author, source and creation time. Optional dojo selection is revalidated. Shared/reviewed history cannot be edited. Simultaneous edit/share admits only one version's write.
3. **Finish goals.** Coaches with ASSIGN_GOAL may complete/cancel ACTIVE goals using expected version and organization scope. Athletes read final states. Transitions are audited and competing completion/cancellation commits only once.
4. **Recover access.** Current-password-proven change revokes every session and recovery code. Offline recovery uses a user-saved, rotatable single-use random code stored only as a hash. No email or public administrator bypass. Rate limits and credential/session lock ordering are tested.
5. **Local release checks.** Production-profile packaged startup, stable JWT across restart, explicit-origin browser access and complete database backup/restore are verified. The script refuses restoration over an existing populated target. Public network and hosting checks remain deployment-dependent.

## Next acceptance gate

Connect the real frontend and exercise the account → save recovery code → dojo enrollment → training edit/share →
coach feedback → goal closure flow. Choose the host/domain, configure HTTPS and trusted ingress, retain the signing key,
schedule private off-machine backups and repeat recovery there. Keep rollout closed until those checks pass; decide on
email verification before relying on verified email ownership for account discovery or public onboarding.

## REFACTOR LATER

- Safe recipient discovery and an invitation UI: current pilot exchanges existing account UUIDs manually. An unverified email address is not account ownership proof.
- Dojo editing, more supported disciplines per dojo, ownership transfer and consolidated role management. Current memberships are additive; removing one role leaves other active roles effective.
- Pending invitation history retention, indexes/quotas beyond current bounds and load testing when volume/latency targets exist.
- User-facing notification inbox or real delivery adapter; current polling APIs can serve the pilot without paid push/email.
- Offline synchronization, attendance, rank evaluation, competition and media. They are separate features, not prerequisites for the agreed training/coaching pilot.

## DELETE

No additional deletion is justified by this review. Cognito-specific decoder/policy were already replaced by local authentication. Keep historical ADRs with supersession links rather than erasing decisions. Do not remove domain boundaries or persistence adapters to reduce the file count.

## New onboarding limitations

Invitations target an existing account UUID and grant only PRACTITIONER, INSTRUCTOR or SENSEI after acceptance. A new invitation replaces pending invitations, not already active roles. Revoking a membership deactivates that role and cancels remaining pending invitations for that recipient in the dojo. Already authorized requests are not retroactively cancelled. New organizations initially support one art. No email, external notification or public account directory was added.

## Verification

An isolated source copy prevents editor-generated classes from interfering with Maven. Temurin 21 and
PostgreSQL Testcontainers: **76 tests, zero failures/errors/skips**, packaging, Spotless, Modulith and
transport-boundary checks passed. The suite includes version races, tenant isolation, consumed/replaced
recovery codes, session revocation across devices, concurrent refresh/password change, rate limiting and CORS.

Live Postman CLI against the packaged prod-profile API and disposable PostgreSQL: **42 requests / 49 assertions,
zero failures, exit 0**. No development aliases or mocks. The expanded collection is
`postman/collections/HAJIME-mvp.postman_collection.json`.

Operational verification: API restart with a stable key retained JWT authentication; a private dump restored
all **27 tables with identical row counts and content fingerprints** into another empty database. The API
started on the restored schema V12 and accepted an existing session. Restoring over a populated target was
rejected. Temporary API/database and the test dump were removed. No public deploy, developer database
migration or commit was performed.
