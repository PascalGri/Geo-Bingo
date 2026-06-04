#!/usr/bin/env bash
# Render all App Store 6.9" preview frames (1320×2868) to out/ as PNGs.
# Uses headless Chrome — no install needed beyond Google Chrome.app.
#
# Usage:  ./render.sh            # render every frame in frames.js
#         ./render.sh 0 2 4      # render only the listed indexes
set -euo pipefail

cd "$(dirname "$0")"

CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
[ -x "$CHROME" ] || { echo "Google Chrome not found at: $CHROME"; exit 1; }

mkdir -p out

# How many frames? Count entries in frames.js (one per "{ t1:" line).
COUNT=$(grep -c '{ t1:' frames.js)

# Which indexes to render: args if given, else 0..COUNT-1.
if [ "$#" -gt 0 ]; then
  INDEXES=("$@")
else
  INDEXES=()
  for ((n=0; n<COUNT; n++)); do INDEXES+=("$n"); done
fi

for i in "${INDEXES[@]}"; do
  num=$(printf '%02d' "$((i+1))")
  out="out/screenshot-${num}.png"
  profile="$(mktemp -d)"
  "$CHROME" \
    --headless=new --disable-gpu --hide-scrollbars \
    --force-device-scale-factor=1 --window-size=1320,2868 \
    --virtual-time-budget=3000 --user-data-dir="$profile" \
    --screenshot="$out" "file://$PWD/frame.html?i=${i}" >/dev/null 2>&1
  rm -rf "$profile"
  echo "rendered  $out"
done

echo "done → $PWD/out/"
