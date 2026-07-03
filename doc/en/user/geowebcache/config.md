# Configuration

GeoWebCache is automatically configured for use with GeoServer using the most common options, with no setup required. All communication between GeoServer and GeoWebCache happens by passing messages inside the JVM.

By default, all layers served by GeoServer will be known to GeoWebCache. See the [Tile Layers](webadmin/layers.md) page to test the configuration.

!!! note
    Version 2.2.0 of GeoServer introduced changes to the configuration of the integrated GeoWebCache.

## Integrated user interface

GeoWebCache has a full integrated web-based configuration. See the [GeoWebCache settings](webadmin/index.md) section in the [Web administration interface](../webadmin/index.md).

## Determining tiled layers

In versions of GeoServer prior to 2.2.0, the GeoWebCache integration was done in a such way that every GeoServer layer and layer group was forced to have an associated GeoWebCache tile layer. In addition, every such tile layer was forcedly published in the EPSG:900913 and EPSG:4326 gridsets with PNG and JPEG output formats.

It is possible to selectively turn caching on or off for any layer served through GeoServer. This setting can be configured in the [Tile Layers](webadmin/layers.md) section of the [Web administration interface](../webadmin/index.md).

## Configuration files

It is possible to configure most aspects of cached layers through the [GeoWebCache settings](webadmin/index.md) section in the [Web administration interface](../webadmin/index.md) or the [GeoWebCache REST API](rest/index.md).

GeoWebCache keeps the configuration for each GeoServer tiled layer separately, inside the **`<data_dir>/gwc-layers/`** directory. There is one XML file for each tile layer. These files contain a different syntax from the `<wmsLayer>` syntax in the standalone version and are *not* meant to be edited by hand. Instead you can configure tile layers on the [Tile Layers](webadmin/layers.md) page or through the [GeoWebCache REST API](rest/index.md).

Configuration for the defined gridsets is saved in **`<data_dir>/gwc/geowebcache.xml`**` so that the integrated GeoWebCache can continue to serve externally-defined tile layers from WMS services outside GeoServer.

If upgrading from a version prior to 2.2.0, a migration process is run which creates a tile layer configuration for all the available layers and layer groups in GeoServer with the old defaults. From that point on, you should configure the tile layers on the [Tile Layers](webadmin/layers.md) page.

## Changing the cache directory

GeoWebCache will automatically store cached tiles in a `gwc` directory inside your GeoServer data directory. To set a different directory, stop GeoServer (if it is running) and add the following code to your GeoServer **`web.xml`** file (located in the **`WEB-INF`** directory):

```xml
<context-param>
   <param-name>GEOWEBCACHE_CACHE_DIR</param-name>
   <param-value>C:\temp</param-value>
