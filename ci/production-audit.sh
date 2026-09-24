#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail=0
check_forbidden() {
  local pattern="$1" label="$2"
  if grep -RInE --exclude-dir=build --exclude-dir=.git --exclude='*.md' "$pattern" app/src/main backend 2>/dev/null; then
    echo "ERROR: $label" >&2
    fail=1
  fi
}

check_forbidden 'example\.invalid' 'endpoint de simulation détecté'
check_forbidden 'default_live' 'identifiant Live factice détecté'
check_forbidden 'openrelay\.metered\.ca' 'fallback TURN public interdit en production'
check_forbidden 'images\.unsplash\.com' 'identité/avatar de démonstration détecté dans le code applicatif'
check_forbidden 'Kevine Moundele|Glodi Mavoungou|Grâce Kamba|Chancel Mbemba' 'profil utilisateur fictif détecté'
check_forbidden 'Michel Loutala|Aïcha Diallo|Cedric Moukoko|Spam Bot 242|Grace Makiese|Aron Loutala' 'donnée utilisateur fictive détectée'
check_forbidden 'MBOTE-ADMIN-2026|admin@loukatech\.com' 'identifiant administrateur intégré au client'

if grep -RInE 'AIza[0-9A-Za-z_-]{20,}|xkeysib-[0-9A-Za-z_-]{20,}|sk-[0-9A-Za-z_-]{20,}' app/src/main backend --exclude-dir=build 2>/dev/null; then
  echo "ERROR: clé API potentiellement intégrée au dépôt" >&2
  fail=1
fi

grep -q 'https://mbote-backend.onrender.com/v1' app/build.gradle.kts
grep -q 'MBOTE_TURN_URL' backend/server.js
grep -q 'MBOTE_TURN_CREDENTIAL' backend/server.js
if grep -q 'MBOTE_TURN_CREDENTIAL' app/build.gradle.kts; then
  echo "ERROR: les identifiants TURN ne doivent pas être intégrés dans BuildConfig Android" >&2
  fail=1
fi

if [ "$fail" -ne 0 ]; then exit 1; fi
echo "Production source audit: OK"
