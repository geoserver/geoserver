#!/bin/bash

# fail on error
# set -e

TE_LOG_DIR="$TE_BASE/users/teamengine"
TE_FORMS_DIR="$TE_BASE/forms"

mkdir -p "$TE_FORMS_DIR"

cd "/home/teamengine/te_base/bin/unix/"


# test.sh comes from https://github.com/opengeospatial/teamengine/blob/master/teamengine-console/src/main/scripts/shell/unix/test.sh
# viewlog.sh comes from https://github.com/opengeospatial/teamengine/blob/master/teamengine-console/src/main/scripts/shell/unix/viewlog.sh

_show_logs() {
    ./viewlog.sh      \
    -logdir="$TE_LOG_DIR" \
    -session=s0001
}

set -o pipefail
_parse_logs(){
  _show_logs | grep -iw "Failed"
  local grep_exit_code=$?
  if [ "$grep_exit_code" -eq "0" ]; then
      echo "Failed tests found in logs! (grep exit code: $grep_exit_code)" >&2
      return 3
  else
      echo "No Failed tests found in logs" >&2
      return 0
  fi
}

_run() {
  ./test.sh \
    -source="$source" \
    -form="$form"
  local rc=$?
  if [ "$rc" -ne "0" ]; then
      echo "test.sh failed!" >&2
      rc=10
  fi

  _show_logs
  if [ "$?" -ne "0" ]; then
      echo "viewlog.sh failed, I cannot tell if the tests failed or not." >&2
      return 20
  fi

  _parse_logs
  if [ "$?" -ne "0" ]; then
      echo "The log shows a failed test!" >&2
      rc=3
  fi

  return $rc
}
set +o pipefail

wms11 () {
  echo $0
  source="$TE_SCRIPTS_DIR/wms11/1.1.1/ctl/main.xml"
  form="$TE_FORMS_DIR/wms-1.1.1.xml"
  _run
}

wms13 () {
  echo $0
  source="$TE_SCRIPTS_DIR/wms13/1.3.0/ctl/main.xml"
  form="$TE_FORMS_DIR/wms-1.3.0.xml"
  _run
}

wfs10 () {
  echo $0
  source="$TE_SCRIPTS_DIR/wfs/1.0.0/ctl/main.xml"
  form="$TE_FORMS_DIR/wfs-1.0.0.xml"
  _run
}

wfs11 () {
  echo $0
  source="$TE_SCRIPTS_DIR/wfs/1.1.0/ctl/main.ctl"
  form="$TE_FORMS_DIR/wfs-1.1.0.xml"
  _run
}

wcs10 () {
  echo $0
  source="$TE_SCRIPTS_DIR/wcs/1.0.0/ctl/wcs.xml"
  form="$TE_FORMS_DIR/wcs-1.0.0.xml"
  _run
}

wcs11 () {
  echo $0
  source="$TE_SCRIPTS_DIR/wcs/1.1.1/ctl/wcs.xml"
  form="$TE_FORMS_DIR/wcs-1.1.1.xml"
  _run
}

interactive () {
    /usr/local/tomcat/bin/startup.sh
    while true; do sleep 100000; done
}

run_all () {

  # WMS
  wms11
  wms13

  # WFS
  wfs10
  wfs11

  # WCS
  wcs10
  wcs11
}

eval $@
