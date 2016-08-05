.. _shapefile_quickstart:

Publishing a shapefile
======================

This tutorial walks through the steps of publishing a Shapefile with GeoServer.

.. note:: This tutorial assumes that GeoServer is running at ``http://localhost:8080/geoserver``.

Data preparation
----------------

First let's gather that the data that we'll be publishing.

#. Download the file :download:`nyc_roads.zip`. This archive contains a shapefile of roads from New York City that will be used during in this tutorial.

#. Unzip the :file:`nyc_roads.zip` into a new directory named :file:`nyc_roads`. The archive contains the following four files::

      nyc_roads.shp
      nyc_roads.shx
      nyc_roads.dbf
      nyc_roads.prj

#. Move the ``nyc_roads`` directory into ``<GEOSERVER_DATA_DIR>/data``, where ``<GEOSERVER_DATA_DIR>`` is the root of the :ref:`GeoServer data directory <datadir>`. If no changes have been made to the GeoServer file structure, the path is ``geoserver/data_dir/data/nyc_roads``. 
 
Creating a new workspace
------------------------

The next step is to create a workspace for the shapefile. A workspace is a container used to group similar layers together.

.. note:: This step is optional if you'd like to use an existing workspace. Usually, a workspace is created for each project, which can include stores and layers that are related to each other.

#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

#. Log into GeoServer as described in the :ref:`logging_in` section. 

#. Navigate to :menuselection:`Data --> Workspaces`.

   .. figure:: ../../data/webadmin/img/data_workspaces.png

      Workspaces page

#. Click the :guilabel:`Add new workspace` button.

#. You will be prompted to enter a workspace :guilabel:`Name` and :guilabel:`Namespace URI`.

   .. figure:: new_workspace.png

      Configure a new workspace

#. Enter the :guilabel:`Name` as ``nyc`` and the :guilabel:`Namespace URI` as ``http://geoserver.org/nyc``.

   .. note:: A workspace name is a identifier describing your project. It must not exceed ten characters or contain spaces. A Namespace URI (Uniform Resource Identifier) can usually be a URL associated with your project with an added trailing identifier indicating the workspace. The Namespace URI filed does not need to resolve to an actual valid web address.

   .. figure:: workspace_nycroads.png

      nyc workspace

#. Click the :guilabel:`Submit` button. The ``nyc`` workspace will be added to the :guilabel:`Workspaces` list.

Create a store
--------------

Once the workspace is created, we are ready to add a new store. The store tells GeoServer how to connect to the shapefile. 

#. Navigate to :menuselection:`Data-->Stores`.
    
#. You should see a list of stores, including the type of store and the workspace that the store belongs to.

#. In order to add the shapefile, you need to create a new store. Click the :guilabel:`Add new Store` button. You will be redirected to a list of the data sources supported by GeoServer. Note that the data sources are extensible, so your list may look slightly different.

   .. figure:: stores_nycroads.png

      Stores
  
#. Click :guilabel:`Shapefile`. The :guilabel:`New Vector Data Source` page will display.

#. Begin by configuring the :guilabel:`Basic Store Info`.

   * Select the workspace ``nyc`` from the drop down menu.
   * Enter the :guilabel:`Data Source Name` as ``NYC Roads``
   * Enter a brief :guilabel:`Description` (such as "Roads in New York City").

#. Under :guilabel:`Connection Parameters`, browse to the location :guilabel:`URL` of the shapefile, typically :file:`nyc_roads/nyc_roads.shp`.
  
   .. figure:: new_shapefile.png

      Basic Store Info and Connection Parameters

#. Click :guilabel:`Save`. You will be redirected to the :guilabel:`New Layer` page in order to configure the ``nyc_roads`` layer. 

Creating a layer
----------------

Now that the store is loaded, we can publish the layer.

#. On the :guilabel:`New Layer` page, click :guilabel:`Publish` beside the ``nyc_roads`` layer name. 

   .. figure:: new_layer.png

      New layer

#. The :guilabel:`Edit Layer` page defines the data and publishing parameters for a layer. Enter a short :guilabel:`Title` and an :guilabel:`Abstract` for the ``nyc_roads`` layer. 

   .. figure:: new_data.png

      Basic Resource Information

#. Generate the layer's bounding boxes by clicking the :guilabel:`Compute from data` and then :guilabel:`Compute from native bounds` links.

   .. figure:: boundingbox.png

      Generating bounding boxes

#. Click the :guilabel:`Publishing` tab at the top of the page.

#. We can set the layer's style here. Under :guilabel:`WMS Settings`, ensure that the :guilabel:`Default Style` is set to :guilabel:`line`.

   .. figure:: style.png

      Select Default Style
  
#. Finalize the layer configuration by scrolling to the bottom of the page and clicking :guilabel:`Save`.

Previewing the layer
--------------------

In order to verify that the ``nyc_roads`` layer is published correctly, we can preview the layer.

#. Navigate to the :guilabel:`Layer Preview` screen and find the ``nyc:nyc_roads`` layer.

   .. figure:: layer_preview.png

      Layer Preview

#. Click the :guilabel:`OpenLayers` link in the :guilabel:`Common Formats` column.

#. An OpenLayers map will load in a new tab and display the shapefile data with the default line style. You can use this preview map to zoom and pan around the dataset, as well as display the attributes of features.

   .. figure:: openlayers.png

      Preview map of nyc_roads
