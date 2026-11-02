-- Preserve organization/profile/art coherence even for non-HTTP writers.
ALTER TABLE practitioner_profile ADD CONSTRAINT practitioner_identity_art UNIQUE(id, martial_art_id);
ALTER TABLE technique ADD CONSTRAINT technique_identity_art UNIQUE(id, martial_art_id);
ALTER TABLE training_session ADD CONSTRAINT training_profile_art_fk
 FOREIGN KEY(practitioner_profile_id, martial_art_id) REFERENCES practitioner_profile(id, martial_art_id);
ALTER TABLE training_session ADD CONSTRAINT training_identity_scope
 UNIQUE(id, practitioner_profile_id, organization_id, martial_art_id);
ALTER TABLE training_review ADD CONSTRAINT review_training_scope_fk
 FOREIGN KEY(training_session_id, practitioner_profile_id, organization_id, martial_art_id)
 REFERENCES training_session(id, practitioner_profile_id, organization_id, martial_art_id);
ALTER TABLE training_review ADD CONSTRAINT review_identity_scope
 UNIQUE(id, training_session_id, practitioner_profile_id, organization_id);
ALTER TABLE feedback ADD CONSTRAINT feedback_review_scope_fk
 FOREIGN KEY(review_id, training_session_id, practitioner_profile_id, organization_id)
 REFERENCES training_review(id, training_session_id, practitioner_profile_id, organization_id);
ALTER TABLE goal ADD CONSTRAINT goal_profile_art_fk
 FOREIGN KEY(practitioner_profile_id, martial_art_id) REFERENCES practitioner_profile(id, martial_art_id);
ALTER TABLE goal ADD CONSTRAINT goal_training_scope_fk
 FOREIGN KEY(source_training_session_id, practitioner_profile_id, organization_id, martial_art_id)
 REFERENCES training_session(id, practitioner_profile_id, organization_id, martial_art_id);
ALTER TABLE goal ADD CONSTRAINT goal_technique_art_fk
 FOREIGN KEY(related_technique_id, martial_art_id) REFERENCES technique(id, martial_art_id);
ALTER TABLE goal ADD CONSTRAINT goal_title_nonblank CHECK(length(trim(title))>0);
ALTER TABLE goal ADD CONSTRAINT goal_objective_nonblank CHECK(length(trim(objective))>0);
