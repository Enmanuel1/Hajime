# HAJIME mobile (planned)

No Ionic application is implemented yet. The backend vertical slice must remain stable first.

Target: Ionic 9, Angular 22, Capacitor. Feature-first structure:

```text
src/app/
  core/
  shared/
  features/
    auth/ home/ training/ goals/ learn/ profile/ competition/ sensei/ dojo/
```

Within each feature: `pages/`, `components/`, `data-access/`, `domain/`, `state/`.
Use Angular Signals and feature Signal Stores, without one global NgRx store.
Future offline storage: Capacitor SQLite and a durable `sync_operation` queue.
Personal records can use client-generated UUIDs; retries must reconcile duplicate IDs.
PERSONAL conflicts favor the client where safe, ORGANIZATION records are dojo authoritative,
and OFFICIAL records retain authoritative source precedence. These are planned rules, not a sync implementation.

Use the existing Martial Precision design tokens in [the design reference](../../docs/mobile-design.md).
Confirm framework release compatibility when mobile implementation begins.
