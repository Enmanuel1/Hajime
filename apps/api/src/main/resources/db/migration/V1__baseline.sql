CREATE TABLE app_user (
 id uuid PRIMARY KEY,
 subject varchar(255) NOT NULL UNIQUE,
 display_name varchar(160) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
