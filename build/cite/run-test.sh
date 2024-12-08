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

wcs20() {
    echo $0
    
    teamenginehost=$(hostname)
    apiurl="http://$teamenginehost:8080/teamengine/rest/suites/wcs/run"
    url="http%3A%2F%2Fgeoserver%3A8080%2Fgeoserver%2Fwcs%3Fservice%3DWCS%26request%3DGetCapabilities"
    testurl="$apiurl?url=$url&core=core&ext_post=post&ext_scal=scaling&ext_int=interpolation&ext_rsub=range_subsetting&ext_crs=crs"
    credentials="ogctest:ogctest"
    targetfile="/logs/xml-results.xml"
    
    set +x

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

    # Check if the first curl command failed
    if [ $? -ne 0 ]; then
        echo "Error: Failed to run tests"
        exit 1
    fi
}


# Usage:
# ogcapi-features10([iut])
# - iut: optional. URL of landing page. Defaults to 'default_iut="http://geoserver:8080/geoserver/ogc/features/v1'
ogcapi-features10() {
    # Define the default Instance Under Test
    default_iut="http://geoserver:8080/geoserver/ogc/features/v1"

    # Use provided argument if given, otherwise use the default value
    iut="${1:-$default_iut}"

    # Call the generic function with iut as the first argument and default values for the others
    run_rest_test 'ogcapi-features-1.0' "$iut"
}

# Usage:
# run_rest_test(suitename iut)
# - suitename: name of the test suite to run (e.g. 'ogcapi-features-1.0')
# - iut: URL of the Instance Under Test's landing page or GetCapabilities document
run_rest_test() {

    set +x

    local suitename="${1}"
    local iut="${2}"    

    local teamenginehost=$(hostname)

    # Check if required arguments are provided
    if [ -z "$suitename" ] || [ -z "$iut" ]; then
        echo "run-rest-test()> Invalid arguments. Expected run-rest-test(suitename, iut)"
        echo "suitename: name of the test suite to run (e.g., 'ogcapi-features-1.0')"
        echo "iut: URL of the Instance Under Test's landing page or GetCapabilities document"
        exit 1
    fi
    
    apiurl="http://$teamenginehost:8080/teamengine/rest/suites/$suitename/run"
    testurl="$apiurl?noofcollections=-1&iut=$iut"
    credentials="ogctest:ogctest"
    targetfile="/logs/testng-results.xml"

    echo
    echo Running tests
    echo api URL: $apiurl
    echo iut: $iut
    echo Will save the XML report to $targetfile
    echo This may take a while...

    # Requesting application/xml as it's the one we can parse to fail the build if there are test failures
    curl -v -s -u "$credentials" "$testurl" -H "Accept: application/xml" > $targetfile
    # Check if the curl command failed
    if [ $? -ne 0 ]; then
        echo "Error: Failed to run tests"
        exit 1
    fi
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
