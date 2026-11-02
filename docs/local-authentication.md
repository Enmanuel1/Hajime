# Local authentication and practitioner onboarding

This implementation requires only Java and PostgreSQL. No Cognito, AWS, mail service or new runtime dependency.

## API contract

All paths start with `/api/v1`. Auth endpoints are public POSTs and should be called with No Auth,
especially refresh: do not attach an expired Bearer token to the refresh request. Token responses have
Cache-Control: no-store and Pragma: no-cache. There are no authentication cookies.

| Method / path | Body | Result |
| --- | --- | --- |
| POST /auth/register | email, password, displayName | 201 + tokens and user |
| POST /auth/login | email, password | 200 + tokens and user |
| POST /auth/refresh | refreshToken | 200 + replacement access/refresh tokens |
| POST /auth/logout | refreshToken | 204, revoke that session |
| POST /me/password | currentPassword, newPassword | 204, revoke all sessions and recovery code |
| POST /me/recovery-code | currentPassword | 200, new single-use recoveryCode (shown once) |
| POST /auth/recover | email, recoveryCode, newPassword | 204, consume code and revoke all sessions; No Auth |
| POST /me/practitioner-profiles | martialArtCode, optional weightKg | 201 + owned profile and Location |
| GET /me/practitioner-profiles | none | 200, own profiles |
| GET /me/practitioner-profiles/{id} | none | 200 own profile / 404 outside ownership |

Example registration:

```json
{"email":"athlete@example.test","password":"A long private test passphrase","displayName":"New athlete"}
```

Response shape (values abbreviated):

```json
{
  "accessToken":"...",
  "refreshToken":"...",
  "tokenType":"Bearer",
  "expiresIn":900,
  "user":{"id":"...","displayName":"New athlete"}
}
```

Subsequent protected requests use `Authorization: Bearer <accessToken>`. Create a profile with:

```json
{"martialArtCode":"JUDO","weightKg":80.5}
```

The owner is always the authenticated user. Current rank starts null; the caller cannot choose an
owner, role, membership, source or official grade. Unknown fields in registration/login/profile creation
are rejected. Profile uniqueness is user + martial art: duplicates return 409 PROFILE_ALREADY_EXISTS.
An inactive existing profile is not reactivated by creating another one. Weight is optional, positive,
at most 999.99 and at most two decimal places. Arts must be active with registered behavior.

Email is trimmed and normalized to lowercase; addresses must have a conventional valid email shape.
Passwords are 15–128 characters, without silent trimming or truncation. Spaces/passphrases are allowed.
Unknown, incorrect or disabled credentials use the same 401 INVALID_CREDENTIALS response.
Duplicate registration returns 409 ACCOUNT_UNAVAILABLE and rolls back the extra identity.

## Sessions

Access JWTs default to 15 minutes, with standard decoder clock tolerance. Signature, issuer, audience,
subject, token use, expiration and the active session are checked. Every protected JWT request consults
PostgreSQL: this is a deliberate cost for immediate revocation in the modular monolith.

Refresh tokens are opaque random values, not JWTs. Their SHA-256 hashes are stored; plaintext tokens
are returned only to the client. Each successful refresh consumes the old value. Save the NEW pair
atomically and never reuse the old refresh token. Reuse revokes that session, including the token pair
returned to a competing refresh caller. Therefore the mobile client must coordinate one refresh at a time.
A user can have independent sessions on different devices; revoking one does not revoke all devices.

Logout accepts a valid-format refresh token and returns 204 even if already logged out or unknown. A known
consumed token can also revoke its family. A malformed refresh value returns 401 INVALID_REFRESH_TOKEN.
After logout both access and refresh for that session fail. Disabled accounts also fail immediately.
Sessions expire after an absolute 30 days; refreshing does not extend that date. Access expiresIn is capped
by the remaining session lifetime. Expired families older than a day are cleaned in batches of 1,000 hourly;
consumed hashes in live families remain available for replay detection.

## Configuration

```bash
export HAJIME_JWT_SECRET="$(openssl rand -base64 32)"
./mvnw -pl apps/api spring-boot:run
```

Generate and store a stable key outside version control for any persistent non-dev deployment. Changing
it invalidates access JWTs. In dev/test an omitted key is generated per process; do not rely on an old
access token after restarting. The refresh/session database still exists across a process restart.

Defaults under `hajime.auth`: issuer `hajime`, audience `hajime-app`, access-ttl PT15M, session-ttl P30D,
rate-window PT15M, max-ip-attempts 100, max-account-attempts 10. Time and issuer/audience are explicit.
Auth IP limits cover all POST /auth routes, including recovery. Account buckets cover login, recovery and authenticated password proof (separate namespaces). Counters commit even
when the login fails. Exceeding limits returns 429 RATE_LIMITED; the IP response includes Retry-After.
The direct peer IP is used, not an arbitrary client-supplied forwarded header. Configure trusted ingress
before proxy deployment; otherwise all proxied users may share the proxy's rate bucket.

## Development and deployment limits

The legacy dev collection can still use X-Dev-User: athlete/sensei/outsider. A supplied Authorization header
always uses real JWT authentication; a bad Bearer token cannot fall back to a demo account. Auth requests
skip development impersonation. Non-dev profiles never accept the dev header and require the signing key.

Registration currently allows personal accounts without email verification. No dojo membership or coaching
permission is granted. Membership administration/invitations and password change/offline recovery are implemented. Email verification,
MFA, account deletion and email-based recovery remain separate work. Serve production traffic over HTTPS and store client
tokens securely; do not log them or sync real refresh/password values into shared Postman environments.

V10 adds local credential/session/rate tables without changing previous migrations or removing demo users.


## Password change and offline recovery

`POST /me/password` requires a real account's Bearer token and correct current password. New passwords
follow the 15–128 character policy and must differ from the supplied current password. On success every
session, including the caller, is revoked and any recovery code is invalidated. Log in again. Unknown
ownership/role fields are rejected; no target user ID is accepted.

`POST /me/recovery-code` requires a Bearer token and current password. It returns `{"recoveryCode":"..."}`
with no-store headers. Save that value outside the app in a private place; there is no read-back endpoint.
A new generation immediately replaces the previous code. This is a 256-bit random value; only SHA-256 is
stored in V12's nullable recovery_code_hash. The code has no time expiry, but is single-use, rotatable,
and invalidated by password change/recovery. It is a credential, not an email verification mechanism.

If the password is lost, call `POST /auth/recover` with email, saved recoveryCode and newPassword, using
No Auth. Incorrect, consumed, disabled-account and unknown-account recovery details all return
401 INVALID_RECOVERY_CODE. Valid recovery consumes the code and changes the password atomically, then
revokes every session. It does not auto-login or issue replacement tokens. Log in with the new password
and generate/store a new recovery code. Without both password and a saved code there is no self-service
recovery in this pilot; no public administrative bypass was added.

Security actions are audited without passwords, tokens, codes or email metadata. Credential locks precede
session locks in refresh/change/recovery so concurrent refresh cannot leave a usable old session or invert
lock ordering. Rate counters commit independently of rejected requests.
