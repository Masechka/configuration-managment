#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
"$project_dir/scripts/gradle.sh" --quiet installDist
app="$project_dir/build/install/nuget-graph/bin/nuget-graph"
check_error() {
    set +e
    "$app" "$@"
    result=$?
    set -e
    [ "$result" -eq 2 ] || exit 1
}
check_error --package bad/name
check_error --package A --repository ftp://host/index.json
check_error --package A --test-mode yes
check_error --package A --ascii yes
check_error --package A --max-depth -1
check_error --package A --test-mode true --repository /missing/graph.txt
check_error --package A --unknown value
check_error --package A --package B
check_error --package
printf '%s\n' 'Все ошибки параметров обработаны с кодом 2.'
