# GeoServer Access Control List authorization 

[GeoServer ACL](https://github.com/geoserver/geoserver-acl) is an advanced authorization system for GeoServer.

The source code for this plugin resides on the [geoserver-acl](https://github.com/geoserver/geoserver-acl/tree/main/src/plugin) project code base,
where it's [tested](https://github.com/geoserver/geoserver-acl/actions/workflows/build-plugin.yaml) against the stable, development, and maintenance
versions of GeoServer. 

This community module hence only runs a [Testcontainers](https://testcontainers.com/) based integration test suite, using the
[geoservercloud/geoserver-acl](https://hub.docker.com/r/geoservercloud/geoserver-acl/tags) Docker image for the dependency version defined in
this project's `pom.xml`.

## Build

From the GeoServer git clone root:

```
cd src
mvn clean install -Pacl
```

or

```
cd src/community/acl
mvn clean install
```

During the `integration-tests` maven phase, if there's a Docker environment available, the integration tests
will run, otherwise they'll be skipped.

Use `mvn verify` to run the tests without installing the jar file to the local maven repo.

## Usage

The plugin `.zip` file is to be installed as usual, unzipping it inside the GeoServer `WEB-INF/lib` folder.

The GeoServer ACL service is distributed as a Docker image in dockerhub: [geoservercloud/geoserver-acl](https://hub.docker.com/r/geoservercloud/geoserver-acl/tags) 

For the plugin to work in GeoServer, you need a running GeoServer ACL service.

Please check the accompanying [compose.yml](./compose.yml) example docker composition for a complete set up. 

## Plugin Configuration

The GeoServer ACL plugin requires GeoServer to be run with some System properties or environment variables to
set up the ACL API client target URL and credentials.

### System Properties

If you choose to use Java System properties, add the following ones to the GeoServer JVM parameters,
the appropriate values:

```
-Dgeoserver.acl.client.basePath=https://example.com/acl/api
-Dgeoserver.acl.client.username=geoserver
-Dgeoserver.acl.client.password=ch4ng3m3
```

#### Disabling the plugin

The `geoserver.acl.enabled` config property defaults to `true` so it's not required for the plugin to run.
In order to completely disabling the plugin, set it to `false`:

```
-Dgeoserver.acl.enabled=false
```

### Environment variables

In production environments it's usually more convenient to control configuration options through
environment variables, where sentitive values can be obtained from docker/kubernetes secrets.

All the system properties mentioned above can be mapped to environment variables by changing their
names to upper case and replacing dots by underscores:

For example:

```
export GEOSERVER_ACL_ENABLED=false
export GEOSERVER_ACL_CLIENT_BASEPATH=https://example.com/acl/api
export GEOSERVER_ACL_CLIENT_USERNAME=geoserver
export GEOSERVER_ACL_CLIENT_PASSWORD=ch4ng3m3
```

And so on.

### Setting the plugin's authorization cache expiration time

The GeoServer ACL API plugin implements a local cache for authorization requests/responses, that has
a default `30` seconds expiration time.

The cache "expiration time", or "time to live", can be changed through the `Dgeoserver.acl.client.cache.ttl`
configuration property, which accepts a `java.time.Duration` string. For example:

```
-Dgeoserver.acl.client.cache.ttl=PT5S
```

or

```
export GEOSERVER_ACL_CLIENT_CACHE_TTL=PT5S
```

will set the cache TTL to `5` seconds.

Examples:

```
    "PT20.345S" -- parses as "20.345 seconds"
    "PT15M"     -- parses as "15 minutes" (where a minute is 60 seconds)
    "PT10H"     -- parses as "10 hours" (where an hour is 3600 seconds)
    "P2D"       -- parses as "2 days" (where a day is 24 hours or 86400 seconds)
    "P2DT3H4M"  -- parses as "2 days, 3 hours and 4 minutes"
```


This is **important** becaue the cache TTL will impact the latency for GeoServer to respond to data access
or workspace admin access rules modified or deleted directly through the ACL REST API that happen outside GeoServer.

If you're implementing a workflow where you're managing ACL rules directly through the ACL service API,
you might want to set a shorter cache TTL.

Alternatively, you can introduce a direct call to the GeoServer REST API to force clearing the cache using
the `/rest/reset` endpoint. For example:

```
curl -u admin:geoserver -X POST "http://localhost:8080/geoserver/rest/reset"
```

The web user interface can also be used to clear out the caches through the "Server Status" page, clicking
the `Resource Cache -> Clear` button.

In either case, you'd see a message like the following in the GeoServer logs, provider the logging configuration
enables the info level for the `org.geoserver.acl.authorization.cache` topic:

```
INFO   [org.geoserver.acl.authorization.cache] - evicted 56 cached ACL authorizations
```

> Note the cache time to live is not a problem when the plugin runs in [GeoServer Cloud](https://github.com/geoserver/geoserver-cloud),
because the ACL Service integrates with the *GeoServer Cloud* event bus, and notifies all the running pods when a data access
or admin acccess rule is changed, and the GeoServer microservices react immediately clearing out the authorization cache.

