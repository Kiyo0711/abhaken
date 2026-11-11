#!/usr/bin/env bash
# Baut die Android-App ohne Gradle, nur mit den Werkzeugen des Android SDK.
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -d "$SDK" ] || { echo "Android SDK nicht gefunden (ANDROID_HOME setzen)"; exit 1; }
BT="$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)"
PLATFORM_JAR="$(ls "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
VERSION_CODE="${VERSION_CODE:-1}"
echo "Build-Tools: $BT"
echo "Plattform:   $PLATFORM_JAR"
echo "Version:     1.$VERSION_CODE"

rm -rf build
mkdir -p build/gen build/classes build/dex

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/unsigned.apk -I "$PLATFORM_JAR" \
  --manifest AndroidManifest.xml --java build/gen \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "1.$VERSION_CODE" \
  build/res.zip

javac -source 8 -target 8 -Xlint:-options -encoding UTF-8 \
  -classpath "$PLATFORM_JAR" -d build/classes \
  $(find src build/gen -name '*.java')

"$BT/d8" --release --lib "$PLATFORM_JAR" --min-api 26 --output build/dex \
  $(find build/classes -name '*.class')

(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks abhaken.keystore --ks-key-alias abhaken \
  --ks-pass pass:abhaken --key-pass pass:abhaken \
  --out build/abhaken.apk build/aligned.apk
"$BT/apksigner" verify --print-certs build/abhaken.apk | head -3
ls -la build/abhaken.apk
