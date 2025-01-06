#!/bin/bash

# fail on error
# set -e

if [ -d /home/teamengine/te_base/bin/unix/ ]; then
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

fi;


wms11() {
    echo $0

    run_rest_test_suite "wms11" "xml" "capabilities-url=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Fwms%3Fservice=WMS%26request%3DGetCapabilities%26version%3D1.1.0" "updatesequence=auto" "low-updatesequence=0" "high-updatesequence=100" "profile=queryable" "recommended=true" "getfeatureinfo=true" "feesconstraints=true" "bboxconstraints=EITHER"
}

wms13 () {
  echo $0
  
  run_rest_test_suite "wms13" "xml" "capabilities-url=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Fwms%3Fservice=WMS%26request%3DGetCapabilities%26version%3D1.3.0" "updatesequence=auto" "low-updatesequence=0" "high-updatesequence=100" "queryable=true" "recommended=true"
}

wfs10 () {
  echo $0

  run_rest_test_suite "wfs10" "xml" "capabilities-url=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Fwms%3Fservice=WFS%26request%3DGetCapabilities%26version%3D1.0.0" "multiplenamenspaces=true"
}

wfs11 () {
  # Cannot run via REST API, see https://github.com/opengeospatial/ets-wfs11/issues/109
  echo $0
  source="$TE_SCRIPTS_DIR/wfs/1.1.0/ctl/main.ctl"
  form="$TE_FORMS_DIR/wfs-1.1.0.xml"
  _run
}

wfs20 () {
  echo $0

  run_rest_test_suite "wfs20" "testng" "iut=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Fwfs%3Fservice=WFS%26request%3DGetCapabilities%26version%3D2.0.0"
}

wcs10 () {
  # WCS 1.0.0 is not supported by the REST API,s see https://github.com/opengeospatial/ets-wcs10/issues/43
  echo $0
  source="$TE_SCRIPTS_DIR/wcs/1.0.0/ctl/wcs.xml"
  form="$TE_FORMS_DIR/wcs-1.0.0.xml"
  _run
}

wcs11 () {
  echo $0

  run_rest_test_suite "wcs11" "xml" "url=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Fows%3Fservice=WCS%26request%3DGetCapabilities%26version%3D1.1.0"
}

ogcapi-features10() {
    run_rest_test_suite "ogcapi-features-1.0" "testng" "iut=http://geoserver:8080/geoserver/ogc/features/v1" "noofcollections=-1"
}

geotiff11() {
  # RGB WMS
  run_rest_test_suite_prefix "geotiff11" "01-wms-" "testng" "iut=http:%2F%2Fgeoserver:8080%2Fgeoserver%2Fwms?service%3DWMS%26version%3D1.1.0%26request%3DGetMap%26layers%3Dtopp%3Atazbm%26bbox%3D146.49999999999477%2C-44.49999999999785%2C147.99999999999474%2C-42.99999999999787%26width%3D767%26height%3D768%26srs%3DEPSG%3A4326%26styles%3D%26format%3Dimage%2Fgeotiff"

  # single band WCS
  run_rest_test_suite_prefix "geotiff11" "02-wcs-dem-basic-" "testng" "iut=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Ftopp%2Fows%3Fservice%3DWCS%26version%3D2.0.1%26request%3DGetCoverage%26coverageId%3Dtopp__tazdem"

  # single band rotated
  run_rest_test_suite_prefix "geotiff11" "03-wcs-rotated-" "testng" "iut=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Ftopp%2Fows%3Fservice%3DWCS%26version%3D2.0.1%26request%3DGetCoverage%26coverageId%3Dtopp__rotated"

  # single band, deflate compressed, tiled, WCS
  run_rest_test_suite_prefix "geotiff11" "04-wcs-dem-deflate-tiled-" "testng" "iut=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Ftopp%2Fows%3Fservice%3DWCS%26version%3D2.0.1%26request%3DGetCoverage%26coverageId%3Dtopp__tazdem%26compression%3DDeflate%26tiling%3Dtrue%26tileheight%3D256%26tilewidth%3D256"

  # three band, JPEG compressed, small tiles, WCS
  run_rest_test_suite_prefix "geotiff11" "05-wcs-dem-jpeg-tinytiles-" "testng" "iut=http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Ftopp%2Fows%3Fservice%3DWCS%26version%3D2.0.1%26request%3DGetCoverage%26coverageId%3Dtopp__tazbm%26compression%3DJPEG%26compression%3DJPEG%26jpeg_quality%3D75%26tiling%3Dtrue%26tileheight%3D32%26tilewidth%3D32"
}

run_rest_test_suite() {
  local suite_name=$1
  local format=$2
  shift 2
  run_rest_test_suite_prefix "$suite_name" "" "$format" "$@"
}

run_rest_test_suite_prefix() {
    set +x

    local suite_name=$1
    local targetfile="/logs/$2$3-results.xml"
    shift 3
    local params=("$@")

    echo $0

    teamenginehost=$(hostname)
    apiurl="http://$teamenginehost:8080/teamengine/rest/suites/$suite_name/run"
    
    # Build the URL parameters
    url_params=""
    for param in "${params[@]}"; do
        url_params+="$param&"
    done
    url_params=${url_params%&}  # Remove the trailing '&'

    testurl="$apiurl?$url_params"
    echo $testurl
    credentials="ogctest:ogctest"


    echo
    echo Running tests
    echo api URL: $apiurl
    echo iut: $iut
    echo Will save the XML report to $targetfile
    echo This may take a while...
    # if requesting application/zip, ogccite/teamengine-production will include both the html and xml results,
    # but ogccite/ets-ogcapi-features10 won't.

    # requesting application/xml as it's the one we can parse to fail the build if there are test failures
    curl -v -s -u "$credentials" "$testurl" -H "Accept: application/xml" > $targetfile
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
