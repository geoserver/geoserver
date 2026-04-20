#!/bin/bash

# error out if any statements fail
set -e

function usage() {
  echo "$0 <tag> <branch>"
  echo
  echo " tag : Release tag (eg: 3.0.0, 3.0.1-SNAPSHOT, 3.0.0-RC, ...)"
  echo " branch: Release branch (eg, 3.0.x, main, 3.0.1.x,...)" 
  echo 
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

echo "tag: $tag"
echo "branch: $branch"

if [ `is_primary_branch_num $tag` == "1" ]; then
  echo "$tag is the same as primary branch name"
fi

if [ "`is_version_num $gt_ver`" == "1" ]; then
  echo "$tag is a version number"
fi