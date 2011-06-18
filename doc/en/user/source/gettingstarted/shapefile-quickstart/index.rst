.. _shapefile_quickstart:

Adding a Shapefile
==================

This tutorial walks through the steps of publishing a Shapefile with GeoServer.

.. note::

   This tutorial assumes that GeoServer is running on http://localhost:8090/geoserver/web.

Getting Started
---------------

#. Download the file :download:`nyc_roads.zip`. This file contains a shapefile of roads from New York City that will be used during in this tutorial.

#. Unzip the `nyc_roads.zip`.  The extracted folder consists of the following four files::

      nyc_roads.shp
      nyc_roads.shx
      nyc_roads.dbf
      nyc_roads.prj

#. Move the nyc_roads folder into ``<GEOSERVER_DATA_DIR>/data`` where ``GEOSERVER_DATA_DIR`` is the root of the GeoServer data directory. If no changes were were made to the GeoServer file structure, the path should be ``geoserver/data_dir/data/nyc_roads``. 
 
Create a New Workspace
----------------------

The first step is to create a *workspace* for the Shapefile. The workspace is a container used to group similar layers together. 


    #. In a web browser navigate to http://localhost:8080/geoserver/web.

    #. Log into GeoServer as described in the :ref:`logging_in` quick start.  

    #. Navigate to :menuselection:`Data-->Workspaces`.

	.. figure:: ../../webadmin/images/data_workspaces.png
	   :align: center

	   *Workspaces page*

    #. To create a new workspace click, select the :guilabel:`Add new workspace` button.  You will be prompted to enter a workspace :guilabel:`Name` and :guilabel:`Namespace URI`.   

	.. figure:: new_workspace.png
	   :align: center

	   *Configure a New Worksapce*

    #. Enter the name ``nyc_roads`` and the URI ``http://opengeo.org/nyc_roads`` A workspace name is a name describing your project and cannot exceed ten characters or contain a space.  A Namespace URI (Uniform Resource Identifier), is typically a URL associated with your project, with perhaps a different trailing identifier.  
	
	.. figure:: workspace_nycroads.png
	   :align: center

	   *NYC Roads Workspace*

    #. Click the :guilabel:`Submit` button. GeoServer will append the nyc_roads workspace to the bottom of the Workspace View list.  

Create a Store
--------------

    #. Navigate to :menuselection:`Data-->Stores`.

    #. In order to add the nyc_roads data, we need to create a new Store.  Click on the :guilabel:`Add new store` button.  You will be redirected to a list of data types GeoServer supports.  

	.. figure:: stores_nycroads.png
	   :align: center

	   *Data Sources*
	
    #. Because nyc_roads is a shapefile, select :guilabel:`Shapefile`: *ESRI(tm) Shapefiles (.shp)*.
	
    #. On the :guilabel:`New Vector Data Source` page begin by configuring the :guilabel:`Basic Store Info`.  Select the workspace nyc_roads from the drop down menu, type ``NYC Roads`` for the name and enter a brief description, such as ``Roads in New York City.``
	
    #. Under the :guilabel:`Connections Parameters` specify the location of the shapefile--``file:data/nyc_roads/nyc_roads.shp``.  
	
	.. figure:: new_shapefile.png
	   :align: center

	   *Data Info and Parameters for nyc_roads*
	
    #. Press Save.  You will be redirected to :guilabel:`New Layer chooser` page in order to configure nyc_roads layer. 
	
Layer Configuration 
-------------------

   #. On the :guilabel:`New Layer chooser` page, select the Layer name nyc_roads. 

	.. figure:: new_layer.png
	   :align: center

	   *New Layer Chooser*
	
   #. The following configuration define the data and publishing parameters for a layer. Enter a short :guilabel:`Title` and :guilabel:`Abstract` for the nyc_roads shapefile. 

	.. figure:: new_data.png
	   :align: center

	   *Basic Resource Information for Shapefile*

   #. Generate the shapefile's *bounds* by clicking the :guilabel:`Compute from data` and then :guilabel:`Compute from Native bounds.`

	.. figure:: boundingbox.png
	   :align: center

	   *Generate Bounding Box*
     
   #. Set the shapefile's *style* by first moving over to the :guilabel:`Publishing` tab.  

   #. The select :guilabel:`line` from the :guilabel:`Default Style` drop down list.

	.. figure:: style.png
	   :align: center

	   *Select Default Style*
	
   #. Finalize your data and publishing configuration by scrolling to the bottom and clicking :guilabel:`Save`.

Preview the Layer
-----------------
   #. In order to verify that the nyc_roads is probably published we will preview the layer.  Navigate to the :guilabel:`Map Preview` and search for the nyc_roads:nyc_roads link.

	.. figure:: layer_preview.png
	   :align: center

	   *Layer Preview*

   #. Click on the :guilabel:`OpenLayers` link under the :guilabel:`Common Formats` column. 

   #. Success! An OpenLayers map should load with the default line style. 

	.. figure:: openlayers.png
	   :align: center

	   *OpenLayers map of nyc_roads*





















