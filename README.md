# HAJIME 始め

**A martial arts development platform for athletes, coaches and academies.**

HAJIME is a mobile-first platform designed to connect martial arts training, technical development, coaching, academy operations, grading and competition history in one system.

The product launches with **Judo as its first supported martial art**, while the underlying domain is intentionally designed to support additional disciplines in the future.

> Judo-first. Not Judo-only.

---

## Vision

Most martial artists currently keep their development fragmented across memory, notebooks, chat messages, spreadsheets, federation systems and conversations after training.

HAJIME connects that information into a continuous development loop:

```text
TRAIN
  ↓
RECORD
  ↓
REVIEW
  ↓
RECEIVE FEEDBACK
  ↓
WORK ON GOALS
  ↓
BUILD EVIDENCE
  ↓
GET EVALUATED
  ↓
PROGRESS
```

For Judo this means connecting:

- personal training
- randori
- technique study
- Sensei feedback
- technical goals
- dojo attendance
- Kyu/Dan progression
- grading
- promotions
- competition history

without pretending that self-reported information has the same authority as dojo-validated or official competition data.

---

# Users

HAJIME supports several contexts.

## Athlete / Judoka

The athlete can:

- record training sessions
- record techniques practiced
- record randori
- write private reflections
- share sessions with a Sensei
- receive technical feedback
- receive technical goals
- study techniques
- track progress through the dojo curriculum
- view attendance
- view belt progression
- view grading evidence
- view official competition history

## Sensei / Instructor

A Sensei can:

- view assigned students
- review shared training sessions
- provide technical feedback
- assign improvement goals
- track training evidence
- take attendance
- evaluate grading criteria
- propose belt promotions

## Dojo / Organization

Dojo administrators can eventually:

- manage members
- manage instructors
- manage permissions
- create classes
- manage schedules
- record attendance
- publish announcements
- manage curriculum
- review promotion files
- approve promotions

---

# Core principle: martial-art agnostic

HAJIME does not model the entire application around Judo-specific entities.

The generic domain speaks primarily in concepts such as:

```text
MartialArt
Practitioner
Organization
Rank
RankSystem
Technique
TechniqueCategory
Curriculum
TrainingSession
Sparring
Feedback
Goal
Evaluation
Promotion
CompetitionEvent
Match
```

A martial art provides specific policies and configuration.

For Judo, those concepts may be presented as:

```text
Organization → Dojo
Instructor   → Sensei
Sparring     → Randori
Rank         → Kyu / Dan
```

Future disciplines can provide their own terminology and rules without rewriting the core platform.

---

# Architecture

HAJIME starts as a **Modular Monolith**.

```text
                     Mobile
               Ionic + Angular
                     │
                     │ REST
                     ▼
┌──────────────────────────────────────────────┐
│                HAJIME API                    │
│               Spring Boot                    │
│              Modular Monolith                │
│                                              │
│ identity                                     │
│ organization                                 │
│ practitioner                                 │
│ martialart                                   │
│ curriculum                                   │
│ training                                     │
│ coaching                                     │
│ attendance                                   │
│ grading                                      │
│ competition                                  │
│ notification                                 │
│ media                                        │
│ audit                                        │
│                                              │
│           Domain / Application Events        │
│                     │                        │
│                  Outbox                      │
└─────────────────────┬────────────────────────┘
                      │
              ┌───────┴────────┐
              ▼                ▼
         PostgreSQL            S3
                               future
```

The project deliberately does **not** begin with microservices.

Modules have clear boundaries while remaining deployable as a single application.

Spring Modulith is used to help validate those boundaries.

---

# Backend stack

- Java 21
- Spring Boot 4.1.x
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Modulith
- PostgreSQL
- Flyway
- Testcontainers
- JUnit 5
- Maven
- OpenAPI
- Docker

---

# Mobile stack

The mobile application will use:

