---
render_macros: true
---

# Swagger APIs

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

    You can also view the original [REST configuration API reference](../rest/api/index.md) section.
