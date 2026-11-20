#!/usr/bin/env bash
# Creates a business and one login for it. There is no sign-up page yet, so this is how a
# pilot clinic gets an account.
#
#   scripts/create-clinic.sh "Clinic name" owner@clinic.example "a-strong-password" [CLINIC|SALON|AUTO_SERVICE]
#
# Needs psql and htpasswd (both come with macOS developer tools / Homebrew postgresql).
# The database defaults to the local dev one. To target another, set DATABASE_URL_PSQL, for example
#   DATABASE_URL_PSQL="postgresql://user:password@host:5432/recall"
# On the production server the database has no public port, so run psql inside its container instead:
#   RECALL_DB_CONTAINER=$(docker compose -f deploy/docker-compose.yml ps -q db) scripts/create-clinic.sh ...
set -euo pipefail

if [ $# -lt 3 ]; then
  sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
fi

NAME=$1
EMAIL=$2
PASSWORD=$3
VERTICAL=${4:-CLINIC}
DB=${DATABASE_URL_PSQL:-postgresql://recall:recall@localhost:5432/recall}

# BCrypt hash in the $2a$ form Spring Security reads. The password itself is never stored.
HASH=$(htpasswd -bnBC 10 "" "$PASSWORD" | tr -d ':\n' | sed 's/^\$2y/$2a/')

# psql quotes the values (:'name'), so names with apostrophes cannot break the statement.
if [ -n "${RECALL_DB_CONTAINER:-}" ]; then
  PSQL=(docker exec -i "$RECALL_DB_CONTAINER" psql -U recall recall)
else
  PSQL=(psql "$DB")
fi

"${PSQL[@]}" -v ON_ERROR_STOP=1 -v name="$NAME" -v email="$EMAIL" -v hash="$HASH" -v vertical="$VERTICAL" <<'SQL'
with new_business as (
    insert into business (name, vertical) values (:'name', :'vertical') returning id
)
insert into app_user (business_id, email, password_hash)
select id, :'email', :'hash' from new_business;
SQL
echo "Created \"$NAME\" with login $EMAIL"
