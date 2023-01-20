#!/bin/bash -e
docker version

# this docker image does not have Java installed, which means you can not use Java stored procedures
# such as "SDO_GEOMETRY('POINT (1.0 2.0)', 4326))"
docker pull gvenzl/oracle-xe:18.4.0

# start the dockerized oracle-xe instance (the container will be destroyed/removed on stopping)
# this container can be stopped using: docker stop geoserver
# start with user/credentials (user/password = system/oracle)
docker run --rm -p 1521:1521 --name geoserver -h geoserver -e ORACLE_PASSWORD=oracle -d gvenzl/oracle-xe:18.4.0

printf "\n\nStarting Oracle XE container, this could take a few minutes..."
printf "\nWaiting for Oracle XE database to start up.... "
_WAIT=0;
while :
do
    printf " $_WAIT"
    if $(docker logs geoserver | grep -q 'DATABASE IS READY TO USE!'); then
        printf "\nOracle XE Database started\n\n"
        break
    fi
    sleep 10
    _WAIT=$(($_WAIT+10))

    if (($_WAIT > 300)); then
        printf "\nWaited 300 seconds for Oracle XE database, giving up.\n\n"
        break
    fi
done

# print logs
docker logs geoserver
