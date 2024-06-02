# Web administration interface

The Web administration interface is a web-based tool for configuring all aspects of GeoServer, from adding data to changing service settings. In a default GeoServer installation, this interface is accessed via a web browser at `http://localhost:8080/geoserver/web`. However, this URL may vary depending on your local installation.

![](images/web-admin.png)
*Web admin interface*

The following sections detail the menu options available in GeoServer. **Unless otherwise specified, you will need to be logged in with administrative credentials to see the complete list of pages.**

## Welcome

-   The [Welcome](welcome.md) page lists the web services published by GeoServer.

    When logged in with administrative credentials a configuration overview is provided, along with any information or warning notifications.

## About & Status

The **About & Status** section provides access to GeoServer diagnostic and configuration tools, and can be particularly useful for debugging.

-   The [Status](../configuration/status.md) page shows a summary of server configuration parameters and run-time status.

-   The [GeoServer Logs](../configuration/logging.md) page shows the GeoServer log output. This is useful for determining errors without having to leave the browser.

-   The [Contact Information](../configuration/contact.md) page sets the public contact information available in the Capabilities document of the WMS server.

-   The [About GeoServer Page](about.md) section provides links to the GeoServer documentation, homepage and bug tracker.

    **You do not need to be logged into GeoServer to access this page.**

## Data

The [Data management](../data/index.md) section contains configuration options for all the different data-related settings.

-   The [Layer Preview](../data/webadmin/layerpreview.md) page provides links to layer previews in various output formats, including the common OpenLayers and KML formats. This page helps to visually verify and explore the configuration of a particular layer.

    **You do not need to be logged into GeoServer to access the Layer Preview.**

-   The [Workspaces](../data/webadmin/workspaces.md) page displays a list of workspaces, with the ability to add, edit, and delete. Also shows which workspace is the default for the server.

-   The [Stores](../data/webadmin/stores.md) page displays a list of stores, with the ability to add, edit, and delete. Details include the workspace associated with the store, the type of store (data format), and whether the store is enabled.

-   The [Layers](../data/webadmin/layers.md) page displays a list of layers, with the ability to add, edit, and delete. Details include the workspace and store associated with the layer, whether the layer is enabled, and the spatial reference system (SRS) of the layer.

-   The [Layer Groups](../data/webadmin/layergroups.md) page displays a list of layer groups, with the ability to add, edit, and delete. Details include the associated workspace (if any).

-   The [Styles](../styling/webadmin/index.md) page displays a list of styles, with the ability to add, edit, and delete. Details include the associated workspace (if any).

In each of these pages that contain a table, there are three different ways to locate an object: sorting, searching, and paging. To alphabetically sort a data type, click on the column header. For simple searching, enter the search criteria in the search box and hit Enter. And to page through the entries (25 at a time), use the arrow buttons located on the bottom and top of the table.

**These pages are shown to administrators, and users that have data admin permissions.**

## Services

The [Services](../services/index.md) section is for configuring the services published by GeoServer.

-   The [Web Coverage Service (WCS)](../services/wcs/webadmin.md) page manages metadata, resource limits, and SRS availability for WCS.
-   The [Web Feature Service (WFS)](../services/wfs/webadmin.md) page manages metadata, feature publishing, service level options, and data-specific output for WFS.
-   The [Web Map Service (WMS)](../services/wms/webadmin.md) page manages metadata, resource limits, SRS availability, and other data-specific output for WMS.
-   The [Web Processing Service (WPS)](../services/wps/administration.md) page manages metadata and resource limits for WPS.

## Settings

The **Settings** section contains configuration settings that apply to the entire server.

-   The [Global Settings](../configuration/globalsettings.md) page configures messaging, logging, character and proxy settings for the entire server.
-   The [Image Processing](../configuration/image_processing/index.md) page configures several JAI parameters, used by both WMS and WCS operations.
-   The [Coverage Access](../configuration/raster_access.md) page configures settings related to loading and publishing coverages.

## Tile Caching

The **Tile Caching** section configures the embedded [GeoWebCache](../geowebcache/index.md).

-   The [Tile Layers](../geowebcache/webadmin/layers.md) page shows which layers in GeoServer are also available as tiled (cached)layers, with the ability to add, edit, and delete.
-   The [Caching Defaults](../geowebcache/webadmin/defaults.md) page sets the global options for the caching service.
-   The [Gridsets](../geowebcache/webadmin/gridsets.md) page shows all available gridsets for the tile caches, with the ability to add, edit, and delete.
-   The [Disk Quota](../geowebcache/webadmin/diskquotas.md) page sets the options for tile cache management on disk, including strategies to reduce file size when necessary.
-   The [BlobStores](../geowebcache/webadmin/blobstores.md) pages manages the different blobstores (tile cache sources) known to the embedded GeoWebCache.

## Security

The [Security](../security/webadmin/index.md) section configures the built-in [security subsystem](../security/index.md).

-   The [Settings](../security/webadmin/settings.md) page manages high-level options for the security subsystem.
-   The [Authentication](../security/webadmin/auth.md) page manages authentication filters, filter chains, and providers.
-   The [Passwords](../security/webadmin/passwords.md) page manages the password policies for users and the root account.
-   The [Users, Groups, Roles](../security/webadmin/ugr.md) page manages the users, groups, and roles, and how they are all associated with each other. Passwords for user accounts can be changed here.
-   The [Data](../security/webadmin/data.md) page manages the data-level security options, allowing workspaces and layers to be restricted by role.
-   The [Services](../security/webadmin/services.md) page manages the service-level security options, allowing services and operations to be restricted by role.

## Demos

The [Demos](../configuration/demos/index.md) section contains links to example WMS, WCS, and WFS requests for GeoServer as well as a listing all SRS info known to GeoServer. In addition, there is a reprojection console for converting coordinates between spatial reference systems, and a request builder for WCS requests.

**You do not need to be logged into GeoServer to access these pages.**

## Tools

The [Tools](../configuration/tools/index.md) section contains administrative tools.

-   The [Web Resource](../configuration/tools/resource/index.md) tool provides management of data directory icons, fonts, and configuration files.
-   The [Catalog Bulk Load Tool](../configuration/tools/bulk/index.md) can bulk copy configuration for testing

## Extensions

[GeoServer extensions](../extensions/index.md) can add functionality and extra options to the web interface. Details can be found in the section for each extension.

## User interface

### Navigation

A navigation menu is provided along the left-hand-side of the screen listing configuration pages.

To return to the main [Welcome](welcome.md) click on the GeoServer logo at the top of the navigation menu.

### User login

The upper right of the web administration interface provides options to [login](../gettingstarted/web-admin-quickstart/index.md#logging_in).

GeoServer will share only the web services and layers available to the current user.

### Choosing the UI language

The administration interface is displayed using the browser's preferred language when available, otherwise it will fall back to English. The drop-down chooser on the side of the login/logout button allows selection of a different language.

The language choice is saved in the session, as well as in a cookie, to retain the language choice across user sessions.
