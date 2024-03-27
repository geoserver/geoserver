# WCS Request Builder

GeoServer includes a request builder for building and testing out WCS requests. Since WCS requests can be cumbersome to author, this tool can make working with WCS much easier.

## Accessing the WCS Request Builder

To access the WCS Request Builder:

1.  Navigate to the [Web administration interface](../../webadmin/index.md).
2.  Click the [Demos](../../configuration/demos/index.md) link on the left side.
3.  Select **WCS Request Builder** from the list of demos.

![](img/demos_wcsrequestbuilder.png)
*WCS request builder in the list of demos*

## Using the WCS Request Builder

The WCS Request Builder consists of a form which can be used to generate a number of different types of requests.

When first opened, the form is short, only including the following options:

-   **WCS Version**---Version of WCS to use when crafting the request. Options are **1.0.0** and **1.1.1**.

-   **Coverage name**---Coverage to use in the request. Any published (raster) layer in GeoServer can be selected.

    !!! note

        All other options displayed will be non-functional until **Coverage name** is selected.

![](img/wcsrequestbuilder.png)
*WCS request builder in its initial form*

Once selected, the remainder of the form will be displayed. The following options are available:

-   **Spatial subset**---Sets the extent of the GetCoverage request in units of the layer CRS. Defaults to the full extent of the layer.
-   **Coordinate reference system**---Source CRS of the layer. Default is the CRS of the layer in GeoServer.
-   **Specify source grid manually** *(1.0.0 only)*---If checked, allows for determining the grid of pixels for the output.
-   **Target coverage layout** *(1.1.1 only)*---Specifies how the dimensions of the output grid will be determined:
    -   **Automatic target layout**---Sets that the output grid will be determined automatically.
    -   **Specify grid resolutions**---Sets the resolution of the output grid. X and Y resolutions can be set differently.
    -   **Specify "grid to world" transformation**---Sets the output using latitude/longitude, as well as X and Y scale and shear values.
-   **Target CRS**---CRS of the result (output) of the GetCoverage request. If different from the **Coordinate reference system**, the result will be a reprojection into the target CRS.
-   **Output format**---Format of the result (output) of the GetCoverage request. Any valid WCS output format is allowed. Default is **GeoTIFF**.

![](img/wcsrequestbuilder_100.png)
*WCS request builder form (WCS version 1.0.0)*

![](img/wcsrequestbuilder_111.png)
*WCS request builder form (WCS version 1.1.1)*

There is also a link for **Describe coverage** next to the **Coverage name** which will execute a [WCS DescribeCoverage](reference.md#wcs_describecoverage) request for the particular layer.

At the bottom of the form are two buttons for form submission:

-   **Get Coverage**---Executes a GetCoverage request using the parameters in the form. This will usually result in a file which can be downloaded.
-   **Generate GetCoverage XML**---Generates the GetCoverage request using the parameters in the form and then, instead of executing it, outputs the request itself to the screen.

![](img/wcsrequestbuilder_xml.png)
*WCS request builder showing GetCoverage XML*
