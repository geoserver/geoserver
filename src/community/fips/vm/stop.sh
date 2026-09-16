#!/bin/bash
# Shuts the machine down. The disk keeps everything, so start.sh brings it back as it was.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

if ! pgrep -af "qemu-system-x86_64.*fips-build.qcow2" > /dev/null; then
    echo "The machine is not running"
    exit 0
fi

# poweroff kills the ssh connection on its way out, so a non-zero exit here means nothing
$SSH sudo poweroff || true
echo "Shutting down, allow a few seconds"
for _ in $(seq 1 30); do
    pgrep -af "qemu-system-x86_64.*fips-build.qcow2" > /dev/null || { echo "Stopped"; exit 0; }
    sleep 2
done
echo "Still running after a minute. Look at $VM_DIR/console.log" >&2
exit 1
