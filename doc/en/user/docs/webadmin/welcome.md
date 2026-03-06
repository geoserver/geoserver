# Welcome

The **Welcome Page** lists the web services published by GeoServer for mapping, data access and processing.

## Web Services {: #welcome_webservices }

The welcome page lists the global web services (accessing the complete list of layers).

![](images/web-admin.png)
*Welcome Page Global Services*

To use copy-and-paste the web services URLs into your Desktop GIS or web mapping application.

![](images/qgis-global-wms.png)
*QGIS Desktop GIS WMS Connection*

![](images/qgis-global-wms-add.png)
*QGIS Desktop GIS Add WMS Layer*

![](images/qgis-map.png)
*QGIS Desktop GIS Map*

Opening these URLs in the browser download or display machine readable the service description.

![](images/wms-caps.png)
*WMS 1.3.0 GetCapabilities Document*

If global web services are disabled the initial welcome page web services will not be available.

## Workspace Web Services

Use **workspace** select at the top of the welcome page to choose a workspace. The welcome page contact information and web services are updated to match the workspace selected.

![](images/welcome-workspace.png)
*Welcome Workspace Web Services*

The web service links provided may be used in your Desktop GIS or web mapping application to access the workspace layers.

## Layer Web Services

Use the **layer** select at the top of the welcome page to choose a layer or layer group.

![](images/welcome-layer.png)
*Welcome Workspace Web Services*

The **workspace** select, along with the page contact information and web services are updated to match the layer selected.

## Server Overview (Administrators)

When logged in with administrative credentials a configuration overview is provided, along with any information or warning notifications.

![](images/welcome-admin.png)
*Welcome Administrator Feedback*

Click **Layers summary** link to navigate to the [Layers page](../data/webadmin/layers.md), press [Add layers](../data/webadmin/layers.md#data_webadmin_layers_add_a_layer) to create a new layer.

Click **Stores summary** link to navigate to the [Stores page](../data/webadmin/stores.md), press [Add stores](../data/webadmin/stores.md#data_webadmin_stores_add_a_store) to create a new store.

Click **Workspaces summary** link to navigate to the [Workspaces page](../data/webadmin/workspaces.md), press [Add workspaces](../data/webadmin/workspaces.md#data_webadmin_workspaces_add_workspace) to create a new workspace.

### Information and Warnings

GeoServer status information messages provide feedback on normal operation.

Warnings describe configuration issues to be addressed, often with a short-cut to the configuration page used to address the issue.
