ALTER TABLE local_credential ADD COLUMN recovery_code_hash varchar(64) UNIQUE;
