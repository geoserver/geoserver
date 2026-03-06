---
render_macros: true
---

# REST

GeoServer provides a [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer) interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the [Web administration interface](../webadmin/index.md).

REST is an acronym for "`REpresentational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`_". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP: GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as `http://GEOSERVER_HOME/rest/workspaces/topp`.

## API

!!! warning

    The API is documented as Swagger 2.0 files. However, these files have been written by hand back in 2017, and have not always been kept up to date with the evolution of the GeoServer configuration object structure. Also, they have not been tested for proper client generation, and will likely not work for that purpose. Take them only as a form of documentation.

The following links provide direct access to the GeoServer REST API documentation, including definitions and examples of each endpoint:

- [/about/manifests]({{ api_url }}/manifests.yaml)
- [/about/system-status]({{ api_url }}/system-status.yaml)
- [/datastores]({{ api_url }}/datastores.yaml)
- [/coverages]({{ api_url }}/coverages.yaml)
- [/coveragestores]({{ api_url }}/coveragestores.yaml)
- [/featuretypes]({{ api_url }}/featuretypes.yaml)
- [/fonts]({{ api_url }}/fonts.yaml)
- [/layergroups]({{ api_url }}/layergroups.yaml)
- [/layers]({{ api_url }}/layers.yaml)
- [/logging]({{ api_url }}/logging.yaml)
- [/monitoring]({{ api_url }}/monitoring.yaml)
- [/namespaces]({{ api_url }}/namespaces.yaml)
- [/services/wms|wfs|wcs|wmts/settings]({{ api_url }}/owsservices.yaml)
- [/reload]({{ api_url }}/reload.yaml)
- [/resource]({{ api_url }}/resource.yaml)
- [/security]({{ api_url }}/security.yaml)
- [/settings]({{ api_url }}/settings.yaml)
- [/structuredcoverages]({{ api_url }}/structuredcoverages.yaml)
- [/styles]({{ api_url }}/styles.yaml)
- [/templates]({{ api_url }}/templates.yaml)
- [/transforms]({{ api_url }}/transforms.yaml)
- [/wmslayers]({{ api_url }}/wmslayers.yaml)
- [/wmsstores]({{ api_url }}/wmsstores.yaml)
- [/wmtslayers]({{ api_url }}/wmtslayers.yaml)
- [/wmtsstores]({{ api_url }}/wmtsstores.yaml)
- [/workspaces]({{ api_url }}/workspaces.yaml)
- [/urlchecks]({{ api_url }}/urlchecks.yaml)
- [/usergroup]({{ api_url }}/usergroup.yaml)
- [/roles]({{ api_url }}/roles.yaml)
- [/filterChains]({{ api_url }}/filterchains.yaml)
- [/authFilters]({{ api_url }}/authenticationfilterconfiguration.yaml)
- [/authProviders]({{ api_url }}/authenticationproviders.yaml)
- [/usergroupservices]({{ api_url }}/usergroupservices.yaml)
- GeoWebCache:
    - [/blobstores]({{ api_url }}/gwcblobstores.yaml)
    - [/bounds]({{ api_url }}/gwcbounds.yaml)
    - [/diskquota]({{ api_url }}/gwcdiskquota.yaml)
    - [/filterupdate]({{ api_url }}/gwcfilterupdate.yaml)
    - [/global]({{ api_url }}/gwcglobal.yaml)
    - [/gridsets]({{ api_url }}/gwcgridsets.yaml)
    - [/index]({{ api_url }}/gwcindex.yaml)
    - [/layers]({{ api_url }}/gwclayers.yaml)
    - [/masstruncate]({{ api_url }}/gwcmasstruncate.yaml)
    - [/reload]({{ api_url }}/gwcreload.yaml)
    - [/seed]({{ api_url }}/gwcseed.yaml)
- Importer extension:
    - [/imports]({{ api_url }}/importer.yaml)
    - [/imports (tasks)]({{ api_url }}/importerTasks.yaml)
    - [/imports (transforms)]({{ api_url }}/importerTransforms.yaml)
    - [/imports (data)]({{ api_url }}/importerData.yaml)
- Monitor extension:
    - [/monitor]({{ api_url }}/monitoring.yaml)
- XSLT extension:
    - [/services/wfs/transforms]({{ api_url }}/transforms.yaml)

!!! note

    You can also view the original [REST configuration API reference](api/index.md) section.

## Examples

This section contains a number of examples which illustrate some of the most common uses of the REST API. They are grouped by endpoint.
