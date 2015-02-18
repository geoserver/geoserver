.. _import_using:

Using the Importer extension
============================

The importer extension supports three workflows:

* Import a directory of shapefiles (or other spatial data). You are invited to publish the contents as layers in GeoServer.

* Connect to an existing database and select the tables you wish to publish as layers in GeoServer

* Import a spatial data into a target DataStore (before the results are published as new Layers in GeoServer).

As an example here are the steps needed to import multiple shapefiles.

#. Download the `NaturalEarth Quickstart <http://www.naturalearthdata.com/downloads/>`_ and extract into your GEOSERVER_DATA_DIRECTORY.

#. Login as an administrator and navigate to the gui-label:`Data --> Import Data` page.

#. For step one select select :guilabel:`Spatial Files` as the the data source to import from.

   .. figure:: images/import1-spatial-data.png
      
      Choose Data Source to Import From

#. For step two use gui:label:`Browse` to and navigate to a directory of shape files.

   Start from your GEOSERVER_DATA_DIRECTORY and navigate to the the :file:`110m_cultural` folder.

   .. figure:: images/import2-directory.png
      
      Configure Data Source
      
#. For step three we can select an existing workspace or create new.

   Select :guilabel:`create new` and provide the name :kbd:`ne` for the new workspace.

   .. figure:: images/import3-create.png
      
      Specify the target for import
      
#. Click :guilabel:`Next` to start the import and advance to the next screen.
   
   .. figure:: images/import4-import.png
      
      Import

#. From the import screen we can see the layers available for import.
   
   The natural earth quickstart data does not include :file:`prj` files that can be recognised by GeoServer. For each layer click on :guilabel:`Advanced` and enable reprojection to :kbd:`EPSG:4326`.
   
   .. figure :: images/import5-advanced.png
      
      Advanced Import Settings

#. Select each layer you wish to import using a checkbox and click :guilabel:`Done` at the bottom of the screen.

   .. figure:: images/import6-import.png
      
      Import

#. Click :guilabel:`Done` when finished.
   
   Recent imports are listed at the bottom of the page. You may wish to visit these pages to check if any difficulties were encountered during the import process or import additional layers.
   
   .. figure:: images/import7-done.png
      
      Recent Imports