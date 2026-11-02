# ADR-013: Local dojo onboarding with recipient-bound invitations

Status: Accepted.

## Context

Own accounts and profiles existed, but real users could not enroll in a dojo without database changes. Coaching depended on development memberships. The pilot needs a complete path without a paid identity, email or workflow service.

## Decision

Create organizations atomically with one supported art and a director scoped to the new organization. Reuse the existing role/permission matrix. Invitation targets are existing account UUIDs, not unverified email addresses. A recipient must explicitly accept; invitations expire after seven days and cannot grant ADMIN/DIRECTOR. Membership removal deactivates a specific nonprotected role and cancels pending invitations for that recipient.

Serialize writes using the organization row. Recheck authorization and invitation state after acquiring it. Keep SQL in the organization repository, orchestration in the application service and HTTP mapping in the controller. Publish root-package events; the audit module subscribes synchronously in the same transaction. Keep authentication, practitioner profiles, ranks and dojo roles separate.

## Consequences

Real account onboarding reaches training and coaching without seeds. No external provider is needed. Clients currently exchange account UUIDs manually. Multi-role membership is retained: removing a role does not remove other active roles. Ownership transfer, account discovery, invitation email, expired-row retention and dojo administration beyond membership remain separate work.
