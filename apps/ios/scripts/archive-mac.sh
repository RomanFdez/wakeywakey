#!/usr/bin/env bash
# Genera el archive de la app macOS listo para App Store Connect:
#   1. archive firmado (Release, bundle id de tienda)
#   2. copia los dSYM de los frameworks precompilados (SPM no los mete en el archive)
#   3. sube todos los símbolos a Sentry
#
#   ./scripts/archive-mac.sh
#
# Sobre el paso 2: Sentry se enlaza con el producto `Sentry-Dynamic` porque es el
# único que publica dSYMs. Además el target fija STRIP_INSTALLED_PRODUCT=NO: al
# hacer strip, el binario del framework se reescribe y cambian sus UUID, con lo
# que ningún dSYM coincide y App Store Connect avisa de símbolos ausentes.
set -euo pipefail

cd "$(dirname "$0")/.."
ARCHIVE="build/WakeyWakeyMac.xcarchive"
TEAM="S3SQHNG68C"

echo "▸ Generando proyecto…"
xcodegen generate >/dev/null

# Los xcframeworks precompilados traen una slice arm64e que App Store Connect
# rechaza ("The osx 26 SDK or later is required for an arm64e slice"). Se quita
# aquí, antes de archivar, para que Xcode embeba y firme ya el binario correcto.
# `lipo -remove` copia el resto de slices tal cual → los UUID no cambian y los
# dSYM siguen coincidiendo.
DERIVED_ROOT=$(xcodebuild -project WakeyWakey.xcodeproj -scheme WakeyWakeyMac -showBuildSettings 2>/dev/null \
               | awk '/ BUILD_DIR /{print $3}' | head -1)
ARTIFACTS="${DERIVED_ROOT%/Build/*}/SourcePackages/artifacts"
while IFS= read -r fw; do
  bin="$fw/Versions/A/$(basename "$fw" .framework)"
  [[ -f "$bin" ]] || continue
  if lipo -archs "$bin" 2>/dev/null | grep -qw arm64e; then
    echo "   − arm64e de $(basename "$fw")"
    lipo "$bin" -remove arm64e -output "$bin.thin" && mv "$bin.thin" "$bin"
  fi
done < <(find "$ARTIFACTS" -maxdepth 4 -path "*macos*" -name "*.framework" -type d 2>/dev/null)

echo "▸ Archivando…"
rm -rf "$ARCHIVE"
xcodebuild -project WakeyWakey.xcodeproj -scheme WakeyWakeyMac -configuration Release \
  -destination 'generic/platform=macOS' -archivePath "$ARCHIVE" archive \
  -allowProvisioningUpdates DEVELOPMENT_TEAM="$TEAM" \
  | grep -E "ARCHIVE SUCCEEDED|ARCHIVE FAILED|error:" || true

[[ -d "$ARCHIVE" ]] || { echo "error: no se generó el archive" >&2; exit 1; }

echo "▸ Copiando dSYMs de frameworks precompilados…"
DERIVED=$(xcodebuild -project WakeyWakey.xcodeproj -scheme WakeyWakeyMac -showBuildSettings 2>/dev/null \
          | awk '/ BUILD_DIR /{print $3}' | head -1)
PKG_ARTIFACTS="${DERIVED%/Build/*}/SourcePackages/artifacts"
while IFS= read -r dsym; do
  cp -R "$dsym" "$ARCHIVE/dSYMs/"
  echo "   + $(basename "$dsym")"
done < <(find "$PKG_ARTIFACTS" -path "*macos*/dSYMs/*.dSYM" -maxdepth 6 -type d 2>/dev/null)

echo "▸ Comprobando que cada binario embebido tiene su dSYM…"
MISSING=0
while IFS= read -r bin; do
  while read -r uuid; do
    if ! dwarfdump --uuid "$ARCHIVE"/dSYMs/*.dSYM 2>/dev/null | grep -q "$uuid"; then
      echo "   ✗ sin dSYM: $uuid ($(basename "$bin"))" >&2
      MISSING=1
    fi
  done < <(dwarfdump --uuid "$bin" 2>/dev/null | awk '{print $2}')
done < <(find "$ARCHIVE/Products/Applications/WakeyWakeyMac.app" -type f -perm -u+x \
         \( -path "*/Frameworks/*" -o -path "*/MacOS/*" \) 2>/dev/null)
[[ $MISSING -eq 0 ]] && echo "   todos los símbolos presentes"

echo "▸ Subiendo símbolos a Sentry…"
./scripts/upload-dsyms.sh "$ARCHIVE"

echo
echo "Listo. Abre el archive y súbelo:  open $ARCHIVE"
