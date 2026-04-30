#!/bin/sh
# Convenience wrapper — builds and installs the Android debug APK.
# Uses Android Studio's embedded JDK to avoid Java 25 compatibility issues.
#
# Usage:
#   ./build.sh            → compile only
#   ./build.sh install    → compile + install on connected device
#   ./build.sh run        → compile + install + launch on device

set -e

JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
GRADLE="$HOME/.gradle/wrapper/dists/gradle-8.11-bin/c4te04g51qsyw1bxcb929u7br/gradle-8.11/bin/gradle"
export JAVA_HOME

PACKAGE="com.sierraespada.wakeywakey.debug"
DEVICE="ZY22KD2V4X"   # Moto G55 5G

case "${1:-build}" in
  install)
    JAVA_HOME=$JAVA_HOME $GRADLE :apps:android:installDebug --no-daemon
    ;;
  run)
    JAVA_HOME=$JAVA_HOME $GRADLE :apps:android:installDebug --no-daemon
    adb -s "$DEVICE" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
    ;;
  *)
    JAVA_HOME=$JAVA_HOME $GRADLE :apps:android:assembleDebug --no-daemon
    ;;
esac
