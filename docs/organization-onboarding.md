# Organization onboarding

This slice needs only PostgreSQL and existing local JWT authentication. All paths below use `/api/v1`.

1. Register the director, athlete and instructor using the existing auth endpoints. Each uses their own Bearer token.
2. Director: `POST /organizations` with `{"name":"My dojo","martialArtCode":"JUDO"}`. Returns 201 with `{id,name}`. Creation, supported art, director membership and audit are atomic.
3. Each recipient supplies their user ID from `GET /me`. There is no public account directory or email lookup. Email ownership is not yet verified, so invitations are bound to account UUIDs.
4. Director: `POST /organizations/{org}/invitations` with `{"recipientUserId":"<account UUID>","role":"PRACTITIONER"}`. Returns 201. Allowed roles: PRACTITIONER, INSTRUCTOR, SENSEI. No invitation can grant ADMIN or DIRECTOR. Self-invitation is rejected. Each new invitation revokes previous pending invitations for that recipient in the same dojo.
5. Recipient: `GET /me/organization-invitations`, then `POST /me/organization-invitations/{id}/accept` with no body. Returns 204. Invitations expire after seven days, are single-use, and do not themselves grant access. Wrong recipient gets 404; consumed/revoked/expired invitation gets 409 INVITATION_UNAVAILABLE.
6. `GET /me/organizations` lists only current active memberships' organizations, once per dojo. `GET /organizations/{org}/members` requires VIEW_STUDENTS and returns active membership rows, including member ID, user ID, display name and role. Lists use page/size, default 0/20, maximum size 100.
7. Athlete creates their own JUDO profile, records training with `organizationId`, and shares it. Instructor can use the existing review/feedback/goals APIs without manually seeded memberships.
8. Director: `DELETE /organizations/{org}/members/{membershipId}` returns 204 and deactivates that role. Repetition remains 204. Pending invitations for this recipient in this dojo are cancelled. The next permission check uses current memberships. Other active roles remain effective; remove all applicable role memberships to fully remove a person. Existing training and feedback history remain intact. Requests already authorized in flight are not retroactively cancelled.

DIRECTOR and ADMIN memberships cannot be removed through this API. Ownership transfer and granting additional directors/admins need a separate explicit process and are deferred. INSTRUCTOR and SENSEI cannot manage memberships with the current permission matrix.

Write operations serialize on the organization row, then check current management permissions. Acceptance rereads the invitation after that lock. Concurrent acceptance produces one success and one conflict; revocation and acceptance cannot race into an unintended resurrection. User-specific invitation reads include ownership in SQL.

Organization creation, invitation, acceptance and membership deactivation publish synchronous module events. The audit module persists them in the same transaction, without an organization-to-audit dependency or a cycle. There is no external email, push notification or invitation outbox event in this slice.

## Postman

Import `postman/collections/HAJIME-organizations.postman_collection.json`, select HAJIME Local and run in order.
It creates three fresh accounts and a dojo, stores separate tokens locally, exercises enrollment and coaching,
and checks forbidden invitations, wrong-recipient acceptance, consumed invitations and revoked permissions.
It does not delete test accounts/training history. Existing auth and regression collections remain unchanged.

## Known pilot limits

- One supported martial art is selected at creation; editing dojo configuration is not exposed yet.
- Users exchange UUIDs manually until a safe account discovery/invitation UI is implemented.
- Invitations are additive role grants, not role replacement. A new invitation does not remove already active roles.
- Pending/expired invitation rows are retained for traceability; retention can be added when actual volume warrants it.
- No public account search, ownership transfer, email delivery, dojo editing or administrator self-grant endpoint.
