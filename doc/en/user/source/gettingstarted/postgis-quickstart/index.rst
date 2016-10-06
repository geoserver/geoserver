.. _postgis_quickstart:

Publishing a PostGIS table
==========================

This tutorial walks through the steps of publishing a PostGIS table with GeoServer.

.. note:: This tutorial assumes that PostgreSQL/PostGIS has been previously installed on the system and responding on ``localhost`` on port ``5432``, and also that GeoServer is running at ``http://localhost:8080/geoserver``.

Data preparation
----------------

First let's gather that the data that we'll be publishing.

#. Download the file :download:`nyc_buildings.zip`. It contains a PostGIS dump of a dataset of buildings from New York City.

#. Create a PostGIS database called ``nyc``. This can be done with the following commands:

   .. code-block:: console

      createdb nyc
      psql -d nyc -c 'CREATE EXTENSION postgis'

   .. note:: You may need to supply a user name and password with these commands.

#. Extract :file:`nyc_buildings.sql` from :file:`nyc_buildings.zip`.

#. Import :file:`nyc_buildings.sql` into the ``nyc`` database:

   .. code-block:: console

      psql -f nyc_buildings.sql nyc

Creating a new workspace
------------------------

The next step is to create a workspace for the data. A workspace is a container used to group similar layers together.

.. note:: This step is optional if you'd like to use an existing workspace. Usually, a workspace is created for each project, which can include stores and layers that are related to each other.

#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

#. Log into GeoServer as described in the :ref:`logging_in` section. 

#. Navigate to :menuselection:`Data --> Workspaces`.

   .. figure:: ../../data/webadmin/img/data_workspaces.png

      Workspaces page

#. Click the :guilabel:`Add new workspace` button.

#. You will be prompted to enter a workspace :guilabel:`Name` and :guilabel:`Namespace URI`.

   .. figure:: ../shapefile-quickstart/new_workspace.png

      Configure a new workspace

#. Enter the :guilabel:`Name` as ``nyc`` and the :guilabel:`Namespace URI` as ``http://geoserver.org/nyc``.

   .. note:: A workspace name is a identifier describing your project. It must not exceed ten characters or contain spaces. A Namespace URI (Uniform Resource Identifier) can usually be a URL associated with your project with an added trailing identifier indicating the workspace. The Namespace URI filed does not need to resolve to an actual valid web address.

#. Click the :guilabel:`Submit` button. The ``nyc`` workspace will be added to the :guilabel:`Workspaces` list. 

Creating a store
----------------

Once the workspace is created, we are ready to add a new store. The store tells GeoServer how to connect to the shapefile. 

#. Navigate to :menuselection:`Data-->Stores`.
    
#. You should see a list of stores, including the type of store and the workspace that the store belongs to.

   .. figure:: datastores.png

      Adding a new data source

#. Create a new store by clicking the ``PostGIS`` link.

#. Enter the :guilabel:`Basic Store Info`:

   * Select the ``nyc`` :guilabel:`Workspace`
   * Enter the :guilabel:`Data Source Name` as ``nyc_buildings``
   * Add a brief :guilabel:`Description`

   .. figure:: basicStore.png

      Basic Store Info

#. Specify the PostGIS database :guilabel:`Connection Parameters`:

   .. list-table::
      :header-rows: 1 

      * - Option
        - Value
      * - :guilabel:`dbtype`
        - :kbd:`postgis`
      * - :guilabel:`host`
        - :kbd:`localhost`
      * - :guilabel:`port`
        - :kbd:`5432`
      * - :guilabel:`database`
        - :kbd:`nyc`
      * - :guilabel:`schema`
        - :kbd:`public`
      * - :guilabel:`user`
        - :kbd:`postgres`
      * - :guilabel:`passwd`
        - (Password for the ``postgres`` user)
      * - :guilabel:`validate connections`
        - (Checked)

   .. note:: Leave all other fields at their default values.
           
   .. figure:: connectionParameters.png
       
      Connection Parameters

#. Click :guilabel:`Save`. 

Creating a layer
----------------

Now that the store is loaded, we can publish the layer.

#. Navigate to :menuselection:`Data --> Layers`.

#. Click :guilabel:`Add a new resource`.

#. From the :guilabel:`New Layer chooser` menu, select ``nyc:nyc_buidings``.

   .. figure:: newlayerchooser.png

      Store selection

#. On the resulting layer row, select the layer name ``nyc_buildings``. 

   .. figure:: layerrow.png

      New layer selection

#. The :guilabel:`Edit Layer` page defines the data and publishing parameters for a layer. Enter a short :guilabel:`Title` and an :guilabel:`Abstract` for the ``nyc_buildings`` layer.

   .. figure:: basicInfo.png

      Basic Resource Info

#. Generate the layer's bounding boxes by clicking the :guilabel:`Compute from data` and then :guilabel:`Compute from native bounds` links.

   .. figure:: boundingbox.png

      Generating bounding boxes

#. Click the :guilabel:`Publishing` tab at the top of the page.

#. We can set the layer's style here. Under :guilabel:`WMS Settings`, ensure that the :guilabel:`Default Style` is set to :guilabel:`polygon`.

   .. figure:: style.png

      Select Default Style

#. Finalize the layer configuration by scrolling to the bottom of the page and clicking :guilabel:`Save`.

Previewing the layer
--------------------

In order to verify that the ``nyc_buildings`` layer is published correctly, we can preview the layer.

#. Navigate to the :guilabel:`Layer Preview` screen and find the ``nyc:nyc_buildings`` layer.

#. Click the :guilabel:`OpenLayers` link in the :guilabel:`Common Formats` column.

#. An OpenLayers map will load in a new tab and display the shapefile data with the default line style. You can use this preview map to zoom and pan around the dataset, as well as display the attributes of features.

   .. figure:: openlayers.png

      Preview map of nyc_buildings

