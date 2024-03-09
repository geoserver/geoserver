---
render_macros: true
---

# Extension Install

This workshop course requires GeoServer with a few additional extensions.

-   CSS Styling: Quickly and easily generate SLD files
-   YSLD Styling: An alternative styling language to SLD
-   Importer: Wizard for bulk import of data

On Windows the following is recommended:

-   [FireFox](http://www.mozilla.org/en-US/firefox/new/)
-   [Notepad++](http://notepad-plus-plus.org)

The **CSS extension** is distributed as a supported GeoServer extension. Extensions are unpacked into the `libs` folder of the GeoServer application. The **YSLD extension** is a new addition to geoserver and is distributed as an unsupported GeoServer extension.

!!! note

    In a classroom setting these extensions have already been installed.

## Manual Install

To download and install the required extensions by hand:

1.  -   From the [download page](https://geoserver.org/download) download:
    -   {{ download_extension('css') }} (nightly {{ download_extension('css','snapshot') }})
    -   {{ download_extension('ysld') }} (nightly {{ download_extension('ysld','snapshot') }})

    It is important to download the version that matches the GeoServer you are running.

2.  Stop the GeoServer application.

3.  Navigate into the **`webapps/geoserver/WEB-INF/lib`** folder.

    These files make up the running GeoServer application.

4.  Unzip the contents of the three zip files into the **`lib`** folder.

5.  Restart the Application Server.

6.  Login to the Web Administration application. Select **Styles** from the naviagion menu. Click **Create a new style** and ensure both CSS and YSLD are available in the formats dropdown. Click **Cancel** to return to the **Styles** page without saving.
