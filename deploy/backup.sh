#!/usr/bin/env bash
# Saves a compressed copy of the whole database and keeps the last 14.
#
#   deploy/backup.sh                 # run from anywhere
#
# Run it every night with cron, for example (crontab -e):
#   30 3 * * *  /home/deploy/recall/deploy/backup.sh >> /home/deploy/recall-backup.log 2>&1
#
# A backup on the same server does not protect you if the server is lost. Copy the files in
# deploy/backups to somewhere else as well (another server or a storage box).
#
# Restore into an empty database with:
#   gunzip -c deploy/backups/recall-YYYY-MM-DD-HHMM.sql.gz | docker compose exec -T db psql -U recall recall
set -euo pipefail

cd "$(dirname "$0")"
mkdir -p backups

# Newer Docker has the "docker compose" plugin; some machines only have the older "docker-compose".
if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
else
  COMPOSE=(docker-compose)
fi
FILE="backups/recall-$(date +%F-%H%M).sql.gz"

"${COMPOSE[@]}" exec -T db pg_dump -U recall --no-owner recall | gzip > "$FILE"

# An empty or broken file must never count as a backup.
if ! gunzip -t "$FILE" || [ "$(gunzip -c "$FILE" | wc -c)" -lt 1000 ]; then
  echo "Backup FAILED: $FILE is empty or damaged" >&2
  rm -f "$FILE"
  exit 1
fi

ls -1t backups/recall-*.sql.gz | tail -n +15 | xargs -r rm --
echo "Backup written: $FILE ($(du -h "$FILE" | cut -f1))"
