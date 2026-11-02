# ADR-002: Martial-art policy model

Status: Accepted

## Context

HAJIME starts with Judo but must add disciplines without conditional branching in every use case.

## Decision

Keep rank order, techniques, curricula, and metadata in relational configuration. Resolve cohesive ranking/curriculum/evaluation/competition/terminology policies through a registry keyed by an extensible code value.

## Consequences

A new discipline supplies data and a policy bundle. Rulebook-specific complexity stays in policy implementations. Prototype policies are not federation certification.
