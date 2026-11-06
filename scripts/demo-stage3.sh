#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
"$project_dir/scripts/gradle.sh" --quiet installDist
app="$project_dir/build/install/nuget-graph/bin/nuget-graph"
for graph in leaf chain diamond cycle self-cycle disconnected; do
    printf '\n%s\n' "=== $graph ==="
    "$app" --package A --test-mode true --repository "$project_dir/examples/$graph.txt" \
        --max-depth 10 --ascii true
done
for depth in 0 1 2; do
    printf '\n%s\n' "=== max-depth=$depth ==="
    "$app" --package A --test-mode true --repository "$project_dir/examples/chain.txt" \
        --max-depth "$depth" --ascii true
done
