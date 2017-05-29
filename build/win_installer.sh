#!/bin/bash

set -e 

function usage() {
  echo "$0 [options] <tag> <certhash>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo " certhash : SHA1 fingerprint of code-signing certificate"
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

certhash=$2
if [ -z $certhash ]; then
  usage
  exit 1
fi

# load properties
. "$( cd "$( dirname "$0" )" && pwd )"/properties
. "$( cd "$( dirname "$0" )" && pwd )"/win_properties

# load common functions
. "$( cd "$( dirname "$0" )" && pwd )"/functions

# download and ready the artifacts
setup_artifacts $tag win 

pushd tmp > /dev/null

# move geoserver files into place
mv geoserver-$tag/* .
rmdir geoserver-$tag

# create the exe
"$NSIS_EXE" GeoServerEXE.nsi

# code-sign exe
../code-sign-exe.sh $certhash geoserver-$tag.exe

# upload exe to final location
upload_installer $tag geoserver-$tag.exe

popd > /dev/null
exit 0

