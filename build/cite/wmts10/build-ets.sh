#!/bin/sh

# build the ets-wmts10 Docker image from the
# https://github.com/geoserver/ets-wmts10 fork to get a fix for the scaling tests, see issues:
# - https://github.com/opengeospatial/ets-wmts10/issues/98
# - https://github.com/opengeospatial/ets-wmts10/issues/99
# 
# Currently not all tests are run, due to:
# https://github.com/opengeospatial/ets-wmts10/issues/47 (fix pending)
#
# May other patches be needed in the future, we'll use the
# geoserver's fork `geoserver/integration` branch again, which is intended to apply
# patches to the ets that have not yet being released upstream

echo "Building the ogccite/ets-wmts10 docker image from https://github.com/geoserver/ets-wmts10 schema_invalid_i_j branch"
rm -rf ets-wmts10
git clone https://github.com/geoserver/ets-wmts10.git
cd ets-wmts10
git checkout schema_invalid_i_j
mvn clean install docker:build -Pdocker -DskipTests -ntp
cd ..