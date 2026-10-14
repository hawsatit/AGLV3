#!/bin/bash
# Builds dist/AGLV3-windows.zip: a portable Windows package with its own Java runtime.
# Unzip it on any 64-bit Windows PC and double-click "Start AGLV3.bat" -- no Java install needed.
#
# Needs a JDK 21+ to compile (set JAVA_HOME, or have javac on the PATH) and an internet connection
# the first time (to download the Windows Java runtime, cached in dist/cache afterwards).
set -euo pipefail
cd "$(dirname "$0")"

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
DIST=dist
APP=$DIST/AGLV3
CACHE=$DIST/cache
JRE_ZIP=$CACHE/jre21-windows-x64.zip

rm -rf "$APP" "$DIST/build" "$DIST/AGLV3-windows.zip"
mkdir -p "$APP" "$DIST/build" "$CACHE"

echo "Compiling..."
"${JAVA_BIN}javac" --release 21 -d "$DIST/build" src/main/java/edu/grinnell/psychMAP26/AGLV3/*.java
"${JAVA_BIN}jar" --create --file "$APP/AGLV3.jar" \
    --main-class edu.grinnell.psychMAP26.AGLV3.AGLV3 -C "$DIST/build" .

if [ ! -f "$JRE_ZIP" ]; then
    echo "Downloading Windows Java runtime (Eclipse Temurin 21 JRE)..."
    curl -fSL -o "$JRE_ZIP" "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"
fi
echo "Adding Java runtime..."
rm -rf "$CACHE/jre-unpacked"
mkdir -p "$CACHE/jre-unpacked"
unzip -q "$JRE_ZIP" -d "$CACHE/jre-unpacked"
mv "$CACHE"/jre-unpacked/* "$APP/runtime"

cp -R img img2 "$APP/"
find "$APP" \( -name .DS_Store -o -name Thumbs.db \) -delete

# Windows line endings so cmd.exe and Notepad read them properly
sed 's/$/\r/' > "$APP/Start AGLV3.bat" <<'EOF'
@echo off
title AGLV3
cd /d "%~dp0"
"runtime\bin\java.exe" -jar AGLV3.jar
echo.
pause
EOF

sed 's/$/\r/' > "$APP/Combine results.bat" <<'EOF'
@echo off
title AGLV3 - combine results
cd /d "%~dp0"
"runtime\bin\java.exe" -cp AGLV3.jar edu.grinnell.psychMAP26.AGLV3.ResultsCombiner
echo.
pause
EOF

sed 's/$/\r/' > "$APP/HOW TO RUN.txt" <<'EOF'
AGLV3 - how to run
==================

1. Double-click "Start AGLV3.bat".
   (If Windows shows "Windows protected your PC", click "More info" -> "Run anyway".)
2. Answer the setup questions in the black window and type y to confirm.
   Type only the participant number (1-999, e.g. 5), the SAME on Day 1 and Day 2.
   The program builds the full 8-digit ID for each block and shows it on the
   summary screen (check it against the saliva tube labels):
     digit 1 = day, 2 = block, 3 = condition (0 Control / 1 SECPT),
     4 = rate (0 Fast / 1 Slow), 5 = image bank (1/2), 6-8 = participant
     e.g. 11101005 = Day 1, Block 1, SECPT, Fast, bank 1, participant 5
3. Results are saved in the "results" folder next to this file:
   one row per block with the full ID and the score (number correct out of 24).
   Day 2 must be run from the SAME folder as Day 1 (it reads the Day 1 setup from "results").
4. "results\all_results.csv" has every block score in one spreadsheet. It is rebuilt
   at the end of each session from the per-day files, which are never changed.
   - Close it in Excel before a session ends, or it can't be updated (the session's
     own file is still saved; just run "Combine results.bat" later).
   - To merge data from other computers: copy their participant_..._day... files into
     this "results" folder, then double-click "Combine results.bat".

Keep this whole folder together. Do not move "Start AGLV3.bat" out of it.

Counterbalance codes (Block 1 -> Block 2):
  1 = Group 1 Fast -> Group 2 Slow
  2 = Group 1 Slow -> Group 2 Fast
  3 = Group 2 Fast -> Group 1 Slow
  4 = Group 2 Slow -> Group 1 Fast

During a session:
  - Between blocks, the experimenter types next and presses Enter.
  - Typing quit and pressing Enter aborts; finished blocks stay saved.
EOF

(cd "$DIST" && zip -qr AGLV3-windows.zip AGLV3)
rm -rf "$DIST/build" "$CACHE/jre-unpacked"
echo "Done: $DIST/AGLV3-windows.zip ($(du -h "$DIST/AGLV3-windows.zip" | cut -f1))"
