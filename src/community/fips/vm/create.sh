#!/bin/bash
# Creates the Rocky Linux machine used to build and test GeoServer under a real FIPS operating
# system. Run once; start.sh boots it afterwards. See README.md.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

DISK_SIZE=${DISK_SIZE:-40G}
SSH_KEY=${SSH_KEY:-$HOME/.ssh/id_rsa.pub}
IMAGE=Rocky-$ROCKY_VERSION-GenericCloud.qcow2
IMAGE_URL=https://dl.rockylinux.org/pub/rocky/$ROCKY_VERSION/images/x86_64/Rocky-$ROCKY_VERSION-GenericCloud-Base.latest.x86_64.qcow2

for tool in qemu-system-x86_64 qemu-img genisoimage wget; do
    command -v $tool > /dev/null || { echo "Missing $tool, see README.md" >&2; exit 1; }
done
[ -r /dev/kvm ] || { echo "No access to /dev/kvm, see README.md" >&2; exit 1; }
[ -f "$SSH_KEY" ] || { echo "No ssh public key at $SSH_KEY; make one with ssh-keygen" >&2; exit 1; }

# Rocky 10 ships no OpenJDK 17, its AppStream has 21 and 25 only.
JDK_PACKAGES="  - java-21-openjdk-devel"
[ "$ROCKY_VERSION" = 9 ] && JDK_PACKAGES="$JDK_PACKAGES
  - java-17-openjdk-devel"

# Rocky 9 has fips-mode-setup and Rocky 10 dropped it. On Rocky 10 its three steps are done by
# hand: install the OpenSSL FIPS provider, set the system policy, add the kernel arguments.
# boot=UUID has to sit next to fips=1, because the image keeps /boot on its own partition. Without
# it the boot check looks for /boot/.vmlinuz-<release>.hmac on the root filesystem. The file is not
# there, and the boot stops.
if [ "$ROCKY_VERSION" = 9 ]; then
    FIPS_PACKAGES=""
    FIPS_COMMANDS="  - [ fips-mode-setup, --enable ]"
else
    FIPS_PACKAGES="  - openssl-fips-provider"
    FIPS_COMMANDS="  - [ update-crypto-policies, --set, FIPS ]
  - bash -c 'grubby --update-kernel=ALL --args=\"fips=1 boot=UUID=\$(findmnt -no UUID /boot)\"'
  - [ dracut, -f ]"
fi

mkdir -p "$VM_DIR/seed"
cd "$VM_DIR"

[ -f "$IMAGE" ] || wget -O "$IMAGE" "$IMAGE_URL"

# The image is 10G, too small for a build tree plus the container images the tests pull. qcow2 grows
# only as blocks are written, so this is a ceiling rather than a cost: expect 20-25G in practice.
cp "$IMAGE" fips-build.qcow2
qemu-img resize fips-build.qcow2 "$DISK_SIZE"

cat > seed/meta-data <<EOF
instance-id: fips-build-1
local-hostname: fips-build
EOF

# fips-mode-setup rebuilds the initramfs and takes effect on the next boot only, so the first boot
# ends by rebooting. Docker comes from Docker's own repository, for the testcontainers tests.
cat > seed/user-data <<EOF
#cloud-config
users:
  - name: $USER
    sudo: ALL=(ALL) NOPASSWD:ALL
    shell: /bin/bash
    ssh_authorized_keys:
      - $(cat "$SSH_KEY")
ssh_pwauth: false
packages:
$JDK_PACKAGES
  - git
  - rsync
  - crypto-policies-scripts
  - dnf-plugins-core
$FIPS_PACKAGES
runcmd:
  - [ dnf, config-manager, --add-repo, "https://download.docker.com/linux/centos/docker-ce.repo" ]
  - [ dnf, install, -y, docker-ce, docker-ce-cli, containerd.io ]
  - [ systemctl, enable, --now, docker ]
  - [ usermod, -aG, docker, $USER ]
  # Rocky ships maven 3.6 and GeoServer needs 3.8 or newer. mvnd is installed too: it builds the
  # reactor in parallel and keeps a warm daemon, which is what makes repeated builds bearable.
  - bash -c 'V=\$(curl -s https://dlcdn.apache.org/maven/maven-3/ | grep -oE "3\.9\.[0-9]+" | sort -V | tail -1); curl -sfLo /tmp/mvn.tgz https://dlcdn.apache.org/maven/maven-3/\$V/binaries/apache-maven-\$V-bin.tar.gz; tar xzf /tmp/mvn.tgz -C /opt; ln -sf /opt/apache-maven-\$V/bin/mvn /usr/local/bin/mvn'
  # a block scalar, because the colon and space inside the grep pattern would otherwise read as a
  # YAML mapping, and cloud-init then drops the whole runcmd list
  - |
    bash -c 'V=\$(curl -s https://api.github.com/repos/apache/maven-mvnd/releases/latest | grep -oE "\"tag_name\": \"[^\"]+" | cut -d\" -f4); curl -sfLo /tmp/mvnd.tgz https://github.com/apache/maven-mvnd/releases/download/\$V/maven-mvnd-\$V-linux-amd64.tar.gz; tar xzf /tmp/mvnd.tgz -C /opt; ln -sf /opt/maven-mvnd-\$V-linux-amd64/bin/mvnd /usr/local/bin/mvnd'
  # The host maven repository is offered as a remote to read from, so the machine downloads little,
  # and writes only into its own disk. Never share it read-write: a build inside the machine would
  # then install artifacts over the host's.
  - bash -c 'mkdir -p /home/$USER/.m2; chown $USER:$USER /home/$USER/.m2'
  - bash -c 'printf "%s\n" "<settings><profiles><profile><id>host</id><activation><activeByDefault>true</activeByDefault></activation><repositories><repository><id>host-m2</id><url>file:///mnt/m2</url><releases><enabled>true</enabled></releases><snapshots><enabled>true</enabled></snapshots></repository></repositories></profile></profiles></settings>" > /home/$USER/.m2/settings.xml; chown $USER:$USER /home/$USER/.m2/settings.xml'
  - bash -c 'echo "m2repo /mnt/m2 virtiofs defaults 0 0" >> /etc/fstab; mkdir -p /mnt/m2'
  # The Rocky 10 image runs cockpit on 9090. GeoServerWicketOnlineTest asks that port whether a
  # GeoServer is there, cockpit answers, and three tests that should skip run and fail instead.
  - [ systemctl, disable, --now, cockpit.socket ]
$FIPS_COMMANDS
  - [ reboot ]
EOF

genisoimage -quiet -output seed.iso -volid cidata -joliet -rock seed/user-data seed/meta-data

echo "Created $VM_DIR/fips-build.qcow2."
echo "Start it with start.sh; the first boot installs packages and reboots once, allow a few minutes."
