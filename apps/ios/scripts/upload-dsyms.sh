#!/usr/bin/env bash
# Sube a Sentry los símbolos de depuración (dSYM) del archive de macOS.
#
# Sin esto los crashes de los usuarios llegan sin simbolizar (direcciones en
# vez de nombres de función). Hay que ejecutarlo con CADA archive que se suba
# a App Store Connect, porque los dSYM son distintos en cada build.
#
#   ./scripts/upload-dsyms.sh [ruta-al-.xcarchive]
#
# El auth token NO vive en el repo: está en ~/.sentryclirc (chmod 600), junto
# con la org y el proyecto por defecto. Crear tokens en:
#   https://sentry.io/settings/account/api/auth-tokens/  (scopes: project:releases, org:read)
set -euo pipefail

ARCHIVE="${1:-build/WakeyWakeyMac.xcarchive}"
cd "$(dirname "$0")/.."

if ! command -v sentry-cli >/dev/null 2>&1; then
  echo "error: falta sentry-cli → brew install getsentry/tools/sentry-cli" >&2
  exit 1
fi

if [[ ! -d "$ARCHIVE/dSYMs" ]]; then
  echo "error: no hay dSYMs en $ARCHIVE (¿ruta correcta? ¿archive generado?)" >&2
  exit 1
fi

echo "Subiendo dSYMs de $ARCHIVE …"
sentry-cli debug-files upload --include-sources "$ARCHIVE/dSYMs/"
