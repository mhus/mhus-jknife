#!/usr/bin/env bash
#
# Local build helper for jknife.
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

fail() {
    echo "ERROR: smoke test failed: $*" >&2
    exit 1
}

# basic functional check per tool, called with the binary path and the module name
smoke_test() {
    local bin="$1" tool="$2"
    "$bin" --version >/dev/null 2>&1 || fail "$tool --version"
    case "$tool" in
    jregex)
        echo 'a 12 b 345 c' | "$bin" find '\d+' | grep -qx 12 || fail "$tool find"
        "$bin" match -q '^a+c$' aaac || fail "$tool match"
        ;;
    jbase64)
        echo 'hello world' | "$bin" encode | "$bin" decode | grep -qx 'hello world' || fail "$tool encode/decode"
        ;;
    juuid)
        "$bin" gen | grep -qE '^[0-9a-f-]{36}$' || fail "$tool gen"
        "$bin" parse "$($bin gen -t 7)" | grep -qx 'version: 7' || fail "$tool parse"
        ;;
    jtime)
        "$bin" parse 1725787000 | grep -qx 'epoch: 1725787000' || fail "$tool parse"
        "$bin" now --epoch | grep -qE '^[0-9]{10}$' || fail "$tool now"
        ;;
    jperiod)
        "$bin" parse -u m 2h30m | grep -qx 150 || fail "$tool parse"
        "$bin" add -u h 2h30m 45m | grep -qx 3.25 || fail "$tool add"
        ;;
    jjson)
        "$bin" validate '{"a":1}' || fail "$tool validate"
        "$bin" get a.b[1] '{"a":{"b":[10,20]}}' | grep -qx 20 || fail "$tool get"
        ;;
    jyaml)
        "$bin" tojson 'a: 1' | grep -qx '{"a":1}' || fail "$tool tojson"
        ;;
    jxpath)
        "$bin" select 'count(//b)' '<a><b/><b/></a>' | grep -qx 2 || fail "$tool select"
        "$bin" exists -q '//b' '<a><b/></a>' || fail "$tool exists"
        ;;
    jllm)
        "$bin" --version >/dev/null || fail "$tool --version"
        if "$bin" request 'hi' >/dev/null 2>&1; then fail "$tool request should fail without config"; fi
        ;;
    jsec)
        "$bin" --version >/dev/null || fail "$tool --version"
        "$bin" hash x | grep -qx 2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881 || fail "$tool hash"
        local key="$("$bin" create secret -s 256)"
        echo hi | "$bin" encrypt --key "$key" | "$bin" decrypt --key "$key" | grep -qx hi || fail "$tool encrypt/decrypt"
        ;;
    esac
    echo "    $tool OK"
}

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
    echo "### Native build incl. native tests (mvn -Pnative package)"
    mvn -B -Pnative package

    echo ""
    echo "### Native binaries:"
    find . -mindepth 2 -maxdepth 2 -name pom.xml -exec dirname {} \; | sed 's|^\./||' | sort | while read -r dir; do
        for bin in "$dir"/target/"$dir"-*; do
            [[ -f "$bin" && -x "$bin" ]] || continue
            size="$(du -h "$bin" | cut -f1)"
            echo "    $bin ($size)"
        done
    done

    echo ""
    echo "### Smoke tests:"
    find . -mindepth 2 -maxdepth 2 -name pom.xml -exec dirname {} \; | sed 's|^\./||' | sort | while read -r dir; do
        for bin in "$dir"/target/"$dir"-*; do
            [[ -f "$bin" && -x "$bin" ]] || continue
            smoke_test "$bin" "$dir"
        done
    done
fi
