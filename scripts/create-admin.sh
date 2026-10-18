#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
    printf '%s\n' "Usage: scripts/create-admin.sh <email> <full-name>" >&2
    exit 2
fi

email="$1"
full_name="$2"
printf 'Admin password (minimum 12 characters): ' >&2
stty -echo
IFS= read -r password
stty echo
printf '\nRepeat password: ' >&2
stty -echo
IFS= read -r password_confirmation
stty echo
printf '\n' >&2

if [ "$password" != "$password_confirmation" ]; then
    printf '%s\n' "The passwords do not match." >&2
    exit 1
fi

if [ "${#password}" -lt 12 ]; then
    printf '%s\n' "The password must contain at least 12 characters." >&2
    exit 1
fi

password_hash="$(printf '%s' "$password" | python3 "$(dirname "$0")/hash-password.py")"
unset password password_confirmation

docker compose exec -T postgres psql \
    --username=amh_app \
    --dbname=agent_memory_hub \
    --set=ON_ERROR_STOP=1 \
    --set="email=$email" \
    --set="full_name=$full_name" \
    --set="password_hash=$password_hash" <<'SQL'
INSERT INTO users (id, full_name, email, password_hash, platform_role, enabled)
VALUES (gen_random_uuid()::text, :'full_name', lower(trim(:'email')), :'password_hash', 'ADMIN', TRUE)
ON CONFLICT (email) DO NOTHING;
SQL

unset password_hash
