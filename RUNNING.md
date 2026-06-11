# Running WakeyWakey Desktop

## Debug mode (dev toolbar visible)

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :apps:windows:run --no-daemon --no-configuration-cache
```

## Normal mode (dev toolbar hidden, como producción)

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :apps:windows:run -Prelease --no-daemon --no-configuration-cache
```

> Para parar la app: `Ctrl+C` en la terminal.

---

## Build DMG macOS para release

Las credenciales OAuth se inyectan automáticamente desde `~/.gradle/gradle.properties`.

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :apps:windows:packageDmg \
  -Prelease \
  -PappVersion=1.0.0 \
  --no-daemon --no-configuration-cache --rerun-tasks
```

El DMG se genera en:
`apps/windows/build/compose/binaries/main/dmg/WakeyWakey-1.0.0.dmg`

### Firma + Notarización + Staple

```bash
DMG="apps/windows/build/compose/binaries/main/dmg/WakeyWakey-1.0.0.dmg"
APP="apps/windows/build/compose/binaries/main/app/WakeyWakey.app"
ENT="apps/windows/macos-entitlements.plist"
ID="C188869D14400C870CCF09FCC54BA6C6B7D587A8"
TMPDIR=$(mktemp -d)

# 1. Firmar .jnilib/.dylib dentro de JARs
find "$APP" -name "*.jar" | while read jar; do
  if unzip -l "$jar" 2>/dev/null | grep -q "\.jnilib\|\.dylib"; then
    unzip -q -o "$jar" "*.jnilib" "*.dylib" -d "$TMPDIR" 2>/dev/null || true
    find "$TMPDIR" -name "*.jnilib" -o -name "*.dylib" | while read lib; do
      codesign --force --timestamp --options runtime --sign "$ID" "$lib" 2>/dev/null
    done
    cd "$TMPDIR"
    find . \( -name "*.jnilib" -o -name "*.dylib" \) | while read lib; do
      zip -u "$jar" "${lib#./}" 2>/dev/null || true
    done
    cd - > /dev/null
    rm -rf "$TMPDIR"/*
  fi
done

# 2. Firmar dylibs sueltos en app/
find "$APP/Contents/app" \( -name "*.dylib" -o -name "*.jnilib" \) | while read f; do
  codesign --force --timestamp --options runtime --sign "$ID" "$f"
done

# 3. Firmar dylibs en runtime/Home/lib
find "$APP/Contents/runtime/Contents/Home/lib" \( -name "*.dylib" -o -name "*.jnilib" \) | while read f; do
  codesign --force --timestamp --options runtime --sign "$ID" "$f"
done

# 4. Firmar jspawnhelper
find "$APP" -name "jspawnhelper" | while read f; do
  codesign --force --timestamp --options runtime --sign "$ID" "$f"
done

# 5. Firmar helpers en MacOS/ (CalendarHelper, etc.)
MAIN="$APP/Contents/MacOS/WakeyWakey"
find "$APP/Contents/MacOS" -type f | while read f; do
  [ "$f" = "$MAIN" ] && continue
  codesign --force --timestamp --options runtime --sign "$ID" "$f"
done

# 6. Sellar runtime bundle
codesign --force --timestamp --options runtime --sign "$ID" "$APP/Contents/runtime"

# 7. Firmar binario principal con entitlements
codesign --force --timestamp --options runtime --entitlements "$ENT" --sign "$ID" "$MAIN"

# 8. Sellar .app con entitlements
codesign --force --timestamp --options runtime --entitlements "$ENT" --sign "$ID" "$APP"

# 9. Verificar
codesign --verify --deep --strict "$APP" && echo "VALID ✓"

# 10. Regenerar DMG profesional y firmarlo
#     (fondo amarillo + symlink a Applications + diseño Retina)
ASSETS="apps/windows/dmg-assets"
STAGE=$(mktemp -d)
cp -R "$APP" "$STAGE/"
rm -f "$DMG"

create-dmg \
  --volname "WakeyWakey" \
  --background "$ASSETS/background.tiff" \
  --window-pos 200 120 \
  --window-size 540 380 \
  --icon-size 96 \
  --text-size 12 \
  --icon "WakeyWakey.app" 140 195 \
  --hide-extension "WakeyWakey.app" \
  --app-drop-link 400 195 \
  --no-internet-enable \
  "$DMG" \
  "$STAGE"

rm -rf "$STAGE"
codesign --force --timestamp --sign "$ID" "$DMG"

# 11. Notarizar (credenciales en keychain, perfil "notarytool-wakeywakey")
xcrun notarytool submit "$DMG" --keychain-profile "notarytool-wakeywakey" --wait

# 12. Staple
xcrun stapler staple "$DMG"

# 13. Verificar notarización
spctl --assess --type open --context context:primary-signature -v "$DMG"

rm -rf "$TMPDIR"
```

### Subir al GitHub Release

```bash
# Eliminar DMG antiguo y subir el nuevo
ASSET_ID=$(gh release view v1.0.0 --json assets --jq '.assets[] | select(.name == "WakeyWakey.dmg") | .apiUrl' | sed 's/.*assets\///')
[ -n "$ASSET_ID" ] && gh api --method DELETE /repos/RomanFdez/wakeywakey/releases/assets/$ASSET_ID
cp "$DMG" /tmp/WakeyWakey.dmg
gh release upload v1.0.0 /tmp/WakeyWakey.dmg --clobber
```
