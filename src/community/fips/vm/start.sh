#!/bin/bash
# Boots the machine created by create.sh. Stays in the foreground: run it with
#   setsid ./start.sh &
# to leave it running in the background. Stop it with: ssh -p 2222 localhost sudo poweroff
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

SOCK=${SOCK:-/tmp/vfsd-m2.sock}

[ -f "$VM_DIR/fips-build.qcow2" ] || { echo "No machine in $VM_DIR, run create.sh first" >&2; exit 1; }
if pgrep -af "qemu-system-x86_64.*fips-build.qcow2" > /dev/null; then
    echo "The machine is already running. Stop it first, or connect with: ssh -p $SSH_PORT localhost" >&2
    exit 1
fi

# The host maven repository is shared with the machine over virtiofs, so it downloads little.
# virtiofsd is a separate process and has to be listening before qemu connects to its socket.
if [ ! -S "$SOCK" ]; then
    VIRTIOFSD=$(command -v virtiofsd || echo /usr/libexec/virtiofsd)
    [ -x "$VIRTIOFSD" ] || { echo "virtiofsd not found, see README.md" >&2; exit 1; }
    "$VIRTIOFSD" --socket-path="$SOCK" --shared-dir "$M2_REPO" \
        --cache auto --sandbox none > "$VM_DIR/virtiofsd.log" 2>&1 &
    sleep 2
fi

echo "Booting. ssh reaches it on port $SSH_PORT, the console log is $VM_DIR/console.log"

# The Tomcat forward is bound to loopback on purpose: a connection from a docker container to a qemu
# user mode forward is accepted and then reset, so cite.sh relays it to the docker bridge instead.

# memory-backend-memfd is what lets virtiofs work; discard=unmap lets freed space return to the host
exec qemu-system-x86_64 \
  -enable-kvm -cpu host -smp "$CPUS" -m "$MEMORY" \
  -object memory-backend-memfd,id=mem,size="$MEMORY",share=on -numa node,memdev=mem \
  -drive file="$VM_DIR/fips-build.qcow2",if=virtio,format=qcow2,discard=unmap \
  -drive file="$VM_DIR/seed.iso",if=virtio,format=raw,readonly=on \
  -netdev user,id=net0,hostfwd=tcp::"$SSH_PORT"-:22,hostfwd=tcp:127.0.0.1:"$GS_PORT"-:8080 \
  -device virtio-net-pci,netdev=net0 \
  -chardev socket,id=charm2,path="$SOCK" \
  -device vhost-user-fs-pci,queue-size=1024,chardev=charm2,tag=m2repo \
  -display none -serial file:"$VM_DIR/console.log"