- Ionic 9
- Angular 22
- Capacitor
- Angular Signals
- feature-level Signal Stores
- RxJS where streams provide value

The mobile client will target:

- iOS
- Android
- Web/PWA where appropriate

The app is designed with future offline-friendly workflows in mind.

---

# Repository

```text
hajime/
├── apps/
│   ├── api/
│   └── mobile/
│
├── docs/
│   ├── architecture.md
│   ├── domain-model.md
│   ├── modules.md
│   ├── api.md
│   └── adr/
│
├── infra/
│   └── docker/
│
└── README.md
```

---

# Backend modules

## identity

Authentication identity and current-user context.

HAJIME supports local accounts and stores only versioned salted password hashes.

Authentication uses locally issued JWT access tokens and rotating refresh tokens, without a cloud provider. See [local authentication](docs/local-authentication.md).

---

## organization

Martial arts organizations and memberships.

For Judo, an organization is normally displayed as a **Dojo**.

A membership determines organization-specific roles and permissions.

---

## practitioner

A user's participation in a particular martial art.

A user may eventually have multiple practitioner profiles.

Example:

```text
Enmanuel
├── Judo — 3rd Kyu
└── BJJ — Blue Belt
```

Rank is therefore not stored directly on the global user.

---

## martialart

Defines supported martial arts and discipline-specific policies.

The initial implementation contains Judo.

Responsibilities include policy resolution for:

- ranking
- curriculum
- evaluation
- competition
- terminology

---

## curriculum

Techniques, categories, curricula and grading requirements.

For Judo, examples include:

```text
Ukemi
Ashi-waza
Koshi-waza
Te-waza
Ne-waza
```

These are Judo data, not global hardcoded enums.

---

## training

Personal training records.

A training session may include:

- duration
- organization
- practice type
- energy
- perceived intensity
- reflection
- techniques
- repetitions
- sparring / randori

---

## coaching

The athlete–coach development loop.

Contains:

- reviews
- feedback
- technical goals
- goal evidence

---

## attendance

Classes and dojo attendance.

This module will later support offline-friendly attendance workflows.

---

## grading

Technical evaluations and belt progression.

Contains future concepts such as:

- Evaluation
- EvaluationCriterion
- PromotionProposal
- Promotion

---

## competition

Competition events and match history.

The generic domain must not assume Judo scoring.

Judo-specific scoring belongs to Judo competition policies or discipline-specific data.

---

## notification

In-app and future push notifications.

Examples:

- feedback received
- new goal assigned
- upcoming class
- promotion proposed
- tournament reminder

---

## media

References to uploaded media.

Binary media will not be stored inside PostgreSQL.

Production storage will use an S3-compatible object store.

---

## audit

Audit trail for important organization and official actions.

Examples:

- feedback created
- goal assigned
- attendance modified
- evaluation changed
- promotion approved
- official result modified

---

# Trust / provenance model

HAJIME distinguishes where records originate.

```text
PERSONAL
ORGANIZATION
OFFICIAL
```

## PERSONAL

Entered by the athlete.

Examples:

- personal training
- reflection
- self-reported randori
- weight

## ORGANIZATION

Validated or created by a dojo/Sensei.

Examples:

- attendance
- feedback
- technical goals
- belt evaluation
- promotion

## OFFICIAL

Provided by an authoritative external competition or federation source.

Examples:

- official tournament
- official match
- official ranking

These sources are not treated as equally authoritative.

---

# First vertical slice

The first development milestone is intentionally small.

```text
Athlete records training
        ↓
Athlete shares session
        ↓
Sensei receives pending review
        ↓
Sensei reviews session
        ↓
Sensei sends feedback
        ↓
Sensei optionally creates goal
        ↓
Athlete sees feedback
        ↓
Athlete sees active goal
```

This slice establishes the core product loop before attendance, grading and competition become fully implemented.

---

# Initial API

Base path:

```text
/api/v1
```

