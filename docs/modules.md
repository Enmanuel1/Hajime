# Module boundaries

Only types at `com.hajime.<module>` are exported. Subpackages are internal. Every module declares `allowedDependencies`, and the architecture test rejects unauthorized dependencies, imports of internal types, and cycles.

| Module | Public contracts | Data ownership / maturity |
| --- | --- | --- |
| identity | CurrentUser, UserDirectory | External identities; dev/test and resource-server boundary |
| organization | Organization, OrganizationMembership, OrganizationMartialArt, OrganizationAccess, Permission, MembershipRole | Organizations and membership queries; administrative writes deferred |
| martialart | Catalog, MartialArtCode, RankSystem, TechniqueCategory, MartialArtPolicy and cohesive policies/registry | Catalog and Judo configuration/behavior |
| practitioner | Practitioners/Profile | Profile ownership and rank/weight; creation deferred |
| curriculum | Curriculum, CurriculumRequirement | Schema and versioned public concepts; authoring deferred |
| training | TrainingView, TrainingAccess, TrainingSessionShared, TrainingReviewed | Aggregate, repository port/JPA adapter, athlete endpoints |
| coaching | FeedbackCreated, GoalAssigned | Reviews/feedback/goals, explicit SQL port/adapter |
| attendance | AttendanceRecord | Skeleton; future classes, attendance capture, audited corrections |
| grading | GradingContracts | Skeleton; evaluation criteria, proposal/approval separation |
| competition | CompetitionContracts | Skeleton; neutral match/result model, official provenance |
| media | MediaPort | Interface only; future S3 signing/authorization adapter |
| audit | AuditTrail | Transactional audit adapter; consumes training/coaching events |
| notification | NotificationDelivery, OutboxProcessor | Outbox writer and opt-in local scheduled delivery |
| shared | ApiException, PageResult, RecordSource | Small shared kernel and common HTTP/OpenAPI adapters |

Cross-module events carry identifiers and minimal metadata, never persistence entities. Training has no dependency on coaching: the latter subscribes to the public sharing event. Audit and notification consume events without being called by controllers. Review completion uses TrainingAccess with organization scope. Persistence methods require ownership IDs or organization scope for product reads.

Future attendance, grading, and competition work must preserve these boundaries. Add dependencies deliberately, not by exporting entire modules as open. Notification providers and media storage must implement ports without adding infrastructure to domain objects.
