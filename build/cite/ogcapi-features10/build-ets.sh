#!/bin/sh

# build the ets-ogcapi-features10 Docker image from the
# https://github.com/opengeospatial/ets-ogcapi-features10 main repo's master branch,
# since the required pull request has been merged (https://github.com/opengeospatial/ets-ogcapi-features10/pull/255)
# but there's no new beta release so far.
# This will build ogccite/ets-ogcapi-features10:1.8-SNAPSHOT-teamengine-5.4.1
#
# May other patches be needed in the future, we'll use the
# geoserver's fork `geoserver/integration` branch again, which is intended to apply
# patches to the ets that have not yet being released upstream

echo "Building the ogccite/ets-ogcapi-features10 docker image from opengeospatial/ets-ogcapi-features10 master branch"
rm -rf ets-ogcapi-features10
#git clone https://github.com/geoserver/ets-ogcapi-features10.git
git clone https://github.com/opengeospatial/ets-ogcapi-features10.git
cd ets-ogcapi-features10
#git checkout geoserver/integration
git checkout 1.8
mvn clean install -Pdocker -DskipTests -ntp
cd ..
