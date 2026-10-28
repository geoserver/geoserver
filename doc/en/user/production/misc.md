# Other Considerations

## Host your application separately

GeoServer includes a few examples in the demo section of the [Web administration interface](../webadmin/index.md)
and provides a `www` folder.

For production instances, we recommend against this bundling of your application. To make upgrades and troubleshooting easier, please use a separate container for your application. It is perfectly fine, though, to use one container manager (such as Tomcat or Jetty) to host both GeoServer and your application.

## Publish your server's capabilities documents

In order to make it easier to find your data, your service must be available.

- Put a link to your capabilities document somewhere on the web. This will ensure that a search engine will crawl and index it.

- If your GeoServer is operating as part of a "Spatial Data Infrastructure" it may be registered with a catalogue service.

  Catalogue services such as [GeoNetwork-opensource](http://geonetwork-opensource.org/) or [PyCSW](https://pycsw.org) can harvest GeoServer listing the web services and layers published allowing content to be more easily browsed and searched.

## Proxy your server

GeoServer can have the capabilities documents properly report a proxy. You can configure this in the Server configuration section of the [Web administration interface](../webadmin/index.md) and entering the URL of the external proxy in the field labeled `Proxy base URL`.

### Client address behind a reverse proxy

GeoServer resolves the client address like the Tomcat remote IP valve, with a smaller default. Tomcat trusts proxies on private addresses through its `internalProxies` parameter. GeoServer only trusts loopback (`127.0.0.0/8`, `::1`).

The `GEOSERVER_TRUSTED_PROXIES` [application property](../configuration/properties/index.md) is a comma separated list of reverse proxy IP addresses or CIDR blocks. GeoServer reads the client address from the `X-Forwarded-For` header only for requests arriving from one of these proxies, otherwise it uses the direct connection address. A reverse proxy on another host must be listed, otherwise every request appears to come from the proxy address.

For example, with a reverse proxy at `10.0.0.5`:

```shell
CATALINA_OPTS="${CATALINA_OPTS} -DGEOSERVER_TRUSTED_PROXIES=10.0.0.5"
```

As an application property, it can also be set as an environment variable or a `web.xml` context parameter.

Setting the property replaces the loopback default, so list `127.0.0.1` too if a proxy on the same host is still used.

With `GEOSERVER_TRUSTED_PROXIES=10.0.0.5`, GeoServer sees these client addresses:

| Connection from | `X-Forwarded-For` | Client address | Why |
|---|---|---|---|
| `203.0.113.7` | none | `203.0.113.7` | Direct connection |
| `10.0.0.5` | `198.51.100.20` | `198.51.100.20` | Trusted proxy |
| `203.0.113.7` | `10.1.2.3` | `203.0.113.7` | Not a trusted proxy, header ignored and a warning logged |
| `10.0.0.5` | `10.1.2.3, 198.51.100.20` | `198.51.100.20` | The proxy appended the real client; the hop on the left came from the client and is not used |

With several proxies in a chain, list each of them. The right-most address in the header that is not a trusted proxy is used as the client address.

When the proxy is not listed, logs, monitoring, control flow and IP-based security rules see the proxy address for every request. GeoServer logs a warning the first time a request carries `X-Forwarded-For` from a host that is not listed.

**Important:** The proxy must overwrite `X-Forwarded-For` or append the client address to it. A proxy that passes the header on unchanged lets the client choose its own address, for example to skip the brute force delay with a whitelisted IP. Nginx does this unless `proxy_set_header X-Forwarded-For` is configured. For example, to append the client address:

```
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

#### Docker and Kubernetes

In Docker, GeoServer sees the proxy address on the container network, not `127.0.0.1`. A proxy on the Docker host appears as the bridge gateway, such as `172.17.0.1`. A proxy in another container appears as that container's address. Docker Compose creates a separate network per project, use `docker network inspect` to find its subnet and list it:

```shell
docker run -e GEOSERVER_TRUSTED_PROXIES=172.18.0.0/16 ...
```

```yaml
services:
  geoserver:
    environment:
      GEOSERVER_TRUSTED_PROXIES: 172.18.0.0/16
```

Until the proxy is listed, an `ip=` control flow limit acts as one limit for the whole server, and a blacklist entry for that address blocks everyone.

On Kubernetes, list the address range of the ingress controller pods. To see the real client address, the Service in front of the ingress controller needs `externalTrafficPolicy: Local`.

#### Customized web.xml

The client address is resolved by the `Remote IP Resolution` filter in GeoServer's `WEB-INF/web.xml`, so this is the same for Tomcat, Jetty and other servlet containers. When upgrading with a customized `web.xml`, add the filter:

```xml
<filter>
    <filter-name>Remote IP Resolution</filter-name>
    <filter-class>org.geoserver.filters.RemoteIpFilter</filter-class>
</filter>
```

And map it right after the `Set Character Encoding` filter mapping, so the client address is resolved before any other filter reads it:

```xml
<filter-mapping>
    <filter-name>Remote IP Resolution</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

Without this filter GeoServer ignores `X-Forwarded-For`, and sees the proxy address for every request.

## Clustering

Setting up a [Cluster](http://en.wikipedia.org/wiki/Cluster_(computing)) is one of the best ways to improve the reliability and performance of your GeoServer installation.

There are a variety of techniques to configure a cluster, including at the container level, the virtual machine level, and the physical server level.

This results in running multiple GeoServer nodes behind a common proxy or load balancer:

1.  The `PROXY_BASE_URL` setting allows all nodes in the cluster operate as the same web service.
2.  Optional: Provide settings to identify [Make cluster nodes identifiable from the GUI](identify.md) individual nodes.
3.  Optional: Use [Overriding the log location setup in the GeoServer configuration](../configuration/logging.md#logging_location) settings to direct logs from individual nodes to different files.
4.  While most web services are stateless, some functionality like importer or WPS may require the use of an extension to share state between nodes.

    * WPS clustering has extensions for Hazelcast and JDBC (shared database).

    * Importer provides an extension to establish a Berkley shared database.

5.  The challenge to operating a cluster is the management of changes to the data directory configuration to ensure all the nodes are operating with the same configuration and produce the same results.

    - Using a shared file system to allow nodes to share a common data directory.
    - Using version control or a layer of a docker image to share a data directory.

    To support these workflows teams often direct `geoserver/web` requests to a single node, and use the REST API to restart other nodes when a configuration change is ready.

The limitation of these approach is the startup time associated with restarting nodes. Several community modules exist to address this limitation:

- The project [Cloud Native GeoServer](https://github.com/geoserver/geoserver-cloud) breaks the GeoServer application into microservices that may be scaled by Kubernetes.

  This prevents the needs to restart nodes, but requires a microservice environment.

- The community module <!-- BROKEN LINK: ../-broken.md --> uses a bus to communicate configuration changes in real time between all the nodes in a cluster.

  Each mode is updated when configuration changes are made, preventing the need to restart nodes, while providing full web service performance.

- The community module [JDBCConfig](../community/jdbcconfig/index.md) allows the configuration to be stored in a database. This is best combined with the community module [JDBCStore](../community/jdbcstore/index.md) which allows the icons, fonts and styles to also be stored in a database.

  Each node checks for changes during operation, preventing the need to restart nodes, with an impact to web service performance.
