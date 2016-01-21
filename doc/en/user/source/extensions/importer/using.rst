.. _extensions_importer_using:

Using the Importer extension
============================

Here are step-by-step instructions to import multiple shapefiles in one operation. For more details on different types of operations, please see the :ref:`extensions_importer_guireference`

#. Find a directory of shapefiles and copy into your :ref:`datadir`.

   .. note:: You can always use the `Natural Earth Quickstart <http://www.naturalearthdata.com/downloads/>`_ data for this task.

#. Log in as an administrator and navigate to the :guilabel:`Data --> Import Data` page.

#. For select :guilabel:`Spatial Files` as the data source.

   .. figure:: images/using_datasource.png
      
      Data source

#. Click :guilabel:`Browse` to navigate to the directory of shapefiles to be imported.

#. The web-based file browser will show as options your home directory, data directory, and the root of your file system (or drive). In this case, select :guilabel:`Data directory` 

   .. figure:: images/using_directory.png
      
      Directory
     
#. Back on the main form, select :guilabel:`Create new` next to :guilabel:`Workspace`, and enter :kbd:`ne` to denote the workspace.

   .. note:: Make sure the :guilabel:`Store` field reads :guilabel:`Create new` as well.

   .. figure:: images/using_workspace.png
      
      Import target workspace
      
#. Click :guilabel:`Next` to start the import process.

#. On the next screen, any layers available for import will be shown.

   .. note:: Non-spatial files will be ignored.

   .. figure:: images/using_layerlist.png
      
      Import layer list

#. In most cases, all files will be ready for import, but if the the spatial reference system (SRS) is not recognized, you will need to manually input this but clicking :guilabel:`Advanced`

   .. note:: You will need to manually input the SRS if you used the Natural Earth data above. For each layer click on :guilabel:`Advanced` and set reprojection to :kbd:`EPSG:4326`.
   
      .. figure:: images/using_advanced.png
      
         Advanced import settings

#. Check the box next to each layer you wish to import.

   .. figure:: images/using_layerlistchecked.png
      
      Setting the layers to import

#. When ready, click :guilabel:`Import`.

   .. warning:: Don't click :guilabel:`Done` at this point, otherwise the import will be canceled.

#. The results of the import process will be shown next to each layer.

#. When finished, click :guilabel:`Done`.

   .. note:: Recent import processes are listed at the bottom of the page. You may wish to visit these pages to check if any difficulties were encountered during the import process or import additional layers.

   .. figure:: images/using_recent.png
      
      Recent imports