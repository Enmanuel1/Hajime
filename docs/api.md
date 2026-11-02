# REST API v1

Base path: `/api/v1`. JSON request/response DTOs; UUID identifiers; ISO-8601 timestamps with UTC offsets; dates are `YYYY-MM-DD`. OpenAPI is at `/v3/api-docs`, Swagger at `/swagger-ui.html` in development. POST create operations return 201; sharing returns 200. Development identity header: `X-Dev-User: athlete|sensei|outsider`; omitted means athlete. No such header authenticates outside dev/test. Local registration/login/refresh/logout and profile onboarding are documented in [local authentication](local-authentication.md). Explicit Bearer tokens use real authentication even in dev.

## Endpoints

| Method | Path (relative to /api/v1) | Scope |
| --- | --- | --- |
| GET | /me | Current identity |
| POST | /organizations | Create dojo; creator becomes DIRECTOR of that dojo |
| GET | /me/organizations | Own active organizations, paginated |
| GET | /organizations/{org}/members | VIEW_STUDENTS; active memberships, paginated |
| POST | /organizations/{org}/invitations | MANAGE_MEMBERS; recipient-bound invitation |
| GET | /me/organization-invitations | Own unexpired pending invitations, paginated |
| POST | /me/organization-invitations/{id}/accept | Recipient only; consumes invitation, returns 204 |
| DELETE | /organizations/{org}/members/{id} | MANAGE_MEMBERS; deactivate one membership, returns 204 |
| GET | /me/practitioner-profiles | Own discipline profiles |
| POST | /me/training-sessions | Record personal training |
| PUT | /me/training-sessions/{id} | Owner, RECORDED only, required version, replace content/organization |
| GET | /me/training-sessions | Own paginated history |
| GET | /me/training-sessions/{id} | Own detail including technique and sparring entries |
| POST | /me/training-sessions/{id}/share | Owner, current organization membership, expected version |
| GET | /me/feedback | Own paginated feedback; optional trainingSessionId filter |
| GET | /me/goals | Own goals; status defaults ACTIVE |
| GET | /me/goals/{id} | Own goal detail |
| GET | /organizations/{org}/goals/{id} | ASSIGN_GOAL; scoped goal detail |
| POST | /organizations/{org}/goals/{id}/complete | ASSIGN_GOAL; ACTIVE → ACHIEVED, required version |
| POST | /organizations/{org}/goals/{id}/cancel | ASSIGN_GOAL; ACTIVE → CANCELLED, required version |
| GET | /organizations/{org}/training-reviews | REVIEW_TRAINING; status defaults PENDING |
| GET | /organizations/{org}/training-reviews/{id} | REVIEW_TRAINING; shared session, review, feedback |
| POST | /organizations/{org}/training-reviews/{id}/feedback | REVIEW_TRAINING; optional goal also requires ASSIGN_GOAL |
| POST | /organizations/{org}/practitioners/{id}/goals | ASSIGN_GOAL; target must belong to same organization |
| GET | /martial-arts | Active martial arts |
| GET | /martial-arts/{code}/ranks | Configured ranks ordered by system/order |
| GET | /martial-arts/{code}/techniques | Active techniques, paginated via page/size |

History/review/feedback/goal lists return `{content, page, size, totalElements}`. `page` starts at 0; `size` is 1–100, default 20. Technique lists default to 30 and return a bounded array. Catalog arts/ranks and own practitioner profiles are small configuration/profile lists and return arrays. Training history supports `sort=occurredAt,desc` (default) or `occurredAt,asc`; ordering includes UUID as a stable tiebreaker. Reviews sort oldest first; goals/feedback newest first. Review statuses: PENDING, REVIEWED. Goal filters: ACTIVE, ACHIEVED, CANCELLED.

## Record and share training

