#!/bin/bash -e
printf "\nCopy database connection resource file"
mkdir --parents --verbose ~/.geoserver
cp --verbose --force ./build/ci/oracle/oracle.properties ~/.geoserver/

printf "\nSetup GEOSERVER user\n"
docker exec -i geoserver sqlplus -l system/oracle@//localhost:1521/XE < build/ci/oracle/setup-oracle.sql
