#!/usr/bin/env python3
"""Explicit PostgreSQL backup/restore using PG* environment settings and standard clients."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=["backup", "restore"])
    parser.add_argument("file", type=Path)
    parser.add_argument("--container", help="Optional existing Docker container containing PostgreSQL clients")
    args = parser.parse_args()
    database = os.environ.get("PGDATABASE", "")
    if not database:
        parser.error("Set PGDATABASE explicitly; use PGUSER/PGHOST/PGPORT/PGPASSWORD as needed.")
    if args.operation == "restore" and database in {"postgres", "template0", "template1"}:
        parser.error("Restore requires a dedicated empty application database.")

    def command(tool, *flags):
        prefix = []
        if args.container:
            prefix = ["docker", "exec", "-i"]
            for key in ("PGDATABASE", "PGUSER", "PGHOST", "PGPORT", "PGPASSWORD", "PGSSLMODE"):
                if key in os.environ:
                    prefix += ["-e", key]
            prefix += [args.container]
        return prefix + [tool, *flags]

    if args.operation == "backup":
        if args.file.exists():
            parser.error("Backup destination already exists; choose a new filename.")
        # Same-directory temporary file, private permissions, publish only after dump succeeds.
        fd, temporary = tempfile.mkstemp(prefix=".hajime-backup-", dir=args.file.parent)
        temporary = Path(temporary)
        try:
            with os.fdopen(fd, "wb") as output:
                subprocess.run(command("pg_dump", "--format=custom", "--no-owner", "--no-acl"), stdout=output, check=True)
            # Hard link fails rather than overwriting if another process created the destination.
            os.link(temporary, args.file)
        finally:
            temporary.unlink(missing_ok=True)
        print("Backup created with private file permissions.")
    else:
        if not args.file.is_file():
            parser.error("Backup file does not exist.")
        count = subprocess.check_output(command("psql", "-X", "-A", "-t", "--set=ON_ERROR_STOP=1", "-c", "select count(*) from information_schema.tables where table_schema not in ('pg_catalog','information_schema')"), text=True).strip()
        if count != "0":
            parser.error("Restore target is not empty. Create a separate empty database first.")
        with args.file.open("rb") as source:
            subprocess.run(command("pg_restore", "--dbname", database, "--single-transaction", "--exit-on-error", "--no-owner", "--no-acl"), stdin=source, check=True)
        print("Restore completed. Validate application startup and retained records before switching traffic.")


if __name__ == "__main__":
    main()
