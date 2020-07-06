#!/bin/bash

# fail on error
set -e

TE_LOG_DIR="$TE_BASE/users/root"
TE_FORMS_DIR="$TE_BASE/forms"

mkdir -p "$TE_FORMS_DIR"

cd "/root/te_base/bin/unix/"


# test.sh comes from https://github.com/opengeospatial/teamengine/blob/master/teamengine-console/src/main/scripts/shell/unix/test.sh
# viewlog.sh comes from https://github.com/opengeospatial/teamengine/blob/master/teamengine-console/src/main/scripts/shell/unix/viewlog.sh

_run() {
  ./test.sh \
    -source="$source" \
    -form="$form"  && \
    ./viewlog.sh      \
    -logdir="$TE_LOG_DIR" \
    -session=s0001
  local rc=$?
  if [ "$rc" -ne "0" ]; then
      echo "test.sh failed!" >&2
  fi
  return $status
}

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
