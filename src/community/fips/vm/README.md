# A FIPS machine for building and testing GeoServer with FIPS mode enabled

These scripts build a Rocky Linux virtual machine with FIPS mode on, copy this checkout into it, and
build GeoServer there. Only a machine with FIPS shows which modules use restricted algorithms.

## Why a virtual machine

FIPS mode is a property of the running kernel, read from `/proc/sys/crypto/fips_enabled`. A container
shares the kernel of its host, so on a normal host a container reports FIPS off whatever is installed
inside it. A machine with its own kernel is the only way to turn it on.

Rocky Linux is used because it rebuilds Red Hat Enterprise Linux. The Red Hat Java runtime is what
applies the system cryptographic policy to Java. Rocky is fine for testing, and is not itself
certified: a deployment that has to be certified runs a certified distribution.

## What you need on your own machine

- A Linux OS. Not tested on macOS. Windows would need a different set of scripts.
- Hardware virtualization, that is a readable `/dev/kvm`.
- The packages `qemu-system-x86`, `qemu-utils`, `virtiofsd`, `genisoimage`, `wget`, `rsync`
  (on Red Hat family systems: `qemu-kvm`, `qemu-img`, `virtiofsd`, `genisoimage`, `wget`, `rsync`).
- An ssh key at `~/.ssh/id_rsa.pub`, or `SSH_KEY` naming another one.
- About 30GB of free disk. The disk image has a 40GB ceiling but grows only as blocks are written:
  after a full build with tests it measures 13GB, and the docker images the testcontainers tests pull
  add a few more.
- Enough memory to give the machine 12GB. `mvnd` builds the reactor in parallel and keeps a daemon
  alive between builds, and each surefire fork of a GeoServer module wants around 1GB.

Everything the machine consists of goes in `~/geoserver-fips-vm`, outside the checkout: the disk
image is measured in gigabytes and has no business in a git repository. Set `VM_DIR` to move it.

## Which Rocky release

`ROCKY_VERSION` picks the release. It defaults to 9 because Rocky 10 is new and most deployments
still run 9. Every script reads the variable, so it has to be set on each of them.

To keep two machines at once, give each release its own directory and its own ssh port:

``` bash
export ROCKY_VERSION=10 VM_DIR=~/geoserver-fips-vm-10 SSH_PORT=2232
```

Only one of them can serve CITE, because `cite.sh` reaches the machine on port 8080 of the host.
Each machine wants 12GB while it runs, so check there is memory for both before starting the second.

## Running it

``` bash
cd src/community/fips/vm
./create.sh                # downloads the image and prepares the machine, once
setsid ./start.sh &        # boots it, and keeps running until the machine stops
```

The first boot installs packages, turns FIPS on and reboots itself, because enabling FIPS rebuilds
the boot image and takes effect on the next boot only. Allow a few minutes, then check all three
layers are on:

``` bash
ssh -p 2222 localhost 'cat /proc/sys/crypto/fips_enabled; update-crypto-policies --show'
```

Expect `1` and `FIPS`. Java is in FIPS mode when `SunPKCS11-NSS-FIPS` is first in the provider list.
On Rocky 9 `fips-mode-setup --check` says the same thing in words. Rocky 10 no longer ships that
command, so the kernel flag is what gets read here.

Then build:

``` bash
./build.sh                              # the whole thing with tests
./build.sh -pl :gs-main -am install     # one module and what it needs
```

`build.sh` copies the sources in first, so run it again after every edit. It always builds with
`-Pfips`, and anything you pass replaces the rest of the arguments. Building with `-Pfips`
puts the FIPS validated BouncyCastle in place of the regular one. It registers it first, in
approved-only mode, the same way a deployment does. That is the point of this machine: the
restrictions of the operating system and those of the validated module are both real, and only their
combination behaves like a FIPS deployment.

When you are done, stop the machine. The disk keeps everything, so `start.sh` brings it back as it
was, with the build tree and the maven cache already there:

``` bash
./stop.sh
```

## Running the CITE suites against a deployment

The build checks the code works under a FIPS provider. It does not say a released GeoServer does: the
test harness writes its own data directories, and a war is never started on a FIPS machine during a
build. `cite.sh` closes that gap. It deploys the war on plain Tomcat inside the machine, on the
machine's own Java, and runs the OGC CITE suites against it.

Teamengine runs in docker on your own machine, not in the virtual machine. It is an HTTP client and
its cryptography has nothing to do with what is being measured. Nothing under `build/cite` is
touched: the shipped composition would build its own GeoServer container from a stock Ubuntu image
with no FIPS policy. That is exactly what this machine exists to avoid.

You need `docker` and `socat` on your own machine, and one extra step in the virtual machine, since
`create.sh` installs no server:

``` bash
./cite.sh install          # Tomcat and PostgreSQL with PostGIS, once
```

Then build a war with the FIPS BouncyCastle and the extensions the suites ask for, and run a suite:

``` bash
cd ../../..                # the src directory
mvnd -nsu -fae -Pfips,ogcapi-features,ogcapi-tiles,geopkg-output,app-schema,wcs10,wcs11,mbstyle \
  -Dmaven.test.skip=true install
cd community/fips/vm
./cite.sh run wms13        # or wms11 wfs10 wfs11 wfs20 wcs11 wmts10
                           #    geotiff11 gpkg12 ogcapi-tiles10 ogcapi-features10
./cite.sh log 200          # the Tomcat log, when a suite reports nothing useful
./cite.sh stop
```

