#!/bin/bash

# error out if any statements fail
set -e

function usage() {
  echo "$0 advance <current> <release>"
  echo "$0 rename <current> <release> <gt.current> <gt.release> <gwc.current> <gwc.release>"
  echo
  echo "Updates version references in the git checkout of the current directory."
  echo
  echo " advance : Stable branch to next patch version (pom.xml only)"
  echo " rename  : Main branch to next series, including GeoTools and GeoWebCache"
  echo
  echo "Examples:"
  echo " $0 advance 3.0.3 3.0.4"
  echo " $0 rename 3.1.0 3.2.0 36 37 2.1 2.2"
}

. "$( cd "$( dirname "$0" )" && pwd )"/functions

cd `git rev-parse --show-toplevel`

case "$1" in
  advance)
    if [ -z $3 ] || [ ! -z $4 ]; then
      usage
      exit 1
    fi
    update_version $2-SNAPSHOT $3-SNAPSHOT '*pom.xml'
    ;;
  rename)
    if [ -z $7 ] || [ ! -z $8 ]; then
      usage
      exit 1
    fi
    update_version $2-SNAPSHOT $3-SNAPSHOT '*pom.xml' src/release/bin.xml \
      src/release/installer/win/GeoServerEXE.nsi src/release/installer/win/wrapper.conf
    update_version $2 $3 doc/version.py
    update_version ${2%.*} ${3%.*} doc/version.py
    update_version $4-SNAPSHOT $5-SNAPSHOT src/pom.xml
    update_version $6-SNAPSHOT $7-SNAPSHOT src/pom.xml
    ;;
  *)
    usage
    exit 1
    ;;
esac
