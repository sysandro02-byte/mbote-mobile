#!/usr/bin/env bash
set -euo pipefail

API_ORIGIN="${API_ORIGIN:-https://mbote-backend.onrender.com}"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

code="$(curl -sS -o "$tmp" -w '%{http_code}' --connect-timeout 15 --max-time 45 "$API_ORIGIN/v1/health")"
test "$code" = "200"
grep -q '"status":"online"' "$tmp"
curl -sSI "$API_ORIGIN/v1/health" | tr -d '\r' | grep -qi '^x-content-type-options: nosniff'

protected=(
  /v1/chats
  /v1/publications
  /v1/short-videos
  /v1/masta/users
  /v1/masta/requests
  /v1/users/me/settings
  /v1/calls/history
  /v1/meetings
  /v1/statuses
  /v1/gifts/catalog
  /v1/live
  /v1/jobs
  /v1/channels
)
for path in "${protected[@]}"; do
  code="$(curl -sS -o "$tmp" -w '%{http_code}' --connect-timeout 15 --max-time 45 "$API_ORIGIN$path")"
  if [ "$code" != "401" ]; then
    echo "ERROR: $path expected 401 without token, got $code" >&2
    cat "$tmp" >&2
    exit 1
  fi
done

public_code="$(curl -sS -o "$tmp" -w '%{http_code}' --connect-timeout 15 --max-time 45 "$API_ORIGIN/v1/public-settings")"
test "$public_code" = "200"
echo "Production smoke contract: OK"
