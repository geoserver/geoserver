# Application schemas

The application schema support (app-schema) extension provides support for `app-schema.complex-features`{.interpreted-text role="ref"} in GeoServer WFS.

!!! note

    You must install the app-schema plugin to use Application Schema Support.

GeoServer provides support for a broad selection of simple feature data stores, including property files, shapefiles, and JDBC data stores such as PostGIS and Oracle Spatial. The app-schema module takes one or more of these simple feature data stores and applies a mapping to convert the simple feature types into one or more complex feature types conforming to a GML application schema.

![](app-schema.png)
*Three tables in a database are accessed using GeoServer simple feature support and converted into two complex feature types.*

The app-schema module looks to GeoServer just like any other data store and so can be loaded and used to service WFS requests. In effect, the app-schema data store is a wrapper or adapter that converts a simple feature data store into complex features for delivery via WFS. The mapping works both ways, so queries against properties of complex features are supported.

<div class="grid cards" markdown>

-   [Complex Features](complex-features.md)
-   [Installation](installation.md)
-   [WFS Service Settings](wfs-service-settings.md)
-   [Configuration](configuration.md)
-   [Mapping File](mapping-file.md)
-   [Application Schema Resolution](app-schema-resolution.md)
-   [Supported GML Versions](supported-gml-versions.md)
-   [Secondary Namespaces](secondary-namespaces.md)
-   [CQL functions](cql-functions.md)
-   [Property Interpolation](property-interpolation.md)
-   [Data Stores](data-stores.md)
-   [Feature Chaining](feature-chaining.md)
-   [Polymorphism](polymorphism.md)
-   [Data Access Integration](data-access-integration.md)
-   [WMS Support](wms-support.md)
-   [WFS 2.0 Support](wfs-2.0-support.md)
-   [Joining Support For Performance](joining.md)
-   [Tutorial](tutorial.md)
-   [MongoDB Tutorial](mongo-tutorial.md)
-   [Apache Solr Tutorial](solr-tutorial.md)

</div>
