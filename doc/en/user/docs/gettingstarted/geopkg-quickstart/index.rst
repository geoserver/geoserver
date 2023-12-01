.. _geopkg_quickstart:

Publishing a GeoPackage
=======================

This tutorial walks through the steps of publishing a GeoPackage with GeoServer.

.. note:: This tutorial assumes that GeoServer is running at ``http://localhost:8080/geoserver``.

Data preparation
----------------

First let's gather that the data that we'll be publishing.

#. The sample data folder includes :file:`data/ne/natural_earth.gpkg`

#. This file contains small scale 1:110m data:

   * `coastlines <https://www.naturalearthdata.com/downloads/110m-physical-vectors/110m-coastline/>`__
   * `countries <https://www.naturalearthdata.com/downloads/110m-cultural-vectors/110m-admin-0-countries/>`__
   * `boundary lines <https://www.naturalearthdata.com/downloads/110m-cultural-vectors/110m-admin-0-boundary-lines/>`__
   * `populated places <https://www.naturalearthdata.com/downloads/110m-cultural-vectors/110m-populated-places/>`__


.. note::  This :file:`data/ne/natural_earth.gpkg` file has been processed from https://www.naturalearthdata.com/downloads/ page, to download the original (much larger) file visit the above page and download `GeoPackage <https://naciscdn.org/naturalearth/packages/natural_earth_vector.gpkg.zip>`__ link.
 
Creating a new workspace
------------------------

The next step is to create a workspace for the geopackage. A workspace is a folder used to group similar layers together.

.. note:: This step is optional if you'd like to use an existing workspace. Usually, a workspace is created for each project, which can include stores and layers that are related to each other.

#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

#. Log into GeoServer as described in the :ref:`logging_in` section. 

#. Navigate to :menuselection:`Data --> Workspaces`.

   .. figure:: /data/webadmin/img/data_workspaces.png

      Workspaces page

#. Click the :guilabel:`Add new workspace` button to display the :guilabel:`New Workspace` page.

#. You will be prompted to enter a workspace :guilabel:`Name` and :guilabel:`Namespace URI`.

   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - Name:
        - :kbd:`tutorial`
      * - Namespace URI
        - :kbd:`http://localhost:8080/geoserver/tutorial`

   .. note:: A workspace name is an identifier describing your project. It must not exceed ten characters or contain spaces.
   
   .. note:: A Namespace URI (Uniform Resource Identifier) can usually be a URL associated with your project with an added trailing identifier indicating the workspace. The Namespace URI filed does not need to resolve to an actual valid web address.
   
#. Press the :guilabel:`Submit` button. 

   .. figure:: images/workspace.png
      
      New workspace

#. The ``tutorial`` workspace will be added to the :guilabel:`Workspaces` list.

Create a store
--------------

Once the workspace is created, we are ready to add a new store. The store tells GeoServer how to connect to the geopackage. 

#. Navigate to :menuselection:`Data-->Stores`.
   
   .. figure:: images/stores.png
       
      Stores page 
    
#. This page displays a list of stores, including the type of store and the workspace that the store belongs to.

#. In order to add the geopackage, you need to create a new store. Click the :guilabel:`Add new Store` button. You will be redirected to a list of the data sources supported by GeoServer. Note that the data sources are extensible, so your list may look slightly different.

   .. figure:: images/stores_new.png

      New data source

#. From the list of :guilabel:`Vector Data Sources` locate and click the :guilabel:`GeoPackage` link.

   The :guilabel:`New Vector Data Source` page will display.

#. Begin by configuring the :guilabel:`Basic Store Info`.

   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - workspace
        - ``tutorial``
      * - Data Source Name
        - :kbd:`NaturalEarth`
      * - Description
        - :kbd:`GeoPackage of NaturalEarth data`
   
   This information is internal to GeoServer and is not used as part of the web service protocols. We recommend keeping the :guilabel:`Data Source Name` simple as they will be used to form folders in the data directory (so keep any operating system restrictions on character use in mind).
   
   .. figure:: images/basic.png
      
      Basic Store info

