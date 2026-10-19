#!/bin/sh
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAVA_BIN="$APP_HOME/.env/jdk/jdk17/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN=java
exec "$JAVA_BIN" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
