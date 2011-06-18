.. _postgis_quickstart:

Adding a PostGIS Table
======================

This tutorial walks through the steps of publishing a PostGIS table with GeoServer.

.. note::

   This tutorial assumes that GeoServer is running on http://localhost:8080/geoserver.

.. note::

   This tutorial assumes PostGIS has been previously installed on the system.

Getting started
---------------

#. Download the zip file :download:`nyc_buildings.zip`. It contains a PostGIS dump of a subset of buildings from New York City that will be used during in this tutorial.

#. Create a PostGIS database called "nyc". This can be done with the following command line::

         createdb -T template_postgis nyc

   If the PostGIS install is not set up with the "postgis_template" then the following sequence of commands will perform the equivalent::

        ...

#. Unzip ``nyc_buildings.zip`` to some location on the file system. This will result in the file ``nyc_buildings.sql``. 

#. Import ``nyc_buildings.sql`` into the ``nyc`` database::

         psql -f nyc_buildings.sql nyc


Create a new data store
-----------------------

The first step is to create a *data store* for the PostGIS database "nyc". The data store tells GeoServer how to connect to the database.

    #. In a web browser navigate to http://localhost:8080/geoserver.

    #. Navigate to :menuselection:`Data-->Stores`.

	.. figure:: datastores.png
	   :align: center

	   *Adding a New Data Source*

    #. Create a new data store by clicking the ``PostGIS NG`` link.

    #. Keeping the default :guilabel:`Workspace` enter :guilabel:`Basic Store Info` of Name and Description.

	.. figure:: basicStore.png
	   :align: center

	   *Basic Store Info*

    #. Specify the PostGIS database :guilabel:`Connection Parameters`

       .. list-table::

          * - ``dbtype``
            - postgisng
          * - ``host``
            - localhost
          * - ``post``
            - 5432
          * - ``database``
            - nyc
          * - ``schema``
            - public
          * - ``user``
            - postgres
          * - ``passwd``
            - enter postgres password
          * - ``validate connections``
            - enable with check box

       .. note::

          The **username** and **password** parameters specific to the user who created the postgis database. Depending on how PostgreSQL is configured the password parameter may be unnecessary.
           
		.. figure:: connectionParameters.png
		   :align: center

		   *Connection Parameters*

    #. Click the ``Save`` button.

Layer Configuration 
-------------------

    #. Navigate to :menuselection:`Data-->Layers`.

    #. Select :guilabel:`Add a new resource` button.
	
    #. From the :guilabel:`New Layer chooser` drop down menu, select cite:nyc_buidings.
	
	.. figure:: newlayerchooser.png
	   :align: center

	   *New Layer drop down selection*	
	
    #. On the resulting layer row, select the Layer name nyc_buildings. 

	.. figure:: layerrow.png
	   :align: center

	   *New Layer row*
	
    #. The following configurations define the data and publishing parameters for a layer. Enter the :guilabel:`Basic Resource Info` for nyc_buildings.  
	
	.. figure:: basicInfo.png
	   :align: center

	   *Basic Resource Info*
	
    #. Generate the database *bounds* by clicking the :guilabel:`Compute from data` and then :guilabel:`Compute from Native bounds.`
	
	.. figure:: boundingbox.png
	   :align: center

	   *Generate Bounding Box*
	
    #. Set the layer's *style* by first moving over to the :guilabel:`Publishing` tab.  

    #. The select :guilabel:`polygon` from the :guilabel:`Default Style` drop down list.

	.. figure:: style.png
	   :align: center

	   *Select Default Style*

    #. Finalize your data and publishing configuration by scrolling to the bottom and clicking :guilabel:`Save`.

Preview the Layer
-----------------

    #. In order to verify that the nyc_building is probably published we will preview the layer.  Navigate to the :guilabel:`Map Preview` and search for the cite:nyc_buildings link.

	.. figure:: layer-preview.png
	   :align: center

	   *Layer Preview*

    #. Click on the :guilabel:`OpenLayers` link under the :guilabel:`Common Formats` column. 

    #. Success! An OpenLayers map should load with the default polygon style. 

	.. figure:: openlayers.png
	   :align: center

	   *OpenLayers map of nyc_buildings*
	
	
	
	
	
	
	
	
