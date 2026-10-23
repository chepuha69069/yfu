#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

python3 scripts/setup_mc.py

CP="build/mc/intermediary-client.jar"
for j in build/mc/libs/*.jar; do CP="$CP:$j"; done

rm -rf build/stubs build/classes
mkdir -p build/stubs build/classes

# заглушки Fabric API - только для компиляции, в jar не попадают
javac -encoding UTF-8 -cp "$CP" -d build/stubs $(find stubs -name '*.java')
javac -encoding UTF-8 --release 21 -proc:none -nowarn -cp "$CP:build/stubs" -d build/classes $(find src -name '*.java')

jar --create --file build/applefarm-1.0.0.jar -C build/classes . -C resources .
echo "Готово: build/applefarm-1.0.0.jar"
