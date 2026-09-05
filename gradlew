#!/bin/sh
set -eu

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROPS="$BASE_DIR/gradle/wrapper/gradle-wrapper.properties"
DIST_URL="$(sed -n 's/^distributionUrl=//p' "$PROPS" | sed 's/\\:/:/g')"
GRADLE_VERSION="$(printf '%s' "$DIST_URL" | sed -n 's/.*gradle-\([0-9.]*\)-bin\.zip/\1/p')"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INSTALL_DIR="$GRADLE_HOME/jarvis-wrapper/gradle-$GRADLE_VERSION"
ZIP="$GRADLE_HOME/jarvis-wrapper/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$INSTALL_DIR/bin/gradle" ]; then
  mkdir -p "$GRADLE_HOME/jarvis-wrapper"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 "$DIST_URL" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP" "$DIST_URL"
    else
      echo "Error: curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  rm -rf "$INSTALL_DIR.tmp"
  mkdir -p "$INSTALL_DIR.tmp"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP" -d "$INSTALL_DIR.tmp"
  else
    echo "Error: unzip is required to extract Gradle." >&2
    exit 1
  fi
  mv "$INSTALL_DIR.tmp/gradle-$GRADLE_VERSION" "$INSTALL_DIR"
  rm -rf "$INSTALL_DIR.tmp"
fi

exec "$INSTALL_DIR/bin/gradle" "$@"