```sh
curl -i -H 'Content-Type: application/json' \
  -d '{"id":"50000000-0000-0000-0000-000000000001","practitionerProfileId":"40000000-0000-0000-0000-000000000001","organizationId":"30000000-0000-0000-0000-000000000001","occurredAt":"2026-10-31T22:00:00Z","durationMinutes":90,"practiceType":"MIXED","perceivedIntensity":6,"energyLevel":4,"reflection":"Kept sleeve control; improve supporting foot placement.","techniques":[{"techniqueId":"14000000-0000-0000-0000-000000000008","repetitions":20}],"sparring":[{"rounds":4,"intensity":6}]}' \
  http://localhost:8080/api/v1/me/training-sessions

curl -H 'Content-Type: application/json' -d '{"version":0}' \
  http://localhost:8080/api/v1/me/training-sessions/50000000-0000-0000-0000-000000000001/share

curl -H 'X-Dev-User: sensei' \
  http://localhost:8080/api/v1/organizations/30000000-0000-0000-0000-000000000001/training-reviews
```

The example's fixed ID is for readability. Repeating the same ID/content returns the current session with 201; different content returns 409 TRAINING_ID_CONFLICT. Generate a new UUID for a new personal record. `id` may be omitted for a server-generated UUID. `organizationId`, perceived intensity, energy, and reflection may be omitted/null. `techniques` and `sparring` must be present; use empty arrays when absent. Sharing requires an organization to have been selected when recording. Only recorded sessions can be shared; resubmitting an outdated version returns 409.

Duration: 1–720 minutes; intensity: 1–10; energy: 1–5. Reflection maximum 4,000 characters. At most 100 technique and 100 sparring entries; duplicate technique IDs are rejected. Practice types: TECHNICAL, SPARRING, CONDITIONING, MIXED. Technique IDs must be active and belong to the profile's martial art. Future dates are rejected. The request cannot set source, owner, or state.

## Review and assign a goal

Read the review ID and version from the pending queue. POST to `/organizations/{org}/training-reviews/{id}/feedback`:

```json
{
  "version": 0,
  "body": "Bring your supporting foot closer before the O-soto-gari reap.",
  "goal": {
    "relatedTechniqueId": "14000000-0000-0000-0000-000000000008",
    "title": "Balanced O-soto-gari entry",
    "objective": "Demonstrate a stable entry in three supervised sessions.",
    "coachObservation": "Upper-body control is improving; keep posture during entry.",
    "priority": "HIGH",
    "reviewDate": "2026-12-01"
  }
}
```

`goal` is optional. The response contains `feedback` and optional `goal`. The goal's source session is set to the reviewed session. The entire request is atomic, including audit/outbox. An invalid optional goal leaves the review pending and creates no feedback. A reviewed session cannot receive a second initial review.

For a separate goal, POST the inner goal object to `/organizations/{org}/practitioners/{id}/goals`; `sourceTrainingSessionId` is optional and, when supplied, must refer to shared training for that practitioner in the same organization. Title/objective/priority are required. Priorities: LOW, NORMAL, HIGH. Review date is optional and cannot be in the past. Goals have source ORGANIZATION and initial status ACTIVE. Closing active goals uses the explicit complete/cancel endpoints below; no artificial progress percentage is exposed.

Athletes retrieve `/me/feedback?trainingSessionId={id}`, `/me/goals`, and `/me/goals/{id}`. Foreign athlete records return 404. Missing organization permission returns 403; even an authorized coach from another organization cannot resolve a review through their own organization path.

## Complete repeatable walkthrough

With PostgreSQL and the dev API running:

```sh
python3 scripts/smoke.py
```

This stdlib-only script generates new UUIDs, records training, reads history/detail, shares it, locates and opens the coach review, submits feedback plus a goal, verifies athlete feedback/goals, and checks foreign-organization access. It creates demo journal/review/goal rows each run.

## Errors

RFC 9457 `application/problem+json` uses `type`, `title`, `status`, `detail` where applicable, and stable `code`. Validation adds `errors: [{field,message}]` for the mobile client. Invalid JSON/enums/UUIDs return 400, unauthenticated requests 401, organization authorization failures 403, scoped missing records 404, invalid transitions/stale versions/duplicate IDs 409, and unsupported behavior policy 422.

