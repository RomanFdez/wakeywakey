#!/bin/sh
# Gradle wrapper script
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

exec java -classpath "$WRAPPER_JAR" \
  -Dgradle.user.home="$GRADLE_USER_HOME" \
  org.gradle.wrapper.GradleWrapperMain "$@"
