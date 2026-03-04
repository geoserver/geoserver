# GeoServer data directory

The GeoServer **data directory** is the location in the file system where GeoServer stores its configuration information.

The configuration defines what data is served by GeoServer, where it is stored, and how services interact with and serve the data. The data directory also contains a number of support files used by GeoServer for various purposes.

For production use, it is **highly** recommended to define an *external* data directory (outside the application) both for security reasons and to make it easier to upgrade. The [Setting the data directory location](setting.md) section describes how to configure GeoServer to use an existing data directory.

!!! note

    Since GeoServer provides both an interactive interface (via the [web admin interface](../webadmin/index.md)) and programmatic interface (through the [REST API](../rest/index.md)) to manage configuration, most users do not need to know about the [internal structure of the data directory](structure.md), but an overview is provided below.

<div class="grid cards" markdown>

- [DatadirectoryLocation](location.md)
- [DatadirectorySetting](setting.md)
- [DatadirectoryStructure](structure.md)
- [DatadirectoryMigrating](migrating.md)
- [DatadirectoryConfigtemplate](configtemplate.md)

</div>
