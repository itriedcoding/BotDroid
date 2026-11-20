#!/usr/bin/env bash
# Compiles the command layer on the JVM with fake Discord/Net fakes and runs the
# command tests. Not shipped in the APK; used to verify handlers before release.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JAVA_HOME="${JAVA_HOME:-$HOME/jdk}"
export PATH="$JAVA_HOME/bin:$PATH"
LIB="${LIB:-/tmp/lib/json.jar}"

if [ ! -f "$LIB" ]; then
    mkdir -p "$(dirname "$LIB")"
    curl -fsSL -o "$LIB" https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar
fi

WORK="${WORK:-/tmp/cmdtest}"
rm -rf "$WORK"
mkdir -p "$WORK/dev/botdroid/app" "$WORK/classes"

SRC="$ROOT/app/src/main/java/dev/botdroid/app"
cp "$SRC/Commands.java" "$SRC/CommandsUtil.java" "$SRC/CommandsMod.java" "$SRC/CommandsFun.java" \
   "$SRC/CommandsRoblox.java" "$SRC/CommandsCustom.java" "$SRC/CustomCommands.java" \
   "$SRC/BotState.java" "$SRC/RobloxApi.java" "$WORK/dev/botdroid/app/"
cp "$ROOT/tools/devtest/harness/"*.java "$WORK/dev/botdroid/app/"
cp "$ROOT/tools/devtest/TestNew.java" "$WORK/dev/botdroid/app/"

javac -cp "$LIB" -d "$WORK/classes" "$WORK"/dev/botdroid/app/*.java
java -cp "$LIB:$WORK/classes" dev.botdroid.app.TestCore
java -cp "$LIB:$WORK/classes" dev.botdroid.app.TestNew
