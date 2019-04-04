.. _web_admin:

Web administration interface
============================

The Web administration interface is a web-based tool for configuring all aspects of GeoServer, from adding data to changing service settings. In a default GeoServer installation, this interface is accessed via a web browser at ``http://localhost:8080/geoserver/web``. However, this URL may vary depending on your local installation.

.. figure:: images/web-admin.png

   Web admin interface

The following sections detail the menu options available in GeoServer. **Unless otherwise specified, you will need to be logged in with administrative credentials to see these options.**

About & Status
--------------

The :guilabel:`About & Status` section provides access to GeoServer diagnostic and configuration tools, and can be particularly useful for debugging.  

* The :ref:`config_serverstatus` page shows a summary of server configuration parameters and run-time status. 

* The :ref:`GeoServer Logs <logging>` page shows the GeoServer log output. This is useful for determining errors without having to leave the browser.

* The :ref:`config_contact` page sets the public contact information available in the Capabilities document of the WMS server.

* The :guilabel:`About GeoServer` section provides links to the GeoServer documentation, homepage and bug tracker. **You do not need to be logged into GeoServer to access this page.**


Data
----

The :ref:`Data` section contains configuration options for all the different data-related settings. 

* The :ref:`Layer Preview <layerpreview>` page provides links to layer previews in various output formats, including the common OpenLayers and KML formats. This page helps to visually verify and explore the configuration of a particular layer. **You do not need to be logged into GeoServer to access the Layer Preview.**

* The :ref:`Workspaces <data_webadmin_workspaces>` page displays a list of workspaces, with the ability to add, edit, and delete. Also shows which workspace is the default for the server. 

* The :ref:`Stores <data_webadmin_stores>` page displays a list of stores, with the ability to add, edit, and delete. Details include the workspace associated with the store, the type of store (data format), and whether the store is enabled.

* The :ref:`Layers <data_webadmin_layers>` page displays a list of layers, with the ability to add, edit, and delete. Details include the workspace and store associated with the layer, whether the layer is enabled, and the spatial reference system (SRS) of the layer.

* The :ref:`Layer Groups <data_webadmin_layergroups>` page displays a list of layer groups, with the ability to add, edit, and delete. Details include the associated workspace (if any).

* The :ref:`Styles <styling_webadmin>` page displays a list of styles, with the ability to add, edit, and delete. Details include the associated workspace (if any).

In each of these pages that contain a table, there are three different ways to locate an object: sorting, searching, and paging. To alphabetically sort a data type, click on the column header. For simple searching, enter the search criteria in the search box and hit Enter. And to page through the entries (25 at a time), use the arrow buttons located on the bottom and top of the table.

Services
--------

The :ref:`services` section is for configuring the services published by GeoServer.

* The :ref:`Web Coverage Service (WCS) <services_webadmin_wcs>` page manages metadata, resource limits, and SRS availability for WCS.

* The :ref:`Web Feature Service (WFS) <services_webadmin_wfs>` page manages metadata, feature publishing, service level options, and data-specific output for WFS.

* The :ref:`Web Map Service (WMS) <services_webadmin_wms>` page manages metadata, resource limits, SRS availability, and other data-specific output for WMS.

Settings
--------

The :guilabel:`Settings` section contains configuration settings that apply to the entire server.


* The :ref:`Global Settings <config_globalsettings>` page configures messaging, logging, character and proxy settings for the entire server.

* The :ref:`JAI` page configures several JAI parameters, used by both WMS and WCS operations.

* The :ref:`Coverage Access <config_converageaccess>` page configures settings related to loading and publishing coverages.

Tile Caching
------------

The :guilabel:`Tile Caching` section configures the embedded :ref:`GeoWebCache <gwc>`.

* The :ref:`Tile Layers <gwc_webadmin_layers>` page shows which layers in GeoServer are also available as tiled (cached)layers, with the ability to add, edit, and delete.

* The :ref:`Caching Defaults <gwc_webadmin_defaults>` page sets the global options for the caching service.

* The :ref:`Gridsets <gwc_webadmin_gridsets>` page shows all available gridsets for the tile caches, with the ability to add, edit, and delete.

* The :ref:`Disk Quota <gwc_webadmin_diskquotas>` page sets the options for tile cache management on disk, including strategies to reduce file size when necessary.

* The :ref:`BlobStores <gwc_webadmin_blobstores>` pages manages the different blobstores (tile cache sources) known to the embedded GeoWebCache. 

Security
--------

The :ref:`Security <security_webadmin>` section configures the built-in :ref:`security subsystem <security>`.

* The :ref:`Settings <security_webadmin_settings>` page manages high-level options for the security subsystem.

* The :ref:`Authentication <security_webadmin_auth>` page manages authentication filters, filter chains, and providers.

* The :ref:`Passwords <security_webadmin_passwd>` page manages the password policies for users and the master (root) account.

* The :ref:`Users, Groups, Roles <security_webadmin_ugr>` page manages the users, groups, and roles, and how they are all associated with each other. Passwords for user accounts can be changed here.

* The :ref:`Data <security_webadmin_data>` page manages the data-level security options, allowing workspaces and layers to be restricted by role.

* The :ref:`Services <security_webadmin_services>` page manages the service-level security options, allowing services and operations to be restricted by role.

Demos
-----

The :ref:`demos` section contains links to example WMS, WCS, and WFS requests for GeoServer as well as a listing all SRS info known to GeoServer. In addition, there is a reprojection console for converting coordinates between spatial reference systems, and a request builder for WCS requests. **You do not need to be logged into GeoServer to access these pages.**

Tools
-----

The :guilabel:`Tools` section contains administrative tools. By default, the only tool is the :guilabel:`Catalog Bulk Load Tool`, which can bulk copy test data into the catalog.

Extensions
----------

:ref:`GeoServer extensions <extensions>` can add functionality and extra options to the web interface. Details can be found in the section for each extension.
