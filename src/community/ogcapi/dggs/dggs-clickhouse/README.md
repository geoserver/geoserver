
## RUNNING ONLINE TESTS

Running the online tests requires the following:

* The python portion of [JEP](https://github.com/ninia/jep) needs to be installed with ``pip``
* The [rHEALPixDGGS libraries](https://pypi.org/project/rHEALPixDGGS) with ``pip``
* Start a ClickHouse docker image, e.g. (ClickHouse with ``default`` user having no password):

```
docker run  --ulimit nofile=262144:262144  -e CLICKHOUSE_DB=gttest   -p 8123:8123/tcp --name clickhouse clickhouse/clickhouse-server
```

Finally, add a couple of property files to enable the online tests:

```
> cat /home/<yourUserName>/.geotools/clickhouse.properties 
#This is an example fixture. Update the values and remove the .example suffix to enable the test
#Thu Oct 01 17:06:27 CEST 2020
user=default
port=8123
passwd=
password=
url=jdbc\:clickhouse\://localhost\:8123/gttest
host=localhost
database=gttest
driver=com.clickhouse.jdbc.ClickHouseDriver
```

and:

```
cat /home/aaime/.geotools/clickhouse-dggs-rHEALPix.properties 
user=default
port=8123
password=
url=jdbc\:clickhouse\://localhost\:8123/gttest
dggs_id=rHEALPix
host=localhost
database=gttest
driver=com.clickhouse.jdbc.ClickHouseDriver
```

At this point it should be possible to successfully run the tests in gs-dggs-clickhouse.