# ADR-004: No microservices yet

Status: Accepted

## Context

Network boundaries would make sharing/reviewing and goals harder to keep atomic before the domain stabilizes.

## Decision

No service extraction, message broker, event sourcing, full CQRS, Kubernetes, or service mesh. Use in-process public events and modular contracts.

## Consequences

Avoid distributed consistency and operational costs. If extraction becomes necessary, existing module contracts and outbox events provide starting seams, not a promise of effortless extraction.
