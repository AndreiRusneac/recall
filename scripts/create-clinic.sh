#!/usr/bin/env bash
# Creates a business and one login for it. There is no sign-up page yet, so this is how a
# pilot clinic gets an account.
#
#   scripts/create-clinic.sh "Clinic name" owner@clinic.example "a-strong-password" [CLINIC|SALON|AUTO_SERVICE]
#
# Needs psql and htpasswd (both come with macOS developer tools / Homebrew postgresql).
# The database defaults to the local dev one; set DATABASE_URL_PSQL to target another, for example
#   DATABASE_URL_PSQL="postgresql://user:password@host:5432/recall"
set -euo pipefail

if [ $# -lt 3 ]; then
  sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
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
psql "$DB" -v ON_ERROR_STOP=1 -v name="$NAME" -v email="$EMAIL" -v hash="$HASH" -v vertical="$VERTICAL" <<'SQL'
with new_business as (
    insert into business (name, vertical) values (:'name', :'vertical') returning id
)
insert into app_user (business_id, email, password_hash)
select id, :'email', :'hash' from new_business;
SQL
echo "Created \"$NAME\" with login $EMAIL"
