# ADR-005: Record provenance

Status: Accepted

## Context

Personal observations, dojo-validated records, and official results have different authority, especially during future sync.

## Decision

Use PERSONAL, ORGANIZATION, OFFICIAL through a shared RecordSource. Training entered by an athlete remains PERSONAL even after review; an instructor-assigned goal is ORGANIZATION.

## Consequences

Visibility/state and provenance remain separate. Future sync must apply source-aware conflict rules; no offline synchronizer is implemented here.
