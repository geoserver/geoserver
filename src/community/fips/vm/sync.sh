#!/bin/bash
# Copies the GeoServer sources of this checkout into the machine, at ~/gs/src.
#
#   ./sync.sh            send what changed
#   ./sync.sh --fresh    delete the copy in the machine first, for a clean build
#
# The sources are copied rather than shared: a build writes thousands of small files into target/,
# which is what a shared filesystem is worst at, and two builds from one checkout would write the
# same paths at once. rsync sends only what changed, so a second run costs a couple of seconds.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

# target/ is left out of the transfer, so rsync cannot delete a directory that still holds one. After
# a rename in the source tree that leaves the old module behind, and the machine builds both.
[ "${1:-}" = --fresh ] && vm 'rm -rf ~/gs/src'

# rsync creates the last directory of the destination only, so the machine gets ~/gs first
vm 'mkdir -p ~/gs/src'

rsync -a --delete --info=stats1 --exclude 'target/' --exclude '.git/' \
  -e "ssh $SSH_OPTS" "$SRC_DIR/" "$USER@localhost:gs/src/"

# the war packaging reads data/release, which sits next to src rather than inside it
exec rsync -a --delete --info=stats1 \
  -e "ssh $SSH_OPTS" "$SRC_DIR/../data/" "$USER@localhost:gs/data/"
