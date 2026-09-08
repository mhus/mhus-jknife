#!/usr/bin/env bash
#
# Sets the project version in all pom.xml files and keeps the VERSION
# constants in the *Cmd.java main classes in sync.
#
# Used by scripts/release.sh and by the release workflow.
#
set -euo pipefail

VERSION="${1:?Usage: set-version.sh <version>}"

command -v mvn >/dev/null || { echo "ERROR: mvn not found" >&2; exit 1; }

mvn -B versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false

# keep 'public static final String VERSION = "...";' in all tool main classes in sync
find . -path '*/src/main/*' -name '*Cmd.java' -exec \
    perl -pi -e "s/VERSION = \"[^\"]+\"/VERSION = \"$VERSION\"/" {} +

echo "Version is now $VERSION"
