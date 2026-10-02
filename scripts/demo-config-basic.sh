#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
./run.sh --vfs build/examples/minimal.zip --startup examples/startup/stage2.txt
