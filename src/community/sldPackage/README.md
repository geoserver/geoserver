# SLD Package extension

This extension allows to upload a zip file containing the sld file and related images, deploying them in Geoserver. The extension provides an enhancement of the SLD support in Geoserver REST API, allowing to publish images for styles.

A new service is added: `/sld/{workspaceName}/{styleName}`, accepting POST/PUT with a zip file.

Example of usage to create a new style:

```
curl -v -u admin:geoserver -XPOST -H "Content-type: application/zip"
  --data-binary @roads_style.zip
  http://localhost:8080/geoserver/rest/workspaces/acme/roads_style
```

Example of usage to update an existing style:

```
curl -v -u admin:geoserver -XPOST -H "Content-type: application/zip"
  --data-binary @roads_style.zip
  http://localhost:8080/geoserver/rest/workspaces/acme/roads_style
```
