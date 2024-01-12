# External Web Feature Server

GeoServer has the ability to load data from a remote Web Feature Server (WFS). This is useful if the remote WFS lacks certain functionality that GeoServer contains. For example, if the remote WFS is not also a Web Map Server (WMS), data from the WFS can be cascaded through GeoServer to utilize GeoServer's WMS. If the remote WFS has a WMS but that WMS cannot output KML, data can be cascaded through GeoServer's WMS to output KML.

## Adding an external WFS

To connect to an external WFS, it is necessary to load it as a new datastore. To start, navigate to **Stores --> Add a new store --> Web Feature Server**.

![](images/externalwfs.png)
*Adding an external WFS as a store*

  --------------------------------- ----------------------------------------------------------------------------------------------------------------------------
  **Option**                        **Description**

  **Workspace**                     Name of the workspace to contain the store. This will also be the prefix of all of the layer names created from the store.

  **Data Source Name**              Name of the store as known to GeoServer.

  **Description**                   Description of the store.

  **Enabled**                       Enables the store. If disabled, no data from the external WFS will be served.

  **GET_CAPABILITIES_URL**          URL to access the capabilities document of the remote WFS.

  **PROTOCOL**                      When checked, connects with POST, otherwise uses GET.

  **USERNAME**                      The user name to connect to the external WFS.

  **PASSWORD**                      The password associated with the above user name.

  **ENCODING**                      The character encoding of the XML requests sent to the server. Defaults to `UTF-8`.

  **TIMEOUT**                       Time (in milliseconds) before timing out. Default is `3000`.

  **BUFFER_SIZE**                   Specifies a buffer size (in number of features). Default is `10` features.

  **TRY_GZIP**                      Specifies that the server should transfer data using compressed HTTP if supported by the server.

  **LENIENT**                       When checked, will try to render features that don't match the appropriate schema. Errors will be logged.

  **MAXFEATURES**                   Maximum number of features to retrieve for each featuretype. Default is no limit.

  **AXIS_ORDER**                    Axis order used in result coordinates (It applies only to WFS 1.x.0 servers). Default is Compliant.

  **AXIS_ORDER_FILTER**             Axis order used in filter (It applies only to WFS 1.x.0 servers). Default is Compliant.

  **OUTPUTFORMAT**                  Output format to request (instead of the default remote service one) e.g. JSON.

  **GML_COMPLIANCE_LEVEL**          OCG GML compliance level. i.e. (simple feature) 0, 1 or 2. Default is 0.

  **GML_COMPATIBLE_TYPENAMES**      Use GML Compatible TypeNames (replace : by _). Default is no false.

  **USE_HTTP_CONNECTION_POOLING**   Use connection pooling to connect to the remote WFS service. Also enables digest authentication.
  --------------------------------- ----------------------------------------------------------------------------------------------------------------------------

When finished, click **Save**.

## Configuring external WFS layers

When properly loaded, all layers served by the external WFS will be available to GeoServer. Before they can be served, however, they will need to be individually configured as new layers. See the section on [Layers](../webadmin/layers.md) for how to add and edit new layers.

## Connecting to an external WFS layer via a proxy server

In a corporate environment it may be necessary to connect to an external WFS through a proxy server. To achieve this, various java variables need to be set.

For a Windows install running GeoServer as a service, this is done by modifying the wrapper.conf file. For a default Windows install, modify **`C:\Program Files\GeoServer x.x.x\wrapper\wrapper.conf`** similarly to the following.

> \# Java Additional Parameters
>
> wrapper.java.additional.1=-Djetty.home=. wrapper.java.additional.2=-DGEOSERVER_DATA_DIR="%GEOSERVER_DATA_DIR%" wrapper.java.additional.3=-Dhttp.proxySet=true wrapper.java.additional.4=-Dhttp.proxyHost=maitproxy wrapper.java.additional.5=-Dhttp.proxyPort=8080 wrapper.java.additional.6=-Dhttps.proxyHost=maitproxy wrapper.java.additional.7=-Dhttps.proxyPort=8080 wrapper.java.additional.8=-Dhttp.nonProxyHosts="mait*localhost"

Note that the ***http.proxySet=true*** parameter is required. Also, the parameter numbers must be consecutive - i.e. no gaps.

For a Windows install not running GeoServer as a service, modify **`startup.bat`** so that the ***java*** command runs with similar -D parameters.

For a Linux/UNIX install, modify **`startup.sh`** so that the ***java*** command runs with similar -D parameters.