#. Connection parameters are used to establish the connection with your database. As GeoPackage is a file based database this will primarily consist of the geopackage location.

  
#. Under :guilabel:`Connection Parameters`, browse to the location :guilabel:`URL` of the geopackage, in our example  :file:`data/ne.shp`.
  
   .. figure:: images/connection_browse.png
      :width: 75%

      Browse database location

#. The :guilabel:`Connection Parameters` for our geopackage are:

   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - database
        - :kbd:`file:data/ne/natural_earth.gpkg`
      * - read_only
        - checked
   
   The use of :guilabel:`read_only` above indicates that we will not be writing to this GeoPackage, allowing GeoServer to avoid managing write locks when accessing this content for greater performance.
   
   .. figure:: images/connection.png
      
      Connection Parameters

#. Press :guilabel:`Save`. 

#. You will be redirected to the :guilabel:`New Layer` page (as this is the most common next step when adding a new data store).

Creating a layer
----------------

Now that we have connected to the GeoPackage, we can publish the layer.

#. On the :guilabel:`New Layer` page, click :guilabel:`Publish` beside the ``countries`` :guilabel:`layer name`.

   .. figure:: images/layer_new.png
      
      New Layer

#. The :guilabel:`Edit Layer` page defines the data and publishing parameters for a layer.

   .. figure:: images/layer.png
      
      Edit Layer Data tab
      
   
#. There are three critical pieces of information required on the :guilabel:`Data` tab before we can even save.
   
   * :guilabel:`Basic Resource Info` - describes how the layer is presented to others
   * :guilabel:`Coordinate Reference System` - establishes how the spatial data is to be interpreted or drawn on the world
   * :guilabel:`Bounding Boxes` - establishes where the dataset is located in the world
   
#. Locate :guilabel:`Basic Resource Info` and define the layer:

   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - Name
        - :kbd:`countries`
      * - Title
        - :kbd:`countries`
      * - Abstract
        - :kbd:`Sovereign states`

   The naming of a layer is important, and while GeoServer does not offer restrictions many of the individual protocols will only work with very simple names.
   
   .. figure:: images/layer_basic.png

      Basic Resource Info

#. Double check the :guilabel:`Coordiante Reference Systems` information is correct.

   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - Native SRS
        - :kbd:`EPSG:4326`
      * - Declaired SRS
        - :kbd:`EPSG:4326`
      * - SRS Handling
        - ``Force declared``

   .. figure:: images/layer_crs.png
      
      Coordinate Reference Systems

#. Locate :guilabel:`Bounding Boxes` and generate the layer's bounding boxes by clicking the :guilabel:`Compute from data` and then :guilabel:`Compute from native bounds` links.

   .. figure:: images/layer_bbox.png

      Generating bounding boxes

#. Press :guilabel:`Apply` to save your work thus far without closing the page.
   
   This is a good way to check that your information has been entered correctly, GeoServer will provide a warning if any required information is incomplete.

#. Scroll to the top of the page and navigate to the :guilabel:`Publishing` tab.

#. Locate the :guilabel:`WMS Settings` heading, where we can set the style.Ensure that the :guilabel:`Default Style` is set to ``polygon```.

   .. figure:: images/layer_style.png

      WMS Settings
  
#. Press :guilabel:`Save` to complete your layer edits.

Previewing the layer
--------------------

In order to verify that the ``tutorial:countries`` layer is published correctly, we can preview the layer.

#. Navigate to the :menuselection:`Data > Layer Preview` page and find the ``tutorial:countries`` layer.

   .. note:: Use the :guilabel:`Search` field with :kbd:`tutorial` as shown to limit the number of layers to page through.

   .. figure:: images/preview.png

      Layer Preview

#. Click the :guilabel:`OpenLayers` link in the :guilabel:`Common Formats` column.

#. An OpenLayers map will load in a new tab and display the shapefile data with the default line style.
   
   You can use this preview map to zoom and pan around the dataset, as well as display the attributes of features.

   .. figure:: images/openlayers.png

      Preview map of countries
