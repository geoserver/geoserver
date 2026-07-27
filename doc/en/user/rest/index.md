---
render_macros: true
---

# REST

GeoServer provides a [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer) interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the [Web administration interface](../webadmin/index.md).

REST is an acronym for "[REpresentational State Transfer](http://en.wikipedia.org/wiki/Representational_state_transfer)". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP: GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as `http://GEOSERVER_HOME/rest/workspaces/topp`.

## API

!!! warning
    The API is documented as Swagger 2.0 files. However, these files have been written by hand back in 2017, and have not always been kept up to date with the evolution of the GeoServer configuration object structure. Also, they have not been tested for proper client generation, and will likely not work for that purpose. Take them only as a form of documentation.

The following links provide direct access to the GeoServer REST API documentation, including definitions and examples of each endpoint:

- [/about/manifests]({{ api_url }}?urls.primaryName=About%3A%20Manifests)
- [/about/system-status]({{ api_url }}?urls.primaryName=About%3A%20System%20Status)
- [/datastores]({{ api_url }}?urls.primaryName=Datastores)
- [/coverages]({{ api_url }}?urls.primaryName=Coverages)
- [/coveragestores]({{ api_url }}?urls.primaryName=Coverage%20Stores)
- [/crs]({{ api_url }}?urls.primaryName=CRS)
- [/featuretypes]({{ api_url }}?urls.primaryName=Feature%20Types)
- [/fonts]({{ api_url }}?urls.primaryName=Fonts)
- [/layergroups]({{ api_url }}?urls.primaryName=Layer%20Groups)
- [/layers]({{ api_url }}?urls.primaryName=Layers)
- [/logging]({{ api_url }}?urls.primaryName=Logging)
- [/monitoring]({{ api_url }}?urls.primaryName=Monitoring)
- [/namespaces]({{ api_url }}?urls.primaryName=Namespaces)
- [/services/wms|wfs|wcs|wmts/settings]({{ api_url }}?urls.primaryName=OWS%20Services%20%28WMS%2FWFS%2FWCS%2FWMTS%29)
- [/reload]({{ api_url }}?urls.primaryName=Reload)
- [/resource]({{ api_url }}?urls.primaryName=Resource)
- [/security]({{ api_url }}?urls.primaryName=Security)
- [/settings]({{ api_url }}?urls.primaryName=Settings)
- [/structuredcoverages]({{ api_url }}?urls.primaryName=Structured%20Coverages)
- [/styles]({{ api_url }}?urls.primaryName=Styles)
- [/templates]({{ api_url }}?urls.primaryName=Templates)
- [/transforms]({{ api_url }}?urls.primaryName=Transforms)
- [/wmslayers]({{ api_url }}?urls.primaryName=WMS%20Layers)
- [/wmsstores]({{ api_url }}?urls.primaryName=WMS%20Stores)
- [/wmtslayers]({{ api_url }}?urls.primaryName=WMTS%20Layers)
- [/wmtsstores]({{ api_url }}?urls.primaryName=WMTS%20Stores)
- [/workspaces]({{ api_url }}?urls.primaryName=Workspaces)
- [/urlchecks]({{ api_url }}?urls.primaryName=URL%20Checks)
- [/usergroup]({{ api_url }}?urls.primaryName=User%20Groups)
- [/roles]({{ api_url }}?urls.primaryName=Roles)
- [/filterChains]({{ api_url }}?urls.primaryName=Filter%20Chains)
- [/authFilters]({{ api_url }}?urls.primaryName=Authentication%20Filters)
- [/authProviders]({{ api_url }}?urls.primaryName=Authentication%20Providers)
- [/usergroupservices]({{ api_url }}?urls.primaryName=User%20Group%20Services)
- GeoWebCache:
    - [/blobstores]({{ api_url }}?urls.primaryName=GWC%3A%20Blob%20Stores)
    - [/bounds]({{ api_url }}?urls.primaryName=GWC%3A%20Bounds)
    - [/diskquota]({{ api_url }}?urls.primaryName=GWC%3A%20Disk%20Quota)
    - [/filterupdate]({{ api_url }}?urls.primaryName=GWC%3A%20Filter%20Update)
    - [/global]({{ api_url }}?urls.primaryName=GWC%3A%20Global)
    - [/gridsets]({{ api_url }}?urls.primaryName=GWC%3A%20Gridsets)
    - [/index]({{ api_url }}?urls.primaryName=GWC%3A%20Index)
    - [/layers]({{ api_url }}?urls.primaryName=GWC%3A%20Layers)
    - [/masstruncate]({{ api_url }}?urls.primaryName=GWC%3A%20Mass%20Truncate)
    - [/reload]({{ api_url }}?urls.primaryName=GWC%3A%20Reload)
    - [/seed]({{ api_url }}?urls.primaryName=GWC%3A%20Seed)
- Importer extension:
    - [/imports]({{ api_url }}?urls.primaryName=Importer%3A%20Imports)
    - [/imports (tasks)]({{ api_url }}?urls.primaryName=Importer%3A%20Tasks)
    - [/imports (transforms)]({{ api_url }}?urls.primaryName=Importer%3A%20Transforms)
    - [/imports (data)]({{ api_url }}?urls.primaryName=Importer%3A%20Data)
- Monitor extension:
    - [/monitor]({{ api_url }}?urls.primaryName=Monitor%20Extension)
- XSLT extension:
    - [/services/wfs/transforms]({{ api_url }}?urls.primaryName=XSLT%20Extension)

!!! note
    You can also view the original [REST configuration API reference](api/index.md) section.

## Examples

This section contains a number of examples which illustrate some of the most common uses of the REST API. They are grouped by endpoint.

<div class="grid cards" markdown>

- [About](about.md)
- [Fonts](fonts.md)
- [Layer groups](layergroups.md)
- [Layers](layers.md)
- [Security](security.md)
- [Styles](styles.md)
- [Workspaces](workspaces.md)
- [Stores](stores.md)
- [Uploading a new image mosaic](imagemosaic.md)
- [App Schema](appschema.md)
- [URL Checks](urlchecks.md)
- [Filter Chains](filterchains.md)
- [Auth Filters](authenticationfilters.md)
- [Auth Providers (How-To)](authenticationproviders.md)
- [User/Group Services](usergroupservices.md)
- [REST configuration API reference](api/index.md)

</div>
