.. _data_external_wms:

External Web Map Server
=======================

GeoServer has the ability to proxy a remote Web Map Service (WMS).  This process is sometimes known as **Cascading WMS**.  Loading a remote WMS is useful for many reasons.  If you don't manage or have access to the remote WMS, you can now manage its output as if it were local.  Even if the remote WMS is not GeoServer, you can use GeoServer features to treat its output (watermarking, decoration, printing, etc).

To access a remote WMS, it is necessary to load it as a store in GeoServer.  GeoServer must be able to access the capabilities document of the remote WMS for the store to be successfully loaded.

Adding an external WMS
----------------------

To connect to an external WMS, it is necessary to load it as a new store.  To start, in the :ref:`web_admin`, navigate to :menuselection:`Stores --> Add a new store --> WMS`.  The option is listed under :guilabel:`Other Data Sources`.

.. figure:: images/wmsaddnew.png
   :align: center

   *Adding an external WMS as a store*

.. figure:: images/wmsconfigure.png
   :align: center

   *Configuring a new external WMS store*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - :guilabel:`Workspace`
     - Name of the workspace to contain the store.  This will also be the prefix of all of the layer names published from the store.  **The workspace name on the remote WMS is not cascaded.**
   * - :guilabel:`Data Source Name`
     - Name of the store as known to GeoServer.
   * - :guilabel:`Description`
     - Description of the store. 
   * - :guilabel:`Enabled`
     - Enables the store.  If disabled, no data from the remote WMS will be served.
   * - :guilabel:`Capabilities URL`
     - The full URL to access the capabilities document of the remote WMS.
   * - :guilabel:`User Name`
     - If the WMS requires authentication, the user name to connect as.
   * - :guilabel:`Password`
     - If the WMS requires authentication, the password to connect with.
   * - :guilabel:`Max concurrent connections`
     - The maximum number of persistent connections to keep for this WMS.

When finished, click :guilabel:`Save`.

Configuring external WMS layers
-------------------------------

When properly loaded, all layers served by the external WMS will be available to GeoServer.  Before they can be served, however, they will need to be individually configured (published) as new layers.  See the section on :ref:`data_webadmin_layers` for how to add and edit new layers.  Once published, these layers will show up in the :ref:`layerpreview` and as part of the WMS capabilities document.

Features
--------

Connecting a remote WMS allows for the following features:

* **Dynamic reprojection**.  While the default projection for a layer is cascaded, it is possible to pass the SRS parameter through to the remote WMS.  Should that SRS not be valid on the remote server, GeoServer will dynamically reproject the images sent to it from the remote WMS.

* **GetFeatureInfo**.  WMS GetFeatureInfo requests will be passed to the remote WMS.  If the remote WMS supports the ``application/vnd.ogc.gml`` format the request will be successful. 

* Full **REST Configuration**. See the :ref:`rest` section for more information about the GeoServer REST interface.

Limitations
-----------

Layers served through an external WMS have some, but not all of the functionality of a local WMS.

* Layers cannot be styled with SLD.

* Alternate (local) styles cannot be used.

* Extra request parameters (``time``, ``elevation``, ``cql_filter``, etc.) cannot be used.

* GetLegendGraphic requests aren't supported.

* Image format cannot be specified.  GeoServer will attempt to request PNG images, and if that fails will use the remote server's default image format.
