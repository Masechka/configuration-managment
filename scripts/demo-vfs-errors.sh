#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
./scripts/prepare-examples.sh
printf '%s\n' 'После проверки ошибки закройте окно командой exit.'
./run.sh --vfs build/examples/not-found.zip --startup examples/startup/stage3.txt
printf '%s\n' 'not a ZIP' > build/examples/broken.zip
./run.sh --vfs build/examples/broken.zip --startup examples/startup/stage3.txt