</context-param>
```

Change the path inside `<param-value>` to the desired cache path (such as **`C:\temp`** or **`/tmp`**). Restart GeoServer when done.

!!! note
    Make sure GeoServer has write access in this directory.

## GeoWebCache with multiple GeoServer instances

For stability reasons, it is not recommended to use the embedded GeoWebCache with multiple GeoServer instances. If you want to configure GeoWebCache as a front-end for multiple instances of GeoServer, we recommend using the [standalone GeoWebCache](https://geowebcache.osgeo.org).

## GeoServer Data Security {: #gwc_data_security }

GWC Data Security is an option that can be turned on and turned off through the [Caching defaults](webadmin/defaults.md) page. By default it is turned off.

When turned on, the embedded GWC enforces GeoServer data security on every tile request:

- **Layer access**: users without access to a layer receive a "layer not found" error (HIDE mode) or an authentication challenge (CHALLENGE mode), consistent with regular WMS behavior.

- **Spatial access limits**: when a user's access is restricted to a geographic area, tiles outside that area are rendered as empty transparent tiles rather than being rejected with an error. This is consistent with the regular WMS, which filters data before serving it.

- **Security-aware tile caching**: when a user has data access restrictions (filters, geometry clips), GeoWebCache caches their tiles separately from unrestricted users and from users with different restrictions. The separation is based on the *access profile* (the actual restriction rules), not on the user identity. Two users with identical restrictions share the same cached tiles; users with different restrictions get independent cache entries.

The tile cache separation is implemented by injecting a synthetic `ACCESS_LIMITS_KEY` parameter into the tile request before it reaches the cache. The value is a compact JSON object encoding the active access restrictions. For example, a user restricted to features where `NAME = 'Blue Lake'` would carry:

  ```
  ACCESS_LIMITS_KEY={"readFilter":"NAME = 'Blue Lake'"}
  ```

A user clipped to a raster region in Australia would carry:

  ```
  ACCESS_LIMITS_KEY={"rasterFilter":"MULTIPOLYGON (((140 -50, 150 -50, 150 -30, 140 -30, 140 -50)))"}
  ```

Unrestricted users have no `ACCESS_LIMITS_KEY` in their tile parameters and continue to use the shared default cache. GWC hashes all tile parameters into the `parametersId` used to locate tiles in the cache storage, so tiles with different `ACCESS_LIMITS_KEY` values are stored at different cache paths even if they cover the same bounding box.

When access restrictions involve complex geometries or long filter expressions, the `ACCESS_LIMITS_KEY` value can become large. GeoWebCache limits it to **64 KB** by default. If the serialized key exceeds this limit, individual field values are truncated (longest first) until the total fits. Each truncated value is replaced with a visible prefix followed by `...too long, sha is <sha256hex>`, where the SHA-256 is computed from the full original value. The limit can be overridden with the system property `gwc.security.maxKeyLength` (value in characters).

!!! note
    The `ACCESS_LIMITS_KEY` parameter is injected at runtime and is **never written** to the gwc-layers XML configuration files (`<data_dir>/gwc-layers/`). It will not appear there when inspecting layer configuration. To observe it however one can check out the property files collecting the filter parameter values for the various tile caches, found in each tile layer folder.

### Security tags and targeted invalidation

When access rules change, the tiles cached under the old restrictions become stale. The [disk quota](webadmin/diskquotas.md) eventually reclaims them, but a security subsystem can also trigger immediate invalidation of just the affected tiles.

To make that targeted, GeoWebCache injects a second synthetic parameter, `SECURITY_TAGS_KEY`, alongside `ACCESS_LIMITS_KEY` whenever the access manager attaches *security tags* to the computed restrictions. The value is a comma-separated, sorted list of opaque tags (for example rule identifiers or role names) describing which security rules shaped the restriction:

  ```
  SECURITY_TAGS_KEY=role:planner,rule:42
  ```

`SECURITY_TAGS_KEY` is only present when `ACCESS_LIMITS_KEY` is also present (tags without a restriction are meaningless), and like it is injected at runtime, not persisted.

When the security subsystem signals that a rule changed, GeoWebCache drops the cached tiles associated to the modified security tags, leaving unrelated security-keyed tiles intact. Access managers that do not attach tags fall back to dropping all security-keyed tiles for the affected layers. Tiles without an `ACCESS_LIMITS_KEY` (unrestricted access) are never touched. Layer-group tiles are dropped too when one of their member layers is affected.

At the time of writing **no security subsystem ships an implementation that drives this eager removal**: stale tiles are reclaimed by disk quota until one does. Current or third-party access managers can choose to eagerly drop tiles; check their documentation to confirm.

!!! note
    This invalidation works only in a live, user-driven deployment where security changes flow through the running instance. Changes applied out of band (database restore, bulk imports, environment promotion) produce no invalidation; disk quota remains the cleanup mechanism in those cases. In a clustered deployment with per-node local disk caches, an invalidation reaches only the node that received it; a shared blob store (S3, Azure, NFS) does not have this limitation.

