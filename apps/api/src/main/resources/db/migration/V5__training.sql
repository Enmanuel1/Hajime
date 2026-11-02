CREATE TABLE training_session(
 id uuid PRIMARY KEY, practitioner_profile_id uuid NOT NULL REFERENCES practitioner_profile,
 martial_art_id uuid NOT NULL REFERENCES martial_art, organization_id uuid REFERENCES organization,
 occurred_at timestamptz NOT NULL, duration_minutes integer NOT NULL CHECK(duration_minutes BETWEEN 1 AND 720),
 practice_type varchar(30) NOT NULL CHECK(practice_type IN ('TECHNICAL','SPARRING','CONDITIONING','MIXED')),
 perceived_intensity integer CHECK(perceived_intensity BETWEEN 1 AND 10), energy_level integer CHECK(energy_level BETWEEN 1 AND 5),
 reflection varchar(4000), source varchar(20) NOT NULL CHECK(source IN ('PERSONAL','ORGANIZATION','OFFICIAL')),
 status varchar(30) NOT NULL CHECK(status IN ('DRAFT','RECORDED','SHARED_WITH_COACH','REVIEWED')),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0,
 CHECK(status NOT IN ('SHARED_WITH_COACH','REVIEWED') OR organization_id IS NOT NULL)
);
CREATE INDEX training_history ON training_session(practitioner_profile_id,occurred_at DESC,id);
CREATE INDEX training_org ON training_session(organization_id,status);
CREATE TABLE training_technique(training_session_id uuid NOT NULL REFERENCES training_session ON DELETE CASCADE, technique_id uuid NOT NULL REFERENCES technique, repetitions integer CHECK(repetitions>0), duration_minutes integer CHECK(duration_minutes BETWEEN 1 AND 720), notes varchar(1000), PRIMARY KEY(training_session_id,technique_id));
CREATE TABLE sparring_entry(training_session_id uuid NOT NULL REFERENCES training_session ON DELETE CASCADE, entry_index integer NOT NULL, rounds integer CHECK(rounds BETWEEN 1 AND 100), intensity integer CHECK(intensity BETWEEN 1 AND 10), notes varchar(1000), PRIMARY KEY(training_session_id,entry_index));
