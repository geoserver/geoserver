# GeoServer Java REST client

A GeoServer REST API Java client library based on `openapi-generator-maven-plugin` generated code.

API definition files are on the [openapi/1.0.0/](openapi/1.0.0/) directory.

This folder contains the OpenAPI 3 definition files for GeoServer's REST API v1.

As a difference with the legacy Swagger 2 definitions, these ones focus only in inter-process
communication using `JSON` for request/response encoding.

This API is developed using the Swagger 2 files as base, but carefully inspecting GeoServer's REST config server
implementation, often debugging it, in order to comply with the actual API as much as possible.

The generated code is held at:

* [generated/model](generated/model), for Catalog and Config object models
* [generated/model](generated/feign-client), for [OpenFeign](https://github.com/OpenFeign/feign) based java client

The [java-client/](java-client) project contains the client library built as a wrapper over the generated feign client, to simplify its usage.

## Usage

### Maven

Add the following dependency to your Java project to interact with GeoServer's REST API:

```xml
      <dependency>
        <groupId>org.geoserver.community</groupId>
        <artifactId>gs-rest-openapi-java-client</artifactId>
        <version>${gs.version}</version>
      </dependency>
```
### Java

```java
import org.geoserver.openapi.model.catalog.*; // generated model
import org.geoserver.restconfig.client.*; // client library that depends on generated openfeign client

....

String apiURL = "http://localhost:8080/geoserver/rest";
GeoServerClient client = new GeoServerClient()
                        .setBasePath(apiURL)
                        .setBasicAuth("admin", "geoserver");

//org.geoserver.openapi.model.catalog.WorkspaceInfo, not org.geoserver.catalog.WorkspaceInfo...
WorkspaceInfo ws = client.workspaces().create("ws");

Map<String, String> connectionParams = ...
DataStoreInfo ds = new DataStoreInfo()
        .name("test")
        .enabled(true)
        .workspace(ws)
        connectionParameters(connectionParams);
        
client.dataStores().create(ds);

```

## Run integration tests:

```
mvn verify -Pdocker
```

Will launch the geoserver docker container during maven's `pre-integration-tests` phase, run the tests at
`integration-test`, and shut the container down at `post-integration-tests`.

### Run from an IDE:

Have the GeoServer docker container running before executing the tests from the IDE:

```
docker run -it --rm --name gstests -p18080:8080 oscarfonts/geoserver:latest
```

Or rather

```
docker-compose -f docker-compose.yml up -d
```

The integration tests will look for an environment variable named `geoserver_api_url` to connect to, defaulting to `http://localhost:18080/geoserver/rest`.

If GeoServer is running at a different address, pass the env variable to the test run configuration on the IDE:

```
-Dgeoserver_api_url=http://localhost:18080/geoserver/rest
```

