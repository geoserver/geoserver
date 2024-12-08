#!/bin/sh

# build the ets-wcs20 Docker image from the
# https://github.com/geoserver/ets-wcs20 fork to get a fix for the scaling tests, see
# https://github.com/opengeospatial/ets-wcs20/issues/141
#
# May other patches be needed in the future, we'll use the
# geoserver's fork `geoserver/integration` branch again, which is intended to apply
# patches to the ets that have not yet being released upstream

echo "Building the ogccite/ets-wcs20 docker image from https://github.com/geoserver/ets-wcs20 scal_allow_either_gc_or_rgc branch"
rm -rf ets-wcs20
git clone git@github.com:geoserver/ets-wcs20.git
cd ets-wcs20
git checkout scal_allow_either_gc_or_rgc
# git checkout 1.8
mvn clean install -Pdocker -DskipTests -ntp
cd ..
