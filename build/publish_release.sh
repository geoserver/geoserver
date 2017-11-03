#!/bin/bash

# error out if any statements fail
set -e

function usage() {
  echo "$0 [options] <tag> <branch> <user> <email>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo " branch: Release branch (eg, 2.1.x, 2.2.x)" 
  echo " user:  Git username"
  echo " email: Git email"
  echo 
  echo "Environment variables:"
  echo " SKIP_DEPLOY : Skips deploy to maven repository"
  echo " SKIP_MERGE_AND_TAG : Skips merge/tag of release branch"
  echo " SKIP_PUSH : Skips pushing changes to release branch and tag"
}

if [ -z $4 ]; then
  usage
  exit
fi

tag=$1
branch=$2
git_user=$3
git_email=$4

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

init_git $git_user $git_email

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
   mvn deploy -P allExtensions -DskipTests	

   # deploy released community modules
   pushd community > /dev/null
   set +e
   mvn deploy -P communityRelease -DskipTests
   set -e
   popd > /dev/null
else
   echo "Skipping mvn deploy -P allExtensions -DskipTests"
fi

popd > /dev/null

# upload artifacts to sourceforge
pushd $DIST_PATH/$tag > /dev/null

if [ -z $SKIP_DEPLOY ]; then
  rsync -ave "ssh -i $SF_PK" *-bin.zip *-war.zip *doc.zip *.pdf $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/
  
  # don't fail if exe or dmg is not around
  set +e
  rsync -ave "ssh -i $SF_PK" *.exe *.dmg $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/
  set -e
  pushd plugins > /dev/null
  rsync -ave "ssh -i $SF_PK" *.zip $SF_USER@$SF_HOST:"/home/pfs/project/g/ge/geoserver/GeoServer/$tag/extensions"
  popd > /dev/null
else
  echo "Skipping rsync -ave "ssh -i $SF_PK" *.zip *.exe *.dmg $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/"
  echo "Skipping rsync -ave "ssh -i $SF_PK" *.zip $SF_USER@$SF_HOST:"/home/pfs/project/g/ge/geoserver/GeoServer/$tag/extensions""
fi

popd > /dev/null

# tag release branch
if [ -z $SKIP_MERGE_AND_TAG ]; then
  git tag $tag
else
  echo "Skipping git tag $tag"
fi

# push tag up
if [ -z $SKIP_PUSH ]; then
  git push origin $tag
else
  echo "Skipping git push origin $tag"
fi

