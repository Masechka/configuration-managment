#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
mkdir -p 'build/examples/paths with spaces'
cp examples/startup/stage2.txt 'build/examples/paths with spaces/start script.txt'
./run.sh --vfs 'build/examples/paths with spaces/my vfs.zip' \
    --startup 'build/examples/paths with spaces/start script.txt'
