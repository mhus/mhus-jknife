#!/usr/bin/env bash
#
# Performs a release of mhus-jknife:
#
#   1. checks preconditions (branch, clean & synced worktree)
#   2. runs tests (mvn verify)
#   3. sets the version in all poms and *Cmd.java VERSION constants
#   4. commits, tags vX.Y.Z and pushes to origin
#   5. the GitHub release workflow then builds the native binaries
#      and attaches them to the GitHub release
#
# Afterwards run scripts/gen-formula.sh to create the homebrew formula.
#
# See readme/release.md for the full process description.
#
set -euo pipefail

VERSION=""
ASSUME_YES=false
for arg in "$@"; do
    case "$arg" in
    --yes | -y)
        ASSUME_YES=true
        ;;
    -h | --help)
        echo "Usage: ./scripts/release.sh <version> [--yes]"
        echo "  e.g. ./scripts/release.sh 0.1.0"
        exit 0
        ;;
    -*)
        echo "Unknown option: $arg" >&2
        exit 1
        ;;
    *)
        [[ -z "$VERSION" ]] || { echo "Only one version argument allowed" >&2; exit 1; }
        VERSION="$arg"
        ;;
    esac
done

[[ -n "$VERSION" ]] || { echo "Usage: ./scripts/release.sh <version> [--yes]" >&2; exit 1; }

[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$ ]] || {
    echo "ERROR: '$VERSION' is not a valid version (expected e.g. 1.2.3 or 1.2.3-rc1)" >&2
    exit 1
}

TAG="v$VERSION"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "### Releasing $TAG"

# --- preconditions ---------------------------------------------------------

command -v git >/dev/null || { echo "ERROR: git not found" >&2; exit 1; }
command -v mvn >/dev/null || { echo "ERROR: mvn not found" >&2; exit 1; }

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
[[ "$BRANCH" == "main" ]] || { echo "ERROR: not on branch 'main' (currently on '$BRANCH')" >&2; exit 1; }

[[ -z "$(git status --porcelain)" ]] || {
    echo "ERROR: working tree is not clean:" >&2
    git status --short >&2
    exit 1
}

git fetch origin main
if ! git diff --quiet main origin/main; then
    echo "ERROR: local 'main' and 'origin/main' differ, please sync first" >&2
    exit 1
fi

if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
    echo "ERROR: tag $TAG already exists" >&2
    exit 1
fi

# --- tests -----------------------------------------------------------------

echo "### Running tests (mvn verify)"
mvn -B verify

# --- set version, commit, tag ----------------------------------------------

echo "### Setting version $VERSION"
"$SCRIPT_DIR/set-version.sh" "$VERSION"

git add -A
git commit -m "release: $TAG"

# --- confirm & push --------------------------------------------------------

if [[ "$ASSUME_YES" != true ]]; then
    read -r -p "Push commit and tag $TAG to origin and trigger the release build? [y/N] " answer
    [[ "$answer" == "y" || "$answer" == "Y" ]] || {
        echo "Aborted. To undo locally: git reset --hard HEAD~1 && git tag -d $TAG"
        exit 1
    }
fi

git push origin main
git push origin "$TAG"

echo ""
echo "### Done. Release build is running:"
echo "    https://github.com/mhus/mhus-jknife/actions/workflows/release.yml"
echo "    (or watch it with: gh run watch)"
echo ""
echo "### Next steps:"
echo "    1. wait for the workflow to finish and the GitHub release to be created"
echo "    2. ./scripts/gen-formula.sh $VERSION"
echo "    3. copy the formula from dist/ into the tap repo (see readme/release.md)"
