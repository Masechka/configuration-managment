#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$project_dir/scripts/gradle.sh" --quiet installDist
exec "$project_dir/build/install/vfs-shell/bin/vfs-shell" "$@"
