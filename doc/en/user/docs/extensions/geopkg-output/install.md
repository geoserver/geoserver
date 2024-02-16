# Installing the GeoPackage Output Extension {: #geopkgoutput.install }

The GeoPackage Output extension is an official extension. Download the extension here - `geopkg-output`{.interpreted-text role="download_extension"}

1.  Download the extension for your version of GeoServer.

    !!! warning

        Make sure to match the version of the extension to the version of GeoServer.

2.  Extract the archive and copy the contents into the GeoServer **`WEB-INF/lib`** directory.

3.  Restart GeoServer.

## Verify Installation

To verify that the extension was installed successfully:

1.  Request the [WFS 1.0.0](http://localhost:8080/geoserver/ows?service=wfs&version=1.0.0&request=GetCapabilities) GetCapabilities document from your server.

2.  Inside the resulting WFS 1.0.0 XML GetCapabilities document, find the `WFS_Capabilities/Capability/GetFeature/ResultFormat` section

3.  Verify that `kg`, `ge`, and `kg` are listed as a supported format

    ``` XML
    <GetFeature>
        <ResultFormat>
            <GML2/>
            <GML3/>
            <SHAPE-ZIP/>
            <CSV/>
            <JSON/>
            <KML/>
            <geopackage/>
            <geopkg/>
            <gpkg/>
        </ResultFormat>
    </GetFeature>
    ```

!!! note

    You can also verify installation by looking for `GeoPKG Output Extension` on the server's `ge`.
