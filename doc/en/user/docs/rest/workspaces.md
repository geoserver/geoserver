# Workspaces

The REST API allows you to create and manage workspaces in GeoServer.

!!! note

    Read the [API reference for /workspaces](https://docs.geoserver.org/latest/en/api/#1.0.0/workspaces.yaml).

## Adding a new workspace

**Creates a new workspace named "acme" with a POST request**

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" 
          -d "<workspace><name>acme</name></workspace>" 
          http://localhost:8080/geoserver/rest/workspaces

!!! abstract "python"

    TBD

!!! abstract "java"

    TBD

*Response*

    201 Created

!!! note

    The `Location` response header specifies the location (URI) of the newly created workspace.

## Listing workspace details

**Retrieve information about a specific workspace**

*Request*

!!! abstract "curl"

        curl -v -u admin:geoserver -XGET -H "Accept: text/xml" 
          http://localhost:8080/geoserver/rest/workspaces/acme

!!! note

    The `Accept` header is optional.

!!! abstract "python"

    TBD

!!! abstract "java"

    TBD

*Response*

``` xml
<workspace>
  <name>acme</name>
  <dataStores>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
     href="http://localhost:8080/geoserver/rest/workspaces/acme/datastores.xml" 
     type="application/xml"/>
  </dataStores>
  <coverageStores>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
     href="http://localhost:8080/geoserver/rest/workspaces/acme/coveragestores.xml" 
     type="application/xml"/>
  </coverageStores>
  <wmsStores>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
     href="http://localhost:8080/geoserver/rest/workspaces/acme/wmsstores.xml" 
     type="application/xml"/>
  </wmsStores>
</workspace>
```

This shows that the workspace can contain "`dataStores`" (for [vector data](../data/vector/index.md)), "`coverageStores`" (for [raster data](../data/raster/index.md)), and "`wmsStores`" (for [cascaded WMS servers](../data/cascaded/wms.md)).
