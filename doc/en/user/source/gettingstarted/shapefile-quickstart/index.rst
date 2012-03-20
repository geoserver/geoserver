.. _shapefile_quickstart:

Publishing a Shapefile
======================

This tutorial walks through the steps of publishing a Shapefile with GeoServer.

.. note::

   This tutorial assumes that GeoServer is running at http://localhost:8090/geoserver/web.

Getting Started
---------------

#. Download the file :download:`nyc_roads.zip`. This archive contains a Shapefile of roads from New York City that will be used during in this tutorial.

#. Unzip the `nyc_roads.zip`.  The extracted folder ``nyc_roads`` contains the following four files::

      nyc_roads.shp
      nyc_roads.shx
      nyc_roads.dbf
      nyc_roads.prj

#. Move the ``nyc_roads`` folder into ``<GEOSERVER_DATA_DIR>/data``, where ``<GEOSERVER_DATA_DIR>`` is the root of the GeoServer data directory. 
If no changes have been made to the GeoServer file structure, the path is ``geoserver/data_dir/data/nyc_roads``. 
 
Create a New Workspace
----------------------

The first step is to create a *workspace* for the Shapefile. A workspace is a container used to group similar layers together. 


    #. In a web browser navigate to http://localhost:8080/geoserver/web.

    #. Log into GeoServer as described in :ref:`logging_in`.  

    #. Navigate to :menuselection:`Data-->Workspaces`.

	.. figure:: ../../webadmin/images/data_workspaces.png
	   :align: center

	   *Workspaces page*

    #. To create a new workspace click the :guilabel:`Add new workspace` button.  You will be prompted to enter a workspace :guilabel:`Name` and :guilabel:`Namespace URI`.   

	.. figure:: new_workspace.png
	   :align: center

	   *Configure a New Worksapce*

    #. Enter the :guilabel:`Name` as ``nyc_roads`` and the :guilabel:`Namespace URI` as ``http://opengeo.org/nyc_roads``. A workspace name is a identifier describing your project. It must not exceed ten characters or contain spaces.  A Namespace URI (Uniform Resource Identifier) is typically a URL associated with your project, perhaps with an added trailing identifier indicating the workspace.  
	
	.. figure:: workspace_nycroads.png
	   :align: center

	   *NYC Roads Workspace*

    #. Click the :guilabel:`Submit` button. The ``nyc_roads`` workspace will be added to the :guilabel:`Workspaces` list.  

Create a Data Store
-------------------

    #. Navigate to :menuselection:`Data-->Stores`.

    #. In order to add the nyc_roads Shapefile, we need to create a new Store.  Click on the :guilabel:`Add new store` button.  You will be redirected to a list of the data sources supported by GeoServer.  

	.. figure:: stores_nycroads.png
	   :align: center

	   *Data Sources*
	
    #. Select :guilabel:`Shapefile` - *ESRI(tm) Shapefiles (.shp)*.  The :guilabel:`New Vector Data Source` page will display.
	
    #. Begin by configuring the :guilabel:`Basic Store Info`.  Select the workspace ``nyc_roads`` from the drop down menu.  Enter the :guilabel:`Data Source Name` as ``NYC Roads``. and enter a brief :guilabel:`Description` (such as "Roads in New York City").
	
    #. Under :guilabel:`Connection Parameters` specify the location :guilabel:`URL` of the Shapefile as ``file:data/nyc_roads/nyc_roads.shp``.  
	
	.. figure:: new_shapefile.png
	   :align: center

	   *Basic Store Info and Connection Parameters*
	
    #. Click :guilabel:`Save`.  You will be redirected to the :guilabel:`New Layer chooser` page in order to configure the ``nyc_roads`` layer. 
	
Create a Layer 
--------------

   #. On the :guilabel:`New Layer chooser` page, select the layer ``nyc_roads``. 

	.. figure:: new_layer.png
	   :align: center

	   *New Layer chooser*
	
	   
   #. The :guilabel:`Edit Layer` page defines the Data and Publishing parameters for a layer. Enter a short :guilabel:`Title` and an :guilabel:`Abstract` for the ``nyc_roads`` layer. 

	.. figure:: new_data.png
	   :align: center

	   *Basic Resource Information*

	   
   #. Generate the layer's *bounding boxes* by clicking the :guilabel:`Compute from data` and then :guilabel:`Compute from Native bounds.`

	.. figure:: boundingbox.png
	   :align: center

	   *Generate Bounding Boxes*
     
   #. Set the layer's style by switching to the :guilabel:`Publishing` tab.  

   #. Select the :guilabel:`line` style from the :guilabel:`Default Style` drop down list.

	.. figure:: style.png
	   :align: center

	   *Select Default Style*
	
   #. Finalize the layer configuration by scrolling to the bottom of the page and clicking :guilabel:`Save`.

Preview the Layer
-----------------
   #. In order to verify that the ``nyc_roads`` layer is published correctly you can preview the layer.  Navigate to the :guilabel:`Layer Preview` screen and find the ``nyc_roads:nyc_roads`` layer.

	.. figure:: layer_preview.png
	   :align: center

	   *Layer Preview*

   #. Click on the :guilabel:`OpenLayers` link in the :guilabel:`Common Formats` column. 

   #. Success! An OpenLayers map loads in a new page and displays the Shapefile data with the default line style. You can use the Preview Map to zoom and pan around the dataset, as well as display the attributes of features. 

	.. figure:: openlayers.png
	   :align: center

	   *Preview map of nyc_roads*





















