# Backend MVP readiness — 2026-11-02

## Verdict

The backend now supports real-account enrollment into a dojo and the training/coaching path without development memberships. This is a useful base for a closed pilot. It is not yet a complete public MVP: journal corrections, goal completion, account recovery and deployment/restore operations still need explicit acceptance checks.

## KEEP

- Modular monolith with Spring Modulith boundaries: fewer deployment pieces, module exports verified by tests. Organization depends on identity/catalog contracts; audit consumes organization events without a reverse dependency.
- Parent `pom.xml` and Maven wrapper at repository root, API module in `apps/api`: appropriate Maven reactor layout; moving them adds no product value.
- PostgreSQL/Flyway as the single persistence platform. Existing migrations remain unchanged; V11 adds invitations.
- Immutable training aggregate and explicit state transitions, separate read views, optimistic versions and creation retry behavior. Domain validation also applies outside HTTP.
- Local authentication with password hashing, opaque rotated refresh tokens and session revocation. JWT requests check active sessions in PostgreSQL: immediate revocation on subsequent checks intentionally needs database state.
- Transport-independent BusinessException and consistent problem responses. SQL in persistence adapters, HTTP mapping in controllers.
- Leased, bounded outbox with destination deduplication and explicit dead-letter recovery. Local receipts are technical delivery evidence, not a user inbox or external notification provider.
- Composition, small records and explicit invariants. Add polymorphism when a second behavior actually needs it; inheritance and generic base services would not improve these slices.

## REFACTOR NOW / CLOSE FOR MVP

1. **Dojo membership onboarding — implemented in this change.** Organization creation, scoped director role, paginated own organizations/members/invitations, recipient acceptance, seven-day expiry and role deactivation. Protected director/admin roles cannot be granted or removed through invitation/membership endpoints. Transactional audit and concurrent acceptance/revocation tests included.
2. **Correct training before sharing — next product slice.** `training/domain/TrainingSession.java` has creation/share/review but no content-edit transition. Add owner-only update while RECORDED with expected version; preserve ID, author, provenance and creation time. Reject edits after sharing and stale saves. Cover simultaneous edit/share and scoped ownership. This prevents users recreating an incorrect record.
3. **Finish goals.** `coaching/domain/Goal.java` defines ACHIEVED/CANCELLED, but the API only creates/reads goals. Define who may complete/cancel, require expected version, audit transitions and expose the resulting state to the athlete. Avoid artificial progress percentages.
4. **Recover access.** Add password change and session revocation; then a real password recovery channel. Email verification/recovery are not implemented. A public reset endpoint without proof of ownership is not acceptable; do not substitute a development token printed by production logs.
5. **Release operations.** Validate stable JWT secret across restart, HTTPS/proxy behavior, chosen client origins, backup plus actual restore, and a fresh production-profile startup. Compose and Dockerfile are useful local packaging, not evidence that those operational checks passed. No new AWS service or workflow is required for these checks.

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

A clean source copy avoids the editor rewriting compiled classes during Maven builds. Temurin 21, disposable PostgreSQL and Maven `clean verify` passed: 66 tests, zero failures/errors/skips, packaging, Spotless and module/HTTP boundary checks. Six new production-authenticated integration tests cover real enrollment through coaching, tenant/privilege isolation, expiry/replacement, revocation, concurrent acceptance and concurrent acceptance/deactivation. Historical migrations and developer database were not modified.

See `organization-onboarding.md` and the runnable `postman/collections/HAJIME-organizations.postman_collection.json` for the new contract. A live Postman CLI run against the packaged backend without dev/test profiles and a fresh disposable PostgreSQL passed: 24 requests, 27 assertions, exit 0. Temporary API/database were stopped. Results were kept local and not published to Postman cloud.
