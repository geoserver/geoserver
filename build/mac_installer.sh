#!/bin/bash

set -e 

function usage() {
  echo "$0 [options] <tag>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo 
  echo "Environment variables:"
  echo " SKIP_DOWNLOAD : Skips download of release artifacts"
  echo " SKIP_UPLOAD : Skips upload of mac installer"
}

tag=$1
if [ -z $tag ]; then
  usage
  exit 1
fi

# load properties
. "$( cd "$( dirname "$0" )" && pwd )"/properties

# load common functions
. "$( cd "$( dirname "$0" )" && pwd )"/functions

# download and ready the artifacts
setup_artifacts $tag mac

# Setup_artifacts extracts everything to tmp/
# Those resources are not needed.
rm -fr tmp/*

# Note: the '-mac' directory name is important for the 'build.sh' and 'build.xml' to succeed.
# The build uses a regex to determine and set a variable.
unzip -d tmp/geoserver-$tag-mac files/geoserver-$tag-mac.zip
cp files/geoserver-$tag-bin.zip tmp/geoserver-$tag-mac/

pushd tmp/geoserver-$tag-mac/ > /dev/null
./build.sh
#popd

# upload dmg to final location
upload_installer $tag geoserver-$tag.dmg

popd > /dev/null
exit 0

