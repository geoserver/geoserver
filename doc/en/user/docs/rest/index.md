# REST

GeoServer provides a [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer) interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the [Web administration interface](../webadmin/index.md).

REST is an acronym for "`REpresentational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`_". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP: GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as `http://GEOSERVER_HOME/rest/workspaces/topp`.

## API

!!! warning

    The API is documented as Swagger 2.0 files. However, these files have been written by hand back in 2017, and have not always been kept up to date with the evolution of the GeoServer configuration object structure. Also, they have not been tested for proper client generation, and will likely not work for that purpose. Take them only as a form of documentation.

The following links provide direct access to the GeoServer REST API documentation, including definitions and examples of each endpoint:

- [/about/manifests](api/manifests.md)
- [/about/system-status](api/system-status.md)
- [/datastores](api/datastores.md)
- [/coverages](api/coverages.md)
- [/coveragestores](api/coveragestores.md)
- [/featuretypes](api/featuretypes.md)
- [/fonts](api/fonts.md)
- [/layergroups](api/layergroups.md)
- [/layers](api/layers.md)
- [/logging](api/logging.md)
- [/monitoring](api/monitoring.md)
- [/namespaces](api/namespaces.md)
- [/services/wms|wfs|wcs|wmts/settings](api/owsservices.md)
- [/reload](api/reload.md)
- [/resource](api/resource.md)
- [/security](api/security.md)
- [/settings](api/settings.md)
- [/structuredcoverages](api/structuredcoverages.md)
- [/styles](api/styles.md)
- [/templates](api/templates.md)
- [/transforms](api/transforms.md)
- [/wmslayers](api/wmslayers.md)
- [/wmsstores](api/wmsstores.md)
- [/wmtslayers](api/wmtslayers.md)
- [/wmtsstores](api/wmtsstores.md)
- [/workspaces](api/workspaces.md)
- [/urlchecks](api/urlchecks.md)
- [/usergroup](api/usergroup.md)
- [/roles](api/roles.md)
- [/filterChains](api/filterchains.md)
- [/authFilters](api/authenticationfilterconfiguration.md)
- [/authProviders](api/authenticationproviders.md)
- [/usergroupservices](api/usergroupservices.md)
- GeoWebCache:
  - [/blobstores](api/gwcblobstores.md)
  - [/bounds](api/gwcbounds.md)
  - [/diskquota](api/gwcdiskquota.md)
  - [/filterupdate](api/gwcfilterupdate.md)
  - [/global](api/gwcglobal.md)
  - [/gridsets](api/gwcgridsets.md)
  - [/index](api/gwcindex.md)
  - [/layers](api/gwclayers.md)
  - [/masstruncate](api/gwcmasstruncate.md)
  - [/reload](api/gwcreload.md)
  - [/seed](api/gwcseed.md)
- Importer extension:
  - [/imports](api/importer.md)
  - [/imports (tasks)](api/importerTasks.md)
  - [/imports (transforms)](api/importerTransforms.md)
  - [/imports (data)](api/importerData.md)
- Monitor extension:
  - [/monitor](api/monitoring.md)
- XSLT extension:
  - [/services/wfs/transforms](api/transforms.md)

!!! note

    You can also view the original [REST configuration API reference](api/index.md) section.

## Examples

This section contains a number of examples which illustrate some of the most common uses of the REST API. They are grouped by endpoint.