Initial endpoints include:

```text
GET  /me
GET  /me/practitioner-profiles

POST /me/training-sessions
GET  /me/training-sessions
GET  /me/training-sessions/{id}

POST /me/training-sessions/{id}/share

GET  /me/goals
GET  /me/goals/{id}

GET  /organizations/{organizationId}/training-reviews
GET  /organizations/{organizationId}/training-reviews/{reviewId}

POST /organizations/{organizationId}/training-reviews/{reviewId}/feedback

POST /organizations/{organizationId}/practitioners/{practitionerId}/goals

GET /martial-arts
GET /martial-arts/{code}/techniques
GET /martial-arts/{code}/ranks
```

The API contract will evolve with the product.

---

# Development setup

## Requirements

Install:

- Java 21
- Maven
- Docker
- Docker Compose
- Git

Node/Ionic are not required until mobile development begins.

Run all commands below from the repository root (`hajime/`), which contains
the parent `pom.xml`, `mvnw`, and `mvnw.cmd`. The backend module is `apps/api`;
do not change into that directory to run the root Maven wrapper.

---

## Start infrastructure

From the repository root:

```bash
docker compose up -d
```

Verify:

```bash
docker compose ps
```

---

## Start the backend

From the repository root:

```bash
./mvnw -pl apps/api spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows (PowerShell), also from the repository root:

```powershell
.\mvnw.cmd -pl apps/api spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Run tests

Verify the complete Maven project from the repository root:

```bash
./mvnw verify
```

On Windows (PowerShell), from the same directory:

```powershell
.\mvnw.cmd verify
```

Tests include:

- unit tests
- application tests
- PostgreSQL integration tests
- API tests
- architecture/module verification

---

# Database migrations

HAJIME uses Flyway.

Production schema changes must be represented by migrations.

Hibernate schema generation must not replace migrations.

Typical development sequence:

```text
V1__baseline.sql
V2__martial_art_catalog.sql
V3__organizations.sql
V4__practitioners.sql
V5__training.sql
V6__coaching.sql
V7__outbox_and_audit.sql
V8__dev_judo_seed.sql
```

---

# Judo development dataset

Development data includes:

## Organization

Kodokan Santo Domingo

## Athlete

Enmanuel Avilez

- Judo
- White Belt
- 6th Kyu
- -81 kg

## Sensei

Marco Ruiz

## Initial Judo concepts

- Rei-gi
- Shisei
- Kumi-kata
- Yoko-ukemi
- Ushiro-ukemi
- Mae-ukemi
- Mae-mawari-ukemi
- O-soto-gari
- De-ashi-barai
- O-goshi
- Kesa-gatame

---

# Domain events

HAJIME initially uses in-process domain/application events.

Examples:

```text
TrainingSessionShared
TrainingReviewed
FeedbackCreated
GoalAssigned
```

No external message broker is required for V1.

---

# Transactional outbox

Important asynchronous work is prepared through a transactional outbox.

Example:

```text
save feedback
save outbox event
COMMIT
```

A worker can later process:

```text
FEEDBACK_CREATED
GOAL_ASSIGNED
TRAINING_SHARED
```

This avoids introducing Kafka before HAJIME actually needs Kafka.

---

# What HAJIME intentionally does not use yet

The initial architecture does not include:

- microservices
- Kafka
- RabbitMQ
- Redis
- Elasticsearch
- Kubernetes
- GraphQL
- MongoDB
- event sourcing
- full CQRS

These technologies may be introduced when an actual requirement justifies them.

---

# Why no Redis yet?

PostgreSQL is sufficient for HAJIME's initial workload.

Redis may later be appropriate for:

- distributed rate limiting
- high-volume cache
- distributed locks
- TTL-based temporary state
- realtime presence

It should be introduced to solve a measured problem, not to make an architecture diagram look sophisticated.

---

# Security

Authentication uses local accounts, JWT access tokens and rotating refresh tokens.

