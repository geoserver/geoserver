#!/bin/bash

function check_rc() {
  if [ $1 -gt 0 ]; then
    echo "$2 failed with return value $1"
    exit 1
  else
    echo "$2 succeeded return value $1"
  fi
}

# find marlin jar in zip file
geoserver_zip=`find . -name "geoserver-*-bin.zip" | head -1`
if [ ! -z "$geoserver_zip" ]; then
  marlin_jar=`unzip -l -qq ${geoserver_zip} "*/WEB-INF/lib/marlin*.jar" | awk -F "/" '{print $NF}'`
fi

# build the app
ant -Dmarlin_jar=${marlin_jar}
check_rc $? "build app"

APP_NAME=GeoServer.app
APP=target/$APP_NAME
VER=$( cat ${APP}/Contents/Info.plist | grep -A 1 CFBundleVersion | tail -n 1 | sed 's/.*<string>//g' | sed 's/<\/string>//g' )

# codesign application bundle
# set CODESIGN_CERT_SHA1 env variable to SHA1 fingerprint of signing certificate
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")"; pwd -P)
if [ -x $SCRIPT_DIR/codesign-geoserver-app.sh ]; then
  $SCRIPT_DIR/codesign-geoserver-app.sh $SCRIPT_DIR/$APP
  check_rc $? "codesign app bundle"
fi

VOL=geoserver-$VER

# unmount existing mounts
if [ -d "/Volumes/${VOL}" ]; then
  umount "/Volumes/${VOL}"
fi

# build the dmg volume
rm *.dmg

# requires dmgbuild >= 1.0.0 (ideally >= 1.1.0)
# https://pypi.python.org/pypi/dmgbuild
# note: dmgbuild does not need Finder.app or an active GUI login
dmgbuild -s ./dmgbuild_settings.py -D app=$APP "${VOL}" "${VOL}.dmg"
check_rc $? "generating DMG"

exit 0

