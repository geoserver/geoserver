# OGC API Features - Conformance Test Suite execution

These are the instructions on how to run the CITE Conformance Test Suite for the
*GeoServer* OGC API Features service.

## Goals

* To run them manually on demand in your local machine
* To run them while you're debugging GeoServer in your IDE
* To run them in an automated CI/CD pipeline (Github actions workflow), and to upload the reports somewhere

## Requirements

* A *GeoServer* instance with the `ogcapi-features` extension
* A data set to run the conformance tests against 

## Test Data:

The datasets included for the default test runs are the vector layers from
the GeoServer *release** data directory.

### Limited CRS list advertised per FeatureType

Each `FeatureType` to be tested must have a limited set of advertised CRS's where for which
coordinate reprojection is expected to work. Otherwise the CRS tests will be executed
against all of them.

For example, querying a collection:

```json
...
  "crs": [
    "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
    "http://www.opengis.net/def/crs/EPSG/0/4326",
    "http://www.opengis.net/def/crs/EPSG/0/3857"
  ],
  ...
```
  
as opposed to advertising all the CRS's available in GeoServer.

In order to automate this, the `release` data directory is configured with `EPSG:4326` and `EPSG:3857` as the
advertised CRS's directly in the global `WFS` settings.


## Running

For the most common case, you'll want to run the `ogcapi-features10` suite against a GeoServer instance from the local `.war` build.

First, run 

```shell
make war
```
to build the `geoserver.war` file from `src/web/app`, and

```
make build suite=ogcapi-features10
```

To build the GeoServer Docker image that will be exercised. Then run:

```shell
make test suite=ogcapi-features10
```

These are the same steps the "Run CITE Tests" github actions workflow will follow.

At the end of each test run, a human readable summary of test failures will be printed out, if any.

Finally, shut down the docker composition and clean up the `logs/` folder with

```shell
make clean
```


## Test a local instance

> ⚠️ **Achtung!**:
>
> In the following examples, some `make` targets receive an `iut` parameter with the URL of the OGC Features API landing page to test,
> external to the `teamengine`'s container network. By default, for **Linux** systems, use the <strong><code>172.17.0.1</code></strong> IP address.
> However, if you're running the tests on **MacOS**, replace it with the <strong><code>host.docker.internal</code></strong> hostname instead.
> This difference exists because on Linux, Docker creates a bridge network where the host is accessible via `172.17.0.1`. On MacOS, Docker Desktop for Mac
> runs containers within a virtualization layer, which changes the networking model. As a result, `host.docker.internal` is used to enable containers
> to access the host.


For development purposes you may want to run the `Start.java` GeoServer launcher from the `gs-web-app`'s test sources.

In order to reproduce the Github action CITE tests results, you'll have use the the data directory at `./release` (e.g. make a copy of
the release directory and set the `GEOSERVER_DATA_DIR` environment variable accordingly).

To hit that instance, if it's running at `localhost:8080`, you can simply run.


```shell
make ogcapi-features10-localhost
```

Otherwise, you can further customize the target GeoServer URL, port, and virtual service:

```shell
make test-external suite=ogcapi-features10 iut="http://172.17.0.1:9090/geoserver/sf/archsites/ogc/features/v1"
```

The `iut` parameter refers to the *Instance Under Test* and is the URL for the Landing Page document.

> **Note**
>
> Since `teamengine` is running inside a Docker container, the special IP address `172.17.0.1`
> can be used to reach out to the host as long as the container is running on the default
> Docker bridge network, which is the case in this docker composition.

Finally, shut down the docker composition and clean up the `logs/` folder with

```shell
make clean
```

### Use TeamEngine interactively

In some cases it's useful to log in to the TeamEngine and run tests manually.

You can check which services a given test suite is comprised of with the `make print-services` command:

```shell
make print-services suite=ogcapi-features10
Services used in the ogcapi-features10 test suite:
	geoserver:
  		image: ogccite/geoserver:ogcapi-features10
	teamengine:
  		image: ogccite/ets-ogcapi-features10:1.7.1-teamengine-5.4.1
```

In this case you can simply run the `teamengine` service with:

```shell
make start suite=ogcapi-features10 services=teamengine
```

Check the service is running:

```shell
docker compose ps
NAME                IMAGE                                                  COMMAND                  SERVICE      CREATED          STATUS                    PORTS
cite-teamengine-1   ogccite/ets-ogcapi-features10:1.7.1-teamengine-5.4.1   "catalina.sh jpda run"   teamengine   11 seconds ago   Up 11 seconds (healthy)   0.0.0.0:18080->8080/tcp
```

Then open [http://localhost:18080/teamengine/](http://localhost:18080/teamengine/) in your browser, sign in with
username and password `ogctest`, and start a new test session.


Once done, shut down the docker composition and clean up the `logs/` folder with

```shell
make clean
```
