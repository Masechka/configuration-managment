#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
./scripts/gradle.sh --quiet installDist
app=./build/install/vfs-shell/bin/vfs-shell
expect_error() {
    if "$app" "$@"; then
        printf '%s\n' 'Ожидалась ошибка параметров' >&2
        exit 1
    fi
}
expect_error --vfs build/examples/minimal.zip --startup
expect_error --vfs
expect_error --unknown x
expect_error --vfs a.zip --vfs b.zip --startup examples/startup/stage2.txt
printf '%s\n' 'Ошибки параметров обработаны. Проверка отсутствующего стартового скрипта:'
./run.sh --vfs build/examples/minimal.zip --startup build/missing-startup.txt
