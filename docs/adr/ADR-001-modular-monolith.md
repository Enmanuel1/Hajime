# ADR-001: Modular monolith

Status: Accepted

## Context

One team needs a coherent product slice, local transactions, and enforceable ownership without operating distributed services.

## Decision

Deploy one Spring application. Use feature modules with package-root contracts, explicit dependencies, and Spring Modulith verification. Keep infrastructure behind meaningful ports.

## Consequences

Transactions remain local and deployment stays simple. Boundaries require discipline and tests; a shared process does not imply unrestricted imports.
