#!/bin/bash

# error out if any statements fail
set -e
set -x

function usage() {
  echo "$0 [options] <tag>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo 
}

if [ -z $1 ]; then
  usage
  exit
fi

tag=$1

# load properties + functions
. "$( cd "$( dirname "$0" )" && pwd )"/properties
. "$( cd "$( dirname "$0" )" && pwd )"/functions

# deploy the release to maven repo
pushd tags/$tag/src > /dev/null
mvn deploy -DskipTests
popd > /dev/null

# upload artifacts to sourceforge
pushd $DIST_PATH/$tag > /dev/null

rsync -ave "ssh -i $SF_PK" *.zip *.exe *.dmg $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/

pushd plugins > /dev/null

rsync -ave "ssh -i $SF_PK" *.zip $SF_USER@$SF_HOST:"/home/pfs/project/g/ge/geoserver/GeoServer\ Extensions/$tag/"

popd > /dev/null
popd > /dev/null