`run` does everything one suite needs: it loads the PostGIS dataset when the suite has one, copies
the war and the suite data directory in, starts Tomcat and prints the report. There is one Tomcat, so
suites take turns. Check the war holds the right cryptography before the first run:

``` bash
unzip -l ../../../web/app/target/geoserver.war | grep -iE 'bcprov|bc-fips|bcpkix|bcutil'
```

It must list `bc-fips`, `bcpkix-fips` and `bcutil-fips`, and nothing else from BouncyCastle. Never
add `fips-test` to a war build: that profile declares `bc-fips` at test scope, which beats the
transitive compile one, and the jar silently leaves `WEB-INF/lib`.

Four suites use a teamengine image that is not on Docker Hub, `gpkg12`, `wmts10`, `wcs20` and
`ogcapi-features10`. Each has a `build-ets.sh` next to its composition; run it once. Every one of
those scripts calls `mvn`, so a plain Maven has to be on the `PATH`, `mvnd` alone is not enough.
`ogcapi-features10` needs two more things: `JAVA_HOME` has to be set, and its javadoc step fails
fetching `http://testng.org/javadocs/`, which no longer serves a package list. Build that one by hand:

``` bash
cd ../../../../build/cite/ogcapi-features10
git clone https://github.com/opengeospatial/ets-ogcapi-features10.git
cd ets-ogcapi-features10 && git checkout 1.8
mvn clean install -Pdocker -DskipTests -Dmaven.javadoc.skip=true
```
Every suite tested by the GeoServer CITE GitHub actions passes in FIPS mode too.

Some important notes:

- The CITE data directories were written by GeoServer 2.x and seven of them hold a `security/`
  directory with a JCEKS keystore. A FIPS Java has no JCEKS at all, so `cite.sh` deletes the files
  bound to that keystore and keeps the plain text rule files. GeoServer then writes a fresh security
  directory of its own, with a keystore type it can read. `wms13` has no `security/` at all, so
  it exercises the create-from-nothing path.
- The data directories point their PostGIS stores at the `postgres` container of the composition with
  the password `cite`, and both are rewritten. The password has to be at least 14 characters: the
  PostgreSQL driver computes SCRAM through BouncyCastle, and a validated module refuses a key that
  short. `psql` connects to the same database with a short password, because it never goes near Java.
- `GEOSERVER_DATA_DIR` and `GEOWEBCACHE_CACHE_DIR` are exported in `setenv.sh`, which is where Tomcat
  reads them. When the log says `Loading catalog` with a path inside the war, `setenv.sh` did not
  arrive: GeoServer falls back to the war's own `data` directory when it finds no location it can
  write to.

## Proving a change is needed

A test that passes with a change applied does not show the change was necessary. `prove.sh` reverts
one commit, runs the tests that motivated it, and restores the checkout. The commit is justified only
when those tests fail without it.

``` bash
./prove.sh <commit> <module> '<test pattern>'
```

## What this environment forbids, and what it does not

Measured with the validated module registered first in approved-only mode on top of system FIPS:

| | available |
| --- | --- |
| MD5, SHA-1 as a digest | yes, from the system security token |
| DES cipher | yes |
| JKS and PKCS12 keystores | yes |
| default TLS context | yes |
| SHA1PRNG | no |
| password based ciphers such as `PBEWithMD5AndDES` | no |
| JCEKS keystore | no |
| `SecureRandom.getInstanceStrong()` | no |

The validated module in approved-only mode refuses MD5 itself, so anything GeoServer routes through
it is restricted, while a call that names no provider falls through to Java and still works. The two
coexist, and that is what makes a FIPS deployment possible without rewriting every digest in the
code base.

## Reminders

- Enabling FIPS needs the reboot. Before it, the crypto policy already reads `FIPS` while
  `/proc/sys/crypto/fips_enabled` still reads 0, and Java is not in FIPS mode.
- Rocky 10 has no OpenJDK 17, so that machine builds on 21 alone.
- Use `-nsu` in a build, never `-o`. The host maven repository is offered to the machine as a remote
  it can read, and offline mode refuses every remote, a local one included.
- Keep `gs-main` in the reactor, with `-am` when building one module. Resolved from the repository
  instead, it brings the regular BouncyCastle back, and the build stops with a banned dependency.
  That check is doing its job: the regular and the FIPS BouncyCastle jars share their Java package
  names, and on one classpath neither works.
- `start.sh` fails with a message about a socket when the machine is already running from an earlier
  session. Check with `pgrep -af qemu-system`.
- After a rename in the source tree, sync once with `./sync.sh --fresh`. The sync leaves `target/` out
  of the transfer. So it cannot delete a renamed directory that still holds one, and the stale copy
  stays in the machine.
- The disk grows as blocks are written and does not shrink when a build is cleaned. Hand the space
  back with `ssh -p 2222 localhost 'sudo fstrim -av'`, on the port that machine listens on.
- The machine defaults to 8 processors and 12GB, `CPUS` and `MEMORY` change that.
