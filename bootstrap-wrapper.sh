#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BOOTSTRAP_DIR="$ROOT/.gradle-bootstrap"
DIST_NAME="gradle-4.10.3-bin.zip"
DIST_URL="https://services.gradle.org/distributions/${DIST_NAME}"
DIST_ZIP="$BOOTSTRAP_DIR/$DIST_NAME"
GRADLE_HOME="$BOOTSTRAP_DIR/gradle-4.10.3"

if [[ -f "$ROOT/gradlew" && -f "$ROOT/gradle/wrapper/gradle-wrapper.jar" ]]; then
  echo "Gradle wrapper already exists."
  exit 0
fi

mkdir -p "$BOOTSTRAP_DIR"

if [[ ! -f "$DIST_ZIP" ]]; then
  echo "Downloading Gradle 4.10.3..."
  curl -fL "$DIST_URL" -o "$DIST_ZIP"
fi

if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
  echo "Extracting Gradle 4.10.3..."
  unzip -oq "$DIST_ZIP" -d "$BOOTSTRAP_DIR"
fi

echo "Generating Forge-compatible Gradle wrapper..."
"$GRADLE_HOME/bin/gradle" wrapper --gradle-version 4.10.3 --distribution-type all --no-validate-url

echo "Wrapper generated. You can now run ./gradlew build"
