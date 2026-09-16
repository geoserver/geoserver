#!/bin/bash
# Settings and helpers the other scripts in this directory share. Sourced, not run.
#
# Override any of these in the environment:
#   VM_DIR    where the disk image and the boot files live (default $HOME/geoserver-fips-vm)
#   ROCKY_VERSION  Rocky Linux release to build the machine from (default 9). Give each release its
#             own VM_DIR and SSH_PORT to keep two machines side by side.
#   SSH_PORT  host port that reaches the machine over ssh (default 2222)
#   GS_PORT   host port that reaches Tomcat in the machine (default 8080)
#   CPUS      processors given to the machine (default 8)
#   MEMORY    memory given to the machine (default 12G)
#   M2_REPO   host maven repository shared with the machine (default $HOME/.m2/repository)

VM_DIR=${VM_DIR:-$HOME/geoserver-fips-vm}
ROCKY_VERSION=${ROCKY_VERSION:-9}
SSH_PORT=${SSH_PORT:-2222}
GS_PORT=${GS_PORT:-8080}
CPUS=${CPUS:-8}
MEMORY=${MEMORY:-12G}
M2_REPO=${M2_REPO:-$HOME/.m2/repository}

# the src directory of this checkout, three levels up from here
SRC_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)

SSH_OPTS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -p $SSH_PORT"
# -n keeps ssh from reading the caller's own input, which would eat a loop driving it
SSH="ssh -n $SSH_OPTS $USER@localhost"

# Runs a command inside the machine, or fails with a readable message when it is not up.
vm() {
    $SSH "$@" || {
        echo "The machine did not answer on port $SSH_PORT. Is it running? See start.sh" >&2
        return 1
    }
}
