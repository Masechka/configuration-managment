#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
./scripts/prepare-examples.sh
mkdir -p 'build/examples/paths with spaces'
cp examples/startup/stage2.txt 'build/examples/paths with spaces/start script.txt'
cp build/examples/several.zip 'build/examples/paths with spaces/my vfs.zip'
./run.sh --vfs 'build/examples/paths with spaces/my vfs.zip' \
    --startup 'build/examples/paths with spaces/start script.txt'
