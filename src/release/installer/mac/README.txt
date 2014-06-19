GeoServer Mac Installer Build Instructions
------------------------------------------

To generate the installer just run the `build.sh` script in this directory. It
requires that the -bin.zip artifact exists under `target/release` under the
root of the source tree.

If using this with bin and mac bundles (from the build service). You will need to edit
GeoServer.app/Contents/Info.plish with the matching version number (see CFBundleVersion and
CFBundleShortVersionString). 