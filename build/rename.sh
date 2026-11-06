#!/bin/bash

# error out if any statements fail
set -e

function usage() {
  echo "$0 advance [<release>]"
  echo "$0 rename [<release> [<gt.release> <gwc.release>]]"
  echo
  echo "Updates version references in the git checkout of the current directory,"
  echo "current versions are read from src/pom.xml."
  echo
  echo " advance : Stable branch to next patch version (pom.xml only)"
  echo " rename  : Main branch to next series, including GeoTools and GeoWebCache"
  echo
  echo "Examples:"
  echo " $0 advance           (3.0.3 -> 3.0.4)"
  echo " $0 advance 2.28.8    (2.28 -> 2.28.8)"
  echo " $0 rename            (3.1.0 -> 3.2.0, GeoTools 36 -> 37, GeoWebCache 2.1 -> 2.2)"
  echo " $0 rename 4.0.0 38 3.0"
}

. "$( cd "$( dirname "$0" )" && pwd )"/functions

cd `git rev-parse --show-toplevel`

current=`get_pom_version src/pom.xml`
current=${current%-SNAPSHOT}

case "$1" in
  advance)
    if [ ! -z $3 ]; then
      usage
      exit 1
    fi
    release=$2
    if [ -z $release ]; then
      if [[ ! "$current" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Current version $current is not x.y.z, specify the release"
        exit 1
      fi
      release=${current%.*}.$(( ${current##*.} + 1 ))
    fi
    echo "advancing $current-SNAPSHOT to $release-SNAPSHOT"
    update_version ">$current-SNAPSHOT<" ">$release-SNAPSHOT<" '*pom.xml'
    ;;
  rename)
    if [ ! -z $5 ] || ( [ ! -z $3 ] && [ -z $4 ] ); then
      usage
      exit 1
    fi
    gt_current=`grep "<gt.version>" src/pom.xml | sed 's/ *<gt.version>\(.*\)-SNAPSHOT<\/gt.version>/\1/g'`
    gwc_current=`grep "<gwc.version>" src/pom.xml | sed 's/ *<gwc.version>\(.*\)-SNAPSHOT<\/gwc.version>/\1/g'`
    minor=${current%.*}
    release=${2:-${minor%.*}.$(( ${minor##*.} + 1 )).0}
    gt_release=${3:-$(( gt_current + 1 ))}
    gwc_release=${4:-${gwc_current%.*}.$(( ${gwc_current##*.} + 1 ))}
    echo "renaming $current-SNAPSHOT to $release-SNAPSHOT, GeoTools $gt_release-SNAPSHOT, GeoWebCache $gwc_release-SNAPSHOT"
    update_version ">$current-SNAPSHOT<" ">$release-SNAPSHOT<" '*pom.xml'
    update_version "default='$current'" "default='$release'" doc/version.py
    update_version "default='${current%.*}'" "default='${release%.*}'" doc/version.py
    sed_i "s/\(<gt.version>\).*\(<\/gt.version>\)/\1$gt_release-SNAPSHOT\2/" src/pom.xml
    sed_i "s/\(<gwc.version>\).*\(<\/gwc.version>\)/\1$gwc_release-SNAPSHOT\2/" src/pom.xml
    ;;
  *)
    usage
    exit 1
    ;;
esac
