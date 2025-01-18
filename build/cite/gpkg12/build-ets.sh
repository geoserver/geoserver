#!/bin/sh

# build the ets-gpkg12 Docker image from 
# https://github.com/geoserver/ets-gpkg12/tree/suffix_service_1_2 to get a fix for file extension checking.
#
# The branch is in particular a modification of the 1.2 official branch, rather than main, because the
# latter will only build using Java 17 (and 1.2 is the version used in the official teamengine docker image).

echo "Building the ogccite/ets-gpkg12 docker image from geoserver/ets-gpkg12 suffix_service_1_2 branch"
rm -rf ets-gpkg12
git clone https://github.com/geoserver/ets-gpkg12
cd ets-gpkg12
git checkout suffix_service_1_2
mvn clean install -Pdocker -DskipTests -ntp
cd ..
