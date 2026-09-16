#!/bin/bash
# Syncs the sources and builds them inside the machine, under FIPS.
#
#   ./build.sh                              the whole build with tests: -Prelease install
#   ./build.sh -pl :gs-main -am install     one module and what it needs
#
# Anything passed replaces the default arguments; -Pfips is always added. Two rules worth knowing:
#   - keep gs-main in the reactor, with -am when building a single module: resolved from the
#     repository instead, it brings the regular BouncyCastle and the build stops on purpose
#   - use -nsu, not -o: the shared host repository is a remote, and offline mode refuses remotes
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

ARGS=${*:--Prelease install}

"$(dirname "${BASH_SOURCE[0]}")/sync.sh"
vm "cd ~/gs/src && mvnd -nsu -fae -Pfips $ARGS"