Codes include TRAINING_NOT_FOUND, GOAL_NOT_FOUND, REVIEW_NOT_FOUND, PRACTITIONER_NOT_FOUND, ACCESS_DENIED, UNAUTHENTICATED, INVALID_STATE_TRANSITION, CONCURRENT_MODIFICATION, MARTIAL_ART_NOT_SUPPORTED, TECHNIQUE_MISMATCH, ORGANIZATION_MARTIAL_ART_MISMATCH, VALIDATION_FAILED, INVALID_REQUEST, INVALID_PAGE, INVALID_SORT, INVALID_STATUS, DUPLICATE_TECHNIQUE, ORGANIZATION_REQUIRED, GOAL_TRAINING_MISMATCH, INTERNAL_ERROR. Error bodies never serialize persistence entities or stack traces.


## Recording retries and conflicts

`POST /api/v1/me/training-sessions` accepts an optional client UUID. Repeat the same UUID and
recorded content to recover a lost creation response: the server returns HTTP 201, the same
Location, and the current session, including any later sharing/review state. Technique order
is ignored; sparring entry order is significant. Occurrence times are normalized to microseconds.
Conflicting content or ownership returns 409 `TRAINING_ID_CONFLICT` without returning the stored
session. Without a client UUID, each successful request creates a new session. Replays still
require current creation permissions and active catalog references; this is not an offline sync protocol.

Inactive profiles retain access to their history but cannot share. Optimistic-lock conflicts
use `CONCURRENT_MODIFICATION`; other storage integrity conflicts use `DATA_CONFLICT`.

Goal review dates are checked against the current UTC date by the domain factory, including non-HTTP callers.


## Dojo onboarding with real accounts

Create with `POST /organizations` and `{"name":"My dojo","martialArtCode":"JUDO"}`.
The server assigns DIRECTOR only in the new organization. No rank or global privilege is assigned.
See [organization onboarding](organization-onboarding.md) for invitation, permission and revocation rules.


## Correct training before sharing

`PUT /me/training-sessions/{id}` returns 200 with the updated session. Send the full editable body:
required `version`, `occurredAt`, `durationMinutes`, `practiceType`, `techniques` and `sparring`;
optional `organizationId`, `perceivedIntensity`, `energyLevel` and `reflection`. Arrays replace their
previous contents; empty arrays clear entries. Omitting optional fields clears them (full replacement).
You may choose/change/remove the dojo before sharing; any selected dojo requires active membership and
support for the profile's discipline. Active owned profile and catalog validation still apply.

ID, practitioner profile, martial art, source and creation time remain server-controlled. Unknown fields
are rejected, including ownership/provenance/status fields. Only RECORDED sessions are editable; shared
and reviewed history stays fixed. The server increments version. Stale version or simultaneous edit/share
returns 409 CONCURRENT_MODIFICATION; editing a current but closed state returns 409 INVALID_STATE_TRANSITION.
Another owner sees 404. Repeating an old creation UUID after editing may conflict with current content;
use the update endpoint and current version rather than replaying the original creation payload.

## Close goals

A coach with ASSIGN_GOAL reads `GET /organizations/{org}/goals/{id}` for the latest version, then posts
`{"version":0}` to its `/complete` or `/cancel` route. The response is the updated goal with 200,
ACHIEVED/CANCELLED and incremented version. Only ACTIVE goals can close. Stale requests, repeat transitions
and a competing completion/cancellation return 409; foreign organization records return 404 after the
permission check. Athletes cannot self-complete a coach-assigned goal through their own read endpoints.
They see the result via `/me/goals?status=ACHIEVED` or `CANCELLED` and their own goal detail.
Closing is audited atomically. The original assignment, provenance and text are preserved. No goal reopening
or external completion notification was added. Historical goals may be closed by an authorized coach even
if their athlete subsequently left the dojo; closing does not reactivate membership.
