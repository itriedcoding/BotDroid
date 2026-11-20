#!/usr/bin/env bash
# Builds dist/BotDroid.apk without Gradle, using the Android SDK build-tools.
# Requires: JDK 17+ and Android SDK (build-tools 34 + platform android-34).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
BUILD_TOOLS="${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -n 1)}"
PLATFORM_JAR="${PLATFORM_JAR:-$SDK/platforms/android-34/android.jar}"

[ -d "$BUILD_TOOLS" ] || { echo "Android build-tools not found. Set ANDROID_HOME to your SDK." >&2; exit 1; }
[ -f "$PLATFORM_JAR" ] || { echo "android.jar not found at $PLATFORM_JAR" >&2; exit 1; }
command -v javac >/dev/null || { echo "javac not found. Install a JDK." >&2; exit 1; }

OUT="$ROOT/build"
rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$ROOT/dist"

echo "==> aapt2 compile"
"$BUILD_TOOLS/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$OUT/res.zip"

echo "==> aapt2 link"
"$BUILD_TOOLS/aapt2" link \
    -o "$OUT/base.apk" \
    -I "$PLATFORM_JAR" \
    --manifest "$ROOT/app/src/main/AndroidManifest.xml" \
    --java "$OUT/gen" \
    --min-sdk-version 26 \
    --target-sdk-version 34 \
    --version-code "${VERSION_CODE:-6}" \
    --version-name "${VERSION_NAME:-1.5}" \
    "$OUT/res.zip"

echo "==> javac"
find "$ROOT/app/src/main/java" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -source 8 -target 8 -Xlint:-options -classpath "$PLATFORM_JAR" -d "$OUT/classes" @"$OUT/sources.txt"

echo "==> d8"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BUILD_TOOLS/d8" --release --min-api 26 --lib "$PLATFORM_JAR" --output "$OUT/dex" $(cat "$OUT/classes.txt")

echo "==> package"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q -X "$OUT/unsigned.apk" classes.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

KEYSTORE="${KEYSTORE:-$ROOT/keystore/botdroid.jks}"
KEYSTORE_PASS="${KEYSTORE_PASS:-botdroid}"
KEY_ALIAS="${KEY_ALIAS:-botdroid}"
if [ ! -f "$KEYSTORE" ]; then
    echo "==> generating development keystore at keystore/botdroid.jks"
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -keystore "$KEYSTORE" -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 \
        -validity 10000 -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
        -dname "CN=BotDroid, O=BotDroid, C=US" >/dev/null 2>&1
fi

echo "==> sign"
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
    --ks-pass "pass:$KEYSTORE_PASS" --key-pass "pass:$KEYSTORE_PASS" \
    --out "$ROOT/dist/BotDroid.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" verify "$ROOT/dist/BotDroid.apk"

echo "==> done: dist/BotDroid.apk ($(du -h "$ROOT/dist/BotDroid.apk" | cut -f1))"
rm -f "$ROOT/dist/BotDroid.apk.idsig"
