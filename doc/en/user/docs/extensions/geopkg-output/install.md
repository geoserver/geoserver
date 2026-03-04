---
render_macros: true
---


# Installing the GeoPackage Output Extension

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Output Formats** extensions download **GeoPkg**.

    - {{ release }} example: [geopkg-output](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-geopkg-output-plugin.zip)
    - {{ version }} example: [geopkg-output](https://build.geoserver.org/geoserver/main/extensions/geoserver-{{ snapshot }}-geopkg-output-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Extract the archive and copy the contents into the GeoServer **`WEB-INF/lib`** directory.

4.  Restart GeoServer.

## Verify Installation

To verify that the extension was installed successfully:

1.  Request the [WFS 1.0.0](http://localhost:8080/geoserver/ows?service=wfs&version=1.0.0&request=GetCapabilities) GetCapabilities document from your server.

2.  Inside the resulting WFS 1.0.0 XML GetCapabilities document, find the `WFS_Capabilities/Capability/GetFeature/ResultFormat` section

3.  Verify that ``geopkg``, ``geopackage``, and ``gpkg`` are listed as a supported format

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

    You can also verify installation by looking for `GeoPKG Output Extension` on the server's ``Module Status Page``.
