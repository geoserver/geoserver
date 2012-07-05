#!/bin/bash

# main script invoked by hudson

# sanity check parameters
[ -z $BRANCH ] && echo "BRANCH variable mandatory" && exit 1
[ -z $VERSION ] && echo "VERSION variable mandatory" && exit 1

OPTS="-b $BRANCH"

if [ ! -z $REV ]; then
  OPTS="$OPTS -r $REV"
fi
if [ ! -z $GT_VERSION ]; then
  OPTS="$OPTS -g $GT_VERSION"
fi
if [ ! -z $GWC_VERSION ]; then
  OPTS="$OPTS -w $GWC_VERSION"
fi
if [ ! -z $GIT_USER ]; then
  OPTS="$OPTS -u $GIT_USER"
fi
if [ ! -z $GIT_EMAIL ]; then
  OPTS="$OPTS -e $GIT_EMAIL"
fi

./build_release.sh $OPTS $VERSION
