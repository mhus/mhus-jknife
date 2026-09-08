#!/usr/bin/env bash
#
# Local build helper for mhus-jknife.
#
# Usage: ./scripts/build.sh [options]
#
#   (no options)      clean build + all tests
#   --native          additionally build the native binaries for the
#                     current platform (requires GraalVM native-image)
#   --fast            skip the tests
#   --clean           run 'mvn clean' first (default: incremental)
#   -h | --help       show this help
#
# Examples:
#   ./scripts/build.sh                    # quick check: compile + tests
#   ./scripts/build.sh --clean --native    # full build with native binaries
#
set -euo pipefail

CLEAN=false
NATIVE=false
SKIP_TESTS=false
while [[ $# -gt 0 ]]; do
    case "$1" in
    --clean)
        CLEAN=true
        ;;
    --native)
        NATIVE=true
        ;;
    --fast | --skip-tests)
        SKIP_TESTS=true
        ;;
    -h | --help)
        grep -E '^#( Usage:|   )' "$0" | sed 's/^# \{0,2\}//'
        exit 0
        ;;
    *)
        echo "ERROR: unknown option: $1 (see --help)" >&2
        exit 1
        ;;
    esac
    shift
done

command -v mvn >/dev/null || { echo "ERROR: mvn not found" >&2; exit 1; }

MVN_ARGS=(-B)
if [[ "$CLEAN" == true ]]; then
    MVN_ARGS+=("clean")
fi
if [[ "$SKIP_TESTS" == true ]]; then
    MVN_ARGS+=("-DskipTests")
fi

echo "### Build + tests (mvn ${MVN_ARGS[*]} verify)"
mvn "${MVN_ARGS[@]}" verify

echo "### Build OK"

# --- native -----------------------------------------------------------------

if [[ "$NATIVE" == true ]]; then
    command -v native-image >/dev/null || {
        echo "ERROR: native-image not found" >&2
        echo "       install GraalVM, e.g.: sdk install java 21.0.5-graal" >&2
        exit 1
    }

    echo ""
    echo "### Native build (mvn -Pnative package, tests already run above)"
    mvn -B -Pnative -DskipTests package

    echo ""
    echo "### Native binaries:"
    find . -mindepth 2 -maxdepth 2 -name pom.xml -exec dirname {} \; | sed 's|^\./||' | sort | while read -r dir; do
        for bin in "$dir"/target/"$dir"-*; do
            [[ -f "$bin" && -x "$bin" ]] || continue
            size="$(du -h "$bin" | cut -f1)"
            echo "    $bin ($size)"
        done
    done
fi
