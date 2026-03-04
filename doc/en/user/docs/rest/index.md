# REST

GeoServer provides a [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer) interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the [Web administration interface](../webadmin/index.md).

REST is an acronym for "`REpresentational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`_". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP: GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as `http://GEOSERVER_HOME/rest/workspaces/topp`.

## API

!!! warning

    The API is documented as Swagger 2.0 files. However, these files have been written by hand back in 2017, and have not always been kept up to date with the evolution of the GeoServer configuration object structure. Also, they have not been tested for proper client generation, and will likely not work for that purpose. Take them only as a form of documentation.

The following links provide direct access to the GeoServer REST API documentation, including definitions and examples of each endpoint:

- [/about/manifests](/api/manifests.yaml)
- [/about/system-status](/api/system-status.yaml)
- [/datastores](/api/datastores.yaml)
- [/coverages](/api/coverages.yaml)
- [/coveragestores](/api/coveragestores.yaml)
- [/featuretypes](/api/featuretypes.yaml)
- [/fonts](/api/fonts.yaml)
- [/layergroups](/api/layergroups.yaml)
- [/layers](/api/layers.yaml)
- [/logging](/api/logging.yaml)
- [/monitoring](/api/monitoring.yaml)
- [/namespaces](/api/namespaces.yaml)
- [/services/wms|wfs|wcs|wmts/settings](/api/owsservices.yaml)
- [/reload](/api/reload.yaml)
- [/resource](/api/resource.yaml)
- [/security](/api/security.yaml)
- [/settings](/api/settings.yaml)
- [/structuredcoverages](/api/structuredcoverages.yaml)
- [/styles](/api/styles.yaml)
- [/templates](/api/templates.yaml)
- [/transforms](/api/transforms.yaml)
- [/wmslayers](/api/wmslayers.yaml)
- [/wmsstores](/api/wmsstores.yaml)
- [/wmtslayers](/api/wmtslayers.yaml)
- [/wmtsstores](/api/wmtsstores.yaml)
- [/workspaces](/api/workspaces.yaml)
- [/urlchecks](/api/urlchecks.yaml)
- [/usergroup](/api/usergroup.yaml)
- [/roles](/api/roles.yaml)
- [/filterChains](/api/filterchains.yaml)
- [/authFilters](/api/authenticationfilterconfiguration.yaml)
- [/authProviders](/api/authenticationproviders.yaml)
- [/usergroupservices](/api/usergroupservices.yaml)
- GeoWebCache:
  - [/blobstores](/api/gwcblobstores.yaml)
  - [/bounds](/api/gwcbounds.yaml)
  - [/diskquota](/api/gwcdiskquota.yaml)
  - [/filterupdate](/api/gwcfilterupdate.yaml)
  - [/global](/api/gwcglobal.yaml)
  - [/gridsets](/api/gwcgridsets.yaml)
  - [/index](/api/gwcindex.yaml)
  - [/layers](/api/gwclayers.yaml)
  - [/masstruncate](/api/gwcmasstruncate.yaml)
  - [/reload](/api/gwcreload.yaml)
  - [/seed](/api/gwcseed.yaml)
- Importer extension:
  - [/imports](/api/importer.yaml)
  - [/imports (tasks)](/api/importerTasks.yaml)
  - [/imports (transforms)](/api/importerTransforms.yaml)
  - [/imports (data)](/api/importerData.yaml)
- Monitor extension:
  - [/monitor](/api/monitoring.yaml)
- XSLT extension:
  - [/services/wfs/transforms](/api/transforms.yaml)

!!! note

    You can also view the original [REST configuration API reference](api/index.md) section.

## Examples

This section contains a number of examples which illustrate some of the most common uses of the REST API. They are grouped by endpoint.
