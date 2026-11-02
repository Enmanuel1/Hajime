-- Existing demo/external identities remain valid; local credentials are opt-in per user.
CREATE TABLE local_credential (
 user_id uuid PRIMARY KEY REFERENCES app_user,
 email varchar(254) NOT NULL UNIQUE CHECK(email=lower(trim(email))),
 password_hash varchar(512) NOT NULL,
 active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE auth_session (
 id uuid PRIMARY KEY,
 user_id uuid NOT NULL REFERENCES local_credential(user_id),
 created_at timestamptz NOT NULL,
 expires_at timestamptz NOT NULL CHECK(expires_at>created_at),
 revoked_at timestamptz
);
CREATE INDEX auth_session_user ON auth_session(user_id);
CREATE INDEX auth_session_expiry ON auth_session(expires_at,id);
CREATE TABLE refresh_token (
 token_hash varchar(64) PRIMARY KEY,
 session_id uuid NOT NULL REFERENCES auth_session ON DELETE CASCADE,
 created_at timestamptz NOT NULL,
 consumed_at timestamptz
);
CREATE INDEX refresh_session ON refresh_token(session_id);
CREATE TABLE auth_rate_limit (
 bucket_hash varchar(64) PRIMARY KEY,
 window_start timestamptz NOT NULL,
 attempts integer NOT NULL CHECK(attempts>0)
);
CREATE INDEX auth_rate_limit_window ON auth_rate_limit(window_start);
