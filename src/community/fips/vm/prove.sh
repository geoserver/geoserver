#!/bin/bash
# Shows that a change is really needed on a FIPS system: reverts one commit, runs the tests that
# motivated it inside the machine, and reports the result. The tests must fail without the commit.
# The checkout is restored either way.
#
#   ./prove.sh <commit> <module> '<test pattern>'
#   ./prove.sh 1a2b3c4 :gs-main 'GeoServerPBEPasswordEncoderTest'
set -eo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SHA=$1
MODULE=$2
TESTS=$3
[ -n "$TESTS" ] || { sed -n '2,7p' "${BASH_SOURCE[0]}"; exit 1; }

cd "$SRC_DIR"
if ! git revert --no-commit --no-edit "$SHA" 2>/dev/null; then
    git revert --abort 2>/dev/null || true
    git reset -q --hard HEAD
    echo "SKIPPED $SHA: it does not revert cleanly"
    exit 0
fi
trap 'git -C "$SRC_DIR" reset -q --hard HEAD; "$HERE/sync.sh" > /dev/null || true' EXIT

"$HERE/sync.sh" > /dev/null
LOG=~/prove-$(echo "$SHA" | cut -c1-7).log
# failIfNoSpecifiedTests keeps the modules that hold none of the named tests from failing the build
$SSH "cd ~/gs/src && mvnd -nsu -fae -Pfips -pl $MODULE -am install \
  -Dtest='$TESTS' -Dsurefire.failIfNoSpecifiedTests=false > $LOG 2>&1" || true
echo "REVERTED $SHA ($MODULE / $TESTS):"
$SSH "grep -E 'Tests run: .*Skipped: [0-9]+\$' $LOG | tail -1" || true
