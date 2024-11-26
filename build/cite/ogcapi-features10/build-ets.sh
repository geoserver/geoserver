#!/bin/sh

# build the ets-ogcapi-features10 Docker image from the
# (at the time of writing, ogccite/ets-ogcapi-features10:1.8-SNAPSHOT-teamengine-5.4.1)
# from the geoserver's fork `geoserver/integration` branch, which applies
# patches to the ets that have not yet being released upstream

echo "Building the ogccite/ets-ogcapi-features10 docker image from the geoserver/integration branch"
rm -rf ets-ogcapi-features10
git clone https://github.com/geoserver/ets-ogcapi-features10.git
cd ets-ogcapi-features10
git checkout geoserver/integration && mvn clean install -Pdocker -DskipTests -ntp
cd ..
