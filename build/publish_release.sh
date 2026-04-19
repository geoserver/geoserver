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
  echo " MAVEN_SETTINGS : settings.xml override"
  echo " SKIP_DEPLOY : Skips deploy to maven repository"
  echo " SKIP_COMMUNITY : Skips deploy of community modules"
  echo " SKIP_UPLOAD : Skips upload to source forge"
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

# init_git $git_user $git_email

# fetch single tag
git fetch origin refs/tags/$tag:refs/tags/$tag --no-tags

# ensure tag already exists
if [ `git tag --list $tag | wc -l` == 0 ]; then
  echo "tag $tag not available on $GS_GIT_URL"
  exit 1
fi

# check to see if a release branch already exists
if [ `git branch --list $tag.x | wc -l` == 1 ]; then
  echo "branch $tag.x exists, checking out"
  git checkout $tag.x
else
  echo `release branch $tag.x not available, run build_release.sh first`
fi

# ensure the tagged revision actually on this branch
set +e
git log | grep $tag
if [ $? != 0 ]; then
   echo "Tag $tag not a on branch $tag.x"
   echo "(Perhaps $tag.x is from a prior failed attempt and can be removed)"
   exit -1
fi
set -e

# deploy the release to maven repo
pushd src > /dev/null
if [ -z $SKIP_DEPLOY ]; then
   mvn clean install -P allExtensions,pending -DskipTests $MAVEN_FLAGS 
   mvn deploy -P allExtensions,pending -DskipTests $MAVEN_FLAGS 
else
   echo "Skipping mvn deploy -P allExtensions,pending -DskipTests $MAVEN_FLAGS"
fi

if [ -z $SKIP_COMMUNITY ]; then
   pushd community > /dev/null
   set +e
   mvn clean install -P communityRelease -DskipTests $MAVEN_FLAGS || true
   mvn deploy -P communityRelease -DskipTests $MAVEN_FLAGS || true

   set -e
   popd > /dev/null
else
   echo "Skipping mvn clean deploy -P communityRelease -DskipTests $MAVEN_FLAGS"
fi

popd > /dev/null

# upload artifacts to sourceforge

pushd distribution/$tag > /dev/null

if [ -z $SKIP_UPLOAD ]; then
  rsync -ave "ssh " *-bin.zip *-war.zip *doc.zip $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/
  
  # don't fail if exe or pdf is not around
  set +e
  rsync -ave "ssh " *.exe  *.pdf $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/
  set -e
  pushd plugins > /dev/null
  rsync -ave "ssh " *.zip $SF_USER@$SF_HOST:"/home/pfs/project/g/ge/geoserver/GeoServer/$tag/extensions"
  popd > /dev/null
else
  echo "Skipping rsync -ave "ssh " *.zip *.exe $SF_USER@$SF_HOST:/home/pfs/project/g/ge/geoserver/GeoServer/$tag/"
  echo "Skipping rsync -ave "ssh " *.zip $SF_USER@$SF_HOST:"/home/pfs/project/g/ge/geoserver/GeoServer/$tag/extensions""
fi

popd > /dev/null

# release done, revert branch to snapshot, and supply a merge commit for commit history
git revert $tag
git push $tag.x

git checkout $branch
git merge --no-ff $tag.x -m "Release $tag completed"

