#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
./scripts/gradle.sh --quiet installDist
java -cp 'build/install/vfs-shell/lib/*' shell.ExampleArchivesKt build/examples
