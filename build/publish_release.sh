#!/bin/bash

# error out if any statements fail
set -e

function usage() {
  echo "$0 [options] <tag> <branch>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo " branch: Release branch (eg, 2.1.x, 2.2.x)" 
  echo 
  echo "Environment variables:"
  echo " SKIP_DEPLOY : Skips deploy to maven repository"
  echo " SKIP_MERGE_AND_TAG : Skips merge/tag of release branch"
  echo " SKIP_PUSH : Skips pushing changes to release branch and tag"
}

if [ -z $2 ]; then
  usage
  exit
fi

tag=$1
branch=$2

# load properties + functions
. "$( cd "$( dirname "$0" )" && pwd )"/properties
. "$( cd "$( dirname "$0" )" && pwd )"/functions

if [ `is_version_num $tag` == "0" ]; then
  echo "$tag is a not a valid release tag"
  exit 1
fi
if [ `is_primary_branch_num $tag` == "1" ]; then
  echo "$tag is a not a valid release tag, can't be same as primary branch name"
  exit 1
fi

pushd .. > /dev/null

# switch to the release branch
git checkout rel_$tag

# ensure no changes on it
set +e
git status | grep "working directory clean"
if [ "$?" == "1" ]; then
  echo "branch rel_$tag dirty, exiting"
  exit 1
fi
set -e

# deploy the release to maven repo
pushd src > /dev/null
if [ -z $SKIP_DEPLOY ]; then
   mvn deploy -DskipTests	
else
   echo "Skipping mvn deploy -DskipTests"
fi

popd > /dev/null

# upload artifacts to sourceforge
pushd $DIST_PATH/$tag > /dev/null

if [ -z $SKIP_DEPLOY ]; then
  rsync -ave "ssh -i $SF_PK" *.zip *.exe *.dmg $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/
  pushd plugins > /dev/null
  rsync -ave "ssh -i $SF_PK" *.zip $SF_USER@$SF_HOST:"/home/pfs/project/g/ge/geoserver/GeoServer\ Extensions/$tag/"
  popd > /dev/null
else
  echo "Skipping rsync -ave "ssh -i $SF_PK" *.zip *.exe *.dmg $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/"
  echo "Skipping rsync -ave "ssh -i $SF_PK" *.zip $SF_USER@$SF_HOST:"/home/pfs/project/g/ge/geoserver/GeoServer\ Extensions/$tag/""
fi

popd > /dev/null

# merge the tag release branch into main release branch and tag it
git checkout rel_$branch
if [ -z $SKIP_MERGE_AND_TAG ]; then
  git merge -m "Merging rel_$tag into rel_$branch" rel_$tag
  git tag $tag
else
  echo "Skipping git merge -m "Merging rel_$tag into rel_$branch" rel_$tag"
  echo "Skipping git tag $tag"
fi

# push them up
if [ -z $SKIP_PUSH ]; then
  git push origin rel_$branch
  git push origin $tag
else
  echo "Skipping git push origin rel_$branch"
  echo "Skipping git push origin $tag"
fi

