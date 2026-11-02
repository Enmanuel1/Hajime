# ADR-012: Local accounts, JWT sessions and own profiles

Status: Accepted

## Context

The pilot needs real users without Cognito/AWS or paid infrastructure. Development aliases cannot be the
production identity model. Existing organization permissions and journal APIs must remain intact.

## Decision

Keep app_user subject resolution and add optional local credentials with unique normalized email and
versioned salted PBKDF2-HMAC-SHA256 hashes (600,000 iterations). Issue HS256 access JWTs with an explicit
issuer/audience and a strong environment-provided key outside dev/test. Do not add cloud or crypto libraries.
Replace the Cognito-specific client_id policy with local claim and active-session validation.

Use opaque 32-byte refresh tokens stored as hashes; lock the family, consume and replace atomically.
Detect reuse and commit family revocation even while returning 401. Check session state on access requests
for immediate logout/disable/replay revocation. Use absolute family lifetime and bounded expired cleanup.
Apply PostgreSQL rate limits by direct peer IP and normalized account before expensive authentication.

Create practitioner profiles for CurrentUser only, with active martial-art validation and unique user/art.
Do not allow self-assigned coach roles or official grades. Keep demo impersonation in dev/test only,
and never use it when an Authorization header is supplied.

## Consequences

This replaces the external-provider assumption in ADR-008 and the authentication part of ADR-010.
No external provider is required. One database lookup per protected JWT request trades throughput for
immediate revocation. Mobile clients must coordinate refresh to avoid replay revocation. Public signup
verification/recovery, membership administration and deployment operations remain future tasks; this does
not declare the entire MVP complete. See local-authentication.md for precise contracts and limits.
