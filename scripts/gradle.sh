#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
if [ -n "${GRADLE_BIN:-}" ]; then
    exec "$GRADLE_BIN" "$@"
fi
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi
for candidate in "$HOME"/.gradle/wrapper/dists/gradle-8.13-bin/*/gradle-8.13/bin/gradle; do
    if [ -x "$candidate" ]; then
        exec "$candidate" "$@"
    fi
done
printf '%s\n' 'Нужен Gradle 8.13. Установите его или задайте GRADLE_BIN=/путь/bin/gradle.' >&2
exit 1
