# REST

GeoServer provides a [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer) interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the [Web administration interface](../webadmin/index.md).

REST is an acronym for "[REpresentational State Transfer](http://en.wikipedia.org/wiki/Representational_state_transfer)". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP: GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as `http://GEOSERVER_HOME/rest/workspaces/topp`.

## API

!!! warning

    The API is documented as Swagger 2.0 files. However, these files have been written by hand back in 2017, and have not always been kept up to date with the evolution of the GeoServer configuration object structure. Also, they have not been tested for proper client generation, and will likely not work for that purpose. Take them only as a form of documentation.

The following links provide direct access to the GeoServer REST API documentation, including definitions and examples of each endpoint:

-   [/about/manifests](https://docs.geoserver.org/latest/en/api/#1.0.0/manifests.yaml)
-   [/about/system-status](https://docs.geoserver.org/latest/en/api/#1.0.0/system-status.yaml)
-   [/datastores](https://docs.geoserver.org/latest/en/api/#1.0.0/datastores.yaml)
-   [/coverages](https://docs.geoserver.org/latest/en/api/#1.0.0/coverages.yaml)
-   [/coveragestores](https://docs.geoserver.org/latest/en/api/#1.0.0/coveragestores.yaml)
-   [/featuretypes](https://docs.geoserver.org/latest/en/api/#1.0.0/featuretypes.yaml)
-   [/fonts](https://docs.geoserver.org/latest/en/api/#1.0.0/fonts.yaml)
-   [/layergroups](https://docs.geoserver.org/latest/en/api/#1.0.0/layergroups.yaml)
-   [/layers](https://docs.geoserver.org/latest/en/api/#1.0.0/layers.yaml)
-   [/logging](https://docs.geoserver.org/latest/en/api/#1.0.0/logging.yaml)
-   [/monitoring](https://docs.geoserver.org/latest/en/api/#1.0.0/monitoring.yaml)
-   [/namespaces](https://docs.geoserver.org/latest/en/api/#1.0.0/namespaces.yaml)
-   [/services/wms|wfs|wcs|wmts/settings](https://docs.geoserver.org/latest/en/api/#1.0.0/owsservices.yaml)
-   [/reload](https://docs.geoserver.org/latest/en/api/#1.0.0/reload.yaml)
-   [/resource](https://docs.geoserver.org/latest/en/api/#1.0.0/resource.yaml)
-   [/security](https://docs.geoserver.org/latest/en/api/#1.0.0/security.yaml)
-   [/settings](https://docs.geoserver.org/latest/en/api/#1.0.0/settings.yaml)
-   [/structuredcoverages](https://docs.geoserver.org/latest/en/api/#1.0.0/structuredcoverages.yaml)
-   [/styles](https://docs.geoserver.org/latest/en/api/#1.0.0/styles.yaml)
-   [/templates](https://docs.geoserver.org/latest/en/api/#1.0.0/templates.yaml)
-   [/transforms](https://docs.geoserver.org/latest/en/api/#1.0.0/transforms.yaml)
-   [/wmslayers](https://docs.geoserver.org/latest/en/api/#1.0.0/wmslayers.yaml)
-   [/wmsstores](https://docs.geoserver.org/latest/en/api/#1.0.0/wmsstores.yaml)
-   [/wmtslayers](https://docs.geoserver.org/latest/en/api/#1.0.0/wmtslayers.yaml)
-   [/wmtsstores](https://docs.geoserver.org/latest/en/api/#1.0.0/wmtsstores.yaml)
-   [/workspaces](https://docs.geoserver.org/latest/en/api/#1.0.0/workspaces.yaml)
-   [/usergroup](https://docs.geoserver.org/latest/en/api/#1.0.0/usergroup.yaml)
-   [/roles](https://docs.geoserver.org/latest/en/api/#1.0.0/roles.yaml)
-   GeoWebCache:
    -   [/blobstores](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcblobstores.yaml)
    -   [/bounds](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcbounds.yaml)
    -   [/diskquota](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcdiskquota.yaml)
    -   [/filterupdate](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcfilterupdate.yaml)
    -   [/global](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcglobal.yaml)
    -   [/gridsets](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcgridsets.yaml)
    -   [/index](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcindex.yaml)
    -   [/layers](https://docs.geoserver.org/latest/en/api/#1.0.0/gwclayers.yaml)
    -   [/masstruncate](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcmasstruncate.yaml)
    -   [/statistics](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcmemorycachestatistics.yaml)
    -   [/reload](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcreload.yaml)
    -   [/seed](https://docs.geoserver.org/latest/en/api/#1.0.0/gwcseed.yaml)
-   Importer extension:
    -   [/imports](https://docs.geoserver.org/latest/en/api/#1.0.0/importer.yaml)
    -   [/imports (tasks)](https://docs.geoserver.org/latest/en/api/#1.0.0/importerTasks.yaml)
    -   [/imports (transforms)](https://docs.geoserver.org/latest/en/api/#1.0.0/importerTransforms.yaml)
    -   [/imports (data)](https://docs.geoserver.org/latest/en/api/#1.0.0/importerData.yaml)
-   Monitor extension:
    -   [/monitor](https://docs.geoserver.org/latest/en/api/#1.0.0/monitoring.yaml)
-   XSLT extension:
    -   [/services/wfs/transforms](https://docs.geoserver.org/latest/en/api/#1.0.0/transforms.yaml)

!!! note

    You can also view the original [REST configuration API reference](api/index.md) section.

## Examples

This section contains a number of examples which illustrate some of the most common uses of the REST API. They are grouped by endpoint.
