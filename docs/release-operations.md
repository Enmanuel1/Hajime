# Pilot release operations

## What has been verified locally

The packaged API starts with `prod` profile and a fresh PostgreSQL 17.6 database. Real JWT auth protects
product routes. The same stable signing key and persisted sessions allow existing access tokens after
restart. A custom-format database dump restores into a separate empty database; table counts and content
fingerprints match, Flyway validates, and an existing session authenticates against the restored API.
These are local release checks, not evidence of a public HTTPS deployment.

## Runtime configuration

Use Java 21 and PostgreSQL, DATABASE_URL (JDBC URL), DATABASE_USER, DATABASE_PASSWORD and a persistent
HAJIME_JWT_SECRET (Base64, at least 32 random bytes). Generate/store the signing key once outside Git;
reuse it at restart. Do not activate dev/test alongside prod. Keep PostgreSQL on a private interface.
The existing Dockerfile runs the API as a nonroot user; no new workflow/cloud dependency was added.

For a browser frontend, set HAJIME_WEB_CORS_ALLOWED_ORIGINS to exact origins separated by commas, e.g.
`https://app.example.test,http://localhost:5173`. Values cannot contain a path, wildcard, credentials or
query. Empty means no cross-origin browser access. CORS allows GET/POST/PUT/DELETE/OPTIONS and the
Authorization/Content-Type headers, exposes Location and does not enable credential cookies. It does not
replace Bearer authentication. Native mobile clients do not need CORS. `.env.example` is a template;
export/inject these variables into the actual API process.

Before public exposure, choose the hostname/host, configure HTTPS termination and trusted proxy behavior,
then verify the actual request peer used for rate limiting. Do not trust arbitrary forwarded IP headers.
Without trusted proxy configuration, proxied clients share the ingress IP's rate bucket. Restrict direct
backend access if forwarded headers are enabled. These network checks depend on the chosen host and
are not claimed complete. No public deployment was performed.

## Backup and restore

`scripts/database_backup.py` uses standard PostgreSQL clients with PGDATABASE/PGUSER/PGHOST/PGPORT/
PGPASSWORD/PGSSLMODE. Set PGDATABASE explicitly. Use environment/secret injection for credentials.
An optional `--container <existing-container>` uses those clients inside a Docker container and forwards
only the PG settings; use the container's port/host rather than the published host port.

```sh
# Set PG* for the source, then create a NEW backup filename:
python3 scripts/database_backup.py backup /private/backups/hajime-2026-11-02.dump
# Or add --container <postgres-container-name> to use its matching client tools.
```

The parent directory must already exist. Dumps use custom format, no ownership/ACL restoration, and a
private temporary file. A completed dump is published without overwriting another file. On POSIX the
result has permissions 0600. It contains sensitive account data; retain it in private storage. Scheduling,
rotation, encryption at rest and off-machine storage depend on the selected host and remain operator work.

Restore only into a separately created empty application database; change PGDATABASE to that target:

```sh
python3 scripts/database_backup.py restore /private/backups/hajime-2026-11-02.dump
```

The script refuses default system databases and targets with existing user tables. It does not drop or
clean a populated database. Stop writers during recovery and use a database owned by the recovery process.
Restore runs in a single transaction and stops on errors. Start the API against the restored target with
the same signing key, verify health, login, existing records and permissions, then deliberately switch the
connection/traffic. The script does not switch the application automatically.

A dump contains the database, not the JWT signing secret. Back up that secret separately in private secret
storage. Restoring an older snapshot may restore sessions/recovery codes that were invalidated after that
snapshot; for incident recovery, revoke sessions and recovery codes and rotate the signing key as needed
before opening traffic. A database restore cannot reconstruct changes made after the backup timestamp.