Set `HAJIME_JWT_SECRET` outside dev/test. Logout and session revocation are checked by the resource server.

The development aliases remain available only for dev/test walkthroughs.

Organization permissions are membership-scoped rather than global.

A user may therefore have different roles in different organizations.

Example:

```text
Organization A:
SENSEI

Organization B:
PRACTITIONER
```

---

# Multi-tenancy

HAJIME does not create a database per dojo.

Organization-owned records contain an organization identifier.

Application services and repositories must preserve organization boundaries.

Cross-organization access is explicitly tested.

---

# IDs

HAJIME uses UUIDs.

This allows the future mobile application to create certain records locally before synchronization.

This is important for future offline workflows.

---

# Offline direction

The mobile application will eventually support local persistence using Capacitor-compatible SQLite.

Likely offline-capable workflows include:

- personal training draft
- training registration
- attendance
- form drafts

A local synchronization queue will later store operations that need server synchronization.

Conflict authority follows provenance:

```text
PERSONAL
→ client-friendly conflict strategy where safe

ORGANIZATION
→ server/dojo authority

OFFICIAL
→ official source authority
```

---

# Mobile design language

HAJIME uses the **Martial Precision** visual language.

## Palette

```text
Canvas          #0B0B0D
Surface         #131315
Card            #17171B
Elevated        #1E1E23
Border          #2A2A30
Primary text    #F4F1EA
Secondary text  #A8A8B0
Torii red       #D11A2A
Dark red        #930015
Burnished gold  #C8A45D
```

## Typography

```text
Headings:
Oswald

Body:
Plus Jakarta Sans

Technical metadata:
Space Grotesk
```

The product avoids:

- decorative status dots
- unnecessary pills
- bracket-style labels
- excessive gamification
- generic SaaS appearance

---

# Product roadmap

## Phase 1

Platform foundation.

- martial-art model
- Judo catalog
- organizations
- memberships
- practitioners

## Phase 2

Training + coaching vertical slice.

- training journal
- training sharing
- Sensei review
- feedback
- goals

## Phase 3

Mobile application.

- Ionic shell
- authentication
- athlete home
- training
- goals
- learning library

## Phase 4

Dojo operations.

- classes
- attendance
- member management
- Sensei permissions

## Phase 5

Grading.

- evaluation
- requirements
- promotion proposals
- promotion approval
- rank history

## Phase 6

Competition.

- events
- athlete competitive profile
- match history
- official results

## Phase 7

Offline and notification maturity.

- SQLite
- sync queue
- push notifications
- retry processing

---

# Architecture decisions

Important architectural decisions are recorded under:

```text
docs/adr/
```

Initial ADRs cover:

- Modular Monolith
- Martial Art Policies
- PostgreSQL-first architecture
- Avoiding premature microservices
- Record provenance
- Transactional outbox

---

# Philosophy

HAJIME should remain easy for a small engineering team to understand.

Complexity must be earned.

A new database, message broker, cache, service or framework must solve a concrete product or operational problem.

The goal is not to create the most impressive architecture diagram.

The goal is to ship a martial arts platform that can grow without becoming impossible to maintain.

**始め — Begin.**

### Real dojo onboarding

Real accounts can now create a dojo, invite existing accounts, accept invitations and use coaching without development memberships. See [organization onboarding](docs/organization-onboarding.md). Runnable collection: `postman/collections/HAJIME-organizations.postman_collection.json`. Migration V11 is applied on the next application startup.

### MVP lifecycle and account security

The API supports owner edits before sharing, coach goal completion/cancellation, password change and
single-use offline account recovery. See [API](docs/api.md), [account security](docs/local-authentication.md)
and [release operations](docs/release-operations.md). Import `postman/collections/HAJIME-mvp.postman_collection.json`
for the complete real-account walkthrough. V12 adds hashed recovery codes on the next backend startup.
