.. _tutorial_imagemosaic_timeseries:

Using the ImageMosaic plugin for raster time-series data
========================================================


Introduction
------------

This step-by-step tutorial describes all the steps for building a time-series coverage using the new ImageMosaic plugin. The ImageMosaic plugin allows the creation of a time-series layer of a raster dataset. The single images are hold in a queryable structure in order to access to a specific dataset with a temporal filter.

The concepts explained in :ref:`tutorial_imagemosaic_extension` are required in order to properly understand the steps shown here.

This tutorial is organized in 4 chapter:

* The first chapter, **Configuration**, describes the environment configurations needed before load an Imagemosaic store from geoserver
* The second chapter, **Configuration examples**, describes the details, providing examples, of the configurations files needed.
* The last 2 chapters, **Coverage based on filestore** and **Coverage based on database** describe, once the previous configurations steps are done, how to create and configure an Imagemosaic store using the geoserver GUI.

The dataset used in the tutorial can be downloaded :download:`Here <snowLZWdataset.zip>`. It contains 3 image files and a .tld file representing a style needed for correctly render the images.

Configuration
-------------
In order to load a new CoverageStore from the GeoServer GUI two steps are needed:

1. Create a new directory in which you store all your tif files (the mosaic granules) and three configuration files. This directory rerpresents the **MOSAIC_DIR**.
2. Install and setup a DBMS instance, this DB is that one where the mosaic indexes will be stored.
3. Another important thing is that the web container where geoserver is deployed must have the **timezone properly configured**.

In order to set the time in Coordinated Universal Time (UTC) add this switch when launching the java process::

-Duser.timezone=GMT

If a shapefile is used (see next chapter) also this switch is needed in order to manage properly the timezone::

-Dorg.geotools.shapefile.datetime=true

.. note:: The above properties enables support for timestamp (date and time) data in Shapefile stores. Support for timestamp is not part of the DBF standard, which only supports Date instead, and only few applications understand it. As long as shapefiles are only used for GeoServer input that is not a problem, but the above setting will cause issues if you have WFS enabled and users also download shapefiles as GetFeature output: if the feature type extracted has timestamps the generated shapefile will have as well, making it difficult to use the generated shapefile in desktop applications. As a rule of thumb, if you also need WFS support it is advisable to use an external store (PostGIS, Oracle) instead. Of course, if all that's needed is a date, using shapefile as an index without the above property is fine as well.


MOSAIC_DIR and the Configuration Files
``````````````````````````````````````

The user can name the and place the **MOSAIC_DIR** as and where he wants.

The **MOSAIC_DIR** contains all mosaic granules files and the 3 needed configuration files. The files are in ``.properties`` format.

.. note:: Every tif file must follow the same naming convention. In this tutorial will be used {coveragename}_{timestamp}.tif

In a properties file you specify your properties in a  key-value manner: e.g. `myproperty=myvalue`

The configuration files needed are:

1. **datastore.properties**: contains all relevant information responsible for connecting to the database in which the spatial indexes of the mosaic will be stored
2. **indexer.properties**: specifies the name of the time-variant attribute, the elevation attribute and the type of the attributes
3. **timeregex.properties**: specifies the regular expression used to extract the time information from the filename.

All the configuration files must be placed in the root of the **MOSAIC_DIR**. The granule images could be placed also in **MOSAIC_DIR** subfolders.

Please note that **datastore.properties** isn't mandatory. The plugin provides two possibilities to access to time series data:

* **Using a shapefile** in order to store the granules indexes. That's the default behavior without providing the *datastore.properties* file.
* **Using a DBMS**, which maps the timestamp to the corresponding raster source. The former uses the **time** attribute for access to the granule from the mapping table. 

For production installation is strong reccomended the usage of a DBMS instead of shapefile in order to improve performances. 

Otherwise the usage of a shapefile could be useful in development and test environments due to less configurations are needed.

datastore.properties
""""""""""""""""""""

Here is shown an example of datastore.properties suitable for Postgis.

.. list-table::
   :widths: 15 20 75

   * - **Parameter**
     - **Mandatory**
     - **Description**
   * - *SPI*
     - **Y**
     - The factory class used for the datastore e.g. org.geotools.data.postgis.PostgisNGDataStoreFactory
   * - *host*
     - **Y**
     - The host name of the database.
   * - *port*
     - **Y**
     - The port of the database
   * - *database*
     - **Y**
     - The name/instance of the database.
   * - *schema*
     - **Y**
     - The name of the database schema.
   * - *user*
     - **Y**
     - The database user.
   * - *passwd*
     - **Y**
     - The database password.    
   * - *Loose bbox*
     - **N** default 'false'
     - Boolean value to specify if loosing the bounding box.    
   * - *Estimated extend*
     - **N** default 'true'
     - Boolean values to specify if the extent of the data should be estimated.    
   * - *validate connections*
     - **N** default 'true'
     - Boolean value to specify if the connection should be validated.    
   * - *Connection timeout*
     - **N** default '20'
     - Specifies the timeout in minutes.    
   * - *preparedStatements*
     - **N** default 'false'
     - Boolean flag that specifies if for the database queries prepared statements should be used. This improves performant, because the database query parser has to parse the query only once     

.. note:: The first 8 parameters are valid for each DBMS used, the last 4 may vary from different DBMS. for more informations see `GeoTools JDBC documentation <http://docs.geotools.org/latest/userguide/library/jdbc/index.html>`_
 
indexer.properties
""""""""""""""""""
.. list-table::
   :widths: 15 5 80

   * - **Parameter**
     - **Mandatory**
     - **Description**
   * - *TimeAttribute*
     - N
     - Specifies the name of the time-variant attribute
   * - *ElevationAttribute*
     - N
     - Specifies the name of the elevation attribute.
   * - *Schema*
     - Y
     - A coma separed sequence that describes the mapping between attribute and the data type.
   * - *PropertyCollectors*
     - Y
     - Specifies the extractor classes.

.. warning:: **TimeAttribute** is not a mandatory param but for the purpose of this tutorial is needed.
	 
timeregex.properties
""""""""""""""""""""
.. list-table::
   :widths: 15 5 80

   * - **Parameter**
     - **Mandatory**
     - **Description**
   * - *regex*
     - Y
     - Specifies the pattern used for extracting the information from the file

After this you can create a new imagemosaic datastore.

Install and setup a DBMS instance
`````````````````````````````````
First of all note that the usage of a DBMS to store the mosaic indexes **isn't mandatory**. If the user don't place in the MOSAIC_DIR the datastore.properties file the plugin uses a **shapefile**. The shapefile will be created into the MOSAIC_DIR.

Anyway, especially for large dataset, **the usage of a DBMS is strong recommended**. The ImageMosaic plugin supports all the most used DBMS. 

The configuration needed are the basics: create a new empty DB with geospatial extensions, a new schema and configure the user with W/R grants.

In this tutorial will be used PostgreSQL 9.1 together with PostGIS 2.0 .


Configuration examples
---------------------- 

As example is used a set of data that represents hydrological data in a certain area in South Tyrol, a region in northern Italy. The origin data were converted from asc format to tiff using the gdal utility **gdal translate**. 

For this running example we will create a layer named snow.

As mentioned before the files could located in any part of the file system.

In this tutorial the chosen MOSAIC_DIR direcory is called ``hydroalp`` and is placed under the root of the GEOSERVER_DATA_DIR.


Configure the MOSAIC_DIR:
`````````````````````````
In this part is shown an entire MOSAIC_DIR configuration.


datastore.properties:
"""""""""""""""""""""
.. include:: src/datastore.properties
   :literal:

.. note:: In case of a missing datastore.properties file a shape file is created for the use of the indexes.


Granules Naming Convenction
"""""""""""""""""""""""""""
Here an example of the granules naming that satisfy the rule shown before:

.. include:: src/tiffiles.out
   :literal:


timeregex.properties:
"""""""""""""""""""""
In the timeregex property file you specify the pattern how the date(time) in the file looks like. In this example it consists simply of 8 digits as specified below. 

.. include:: src/timeregex.properties
   :literal:


indexer.properties:
"""""""""""""""""""
Here the user can specify the information that needs Geoserver for creating the table in the database. In this table the time values are stored in the column ingestion.

.. include:: src/indexer.properties
   :literal:

   
Create and Publish an Imagemosaic store:
----------------------------------------

Step 1: create new imagemosaic data store
`````````````````````````````````````````
We create a new data store of type raster data and choose ImageMosaic.

.. figure:: img/step_1_1.png
   :align: center


.. note:: Be aware that Geoserver creates a table which is identical with the name of your layer. If the table already exists, it will not be dropped from the DB and the following error message appear. The same message appear, if the generated property file already exists in the directory or there are wrong connection parameters in datastore.properties file.

.. figure:: img/errormessage.png
   :align: center



Step 2: Specify layer
```````````````````````
We specify the directory in which the property and tif files are located (path must end with a slash) and add the layer. 

.. figure:: img/step_1_2.png
   :align: center

   
Step 3: set Coverage Parameter
``````````````````````````````
The relevant parameters are AllowMultithreading and USE_JAI_IMAGEREAD. Do not forget to specify the background value according to your the value in your tif file.   

.. figure:: img/step_2_1.png
   :align: center

Remember that for display correctly the images contained in the provided dataset a custom style is needed.

Set as default style the *snow_style.tld* contained in the dataset archive.

More information about raster styling can be found at chapter :ref:`sld_cookbook_rasters`
   
Step 4: set temporal properties
```````````````````````````````
In the tab Dimensions you can specify how the time attributes are represented. 

By enabling the Time or Elevation checkbox you can specify the way of presentation. 
In this example query is performed just only over the time attribute. 

Below is shown a snippet of the Capabilities document for each presentation case:

Setting the presentation to **List**, all mosaic times are listed:

.. code-block:: xml

	<Dimension name="time" default="current" units="ISO8601">
		2009-10-01T00:00:00.000Z,2009-11-01T00:00:00.000Z,2009-12-01T00:00:00.000Z,2010-01-01T00:00:00.000Z,2010-02-01T00:00:00.000Z,2010-03-01T00:00:00.000Z,2010-04-01T00:00:00.000Z,2010-05-01T00:00:00.000Z,2010-06-01T00:00:00.000Z,2010-07-01T00:00:00.000Z,2010-08-01T00:00:00.000Z,2010-09-01T00:00:00.000Z,2010-10-01T00:00:00.000Z,2010-11-01T00:00:00.000Z,2010-12-01T00:00:00.000Z,2011-01-01T00:00:00.000Z,2011-02-01T00:00:00.000Z,2011-03-01T00:00:00.000Z,2011-04-01T00:00:00.000Z,2011-05-01T00:00:00.000Z,2011-06-01T00:00:00.000Z,2011-07-01T00:00:00.000Z,2011-08-01T00:00:00.000Z,2011-09-01T00:00:00.000Z
	</Dimension>
	
Setting the presentation to **Continuos interval** only the start, end and interval extent times are listed:

.. code-block:: xml

	<Dimension name="time" default="current" units="ISO8601">
		2009-10-01T00:00:00.000Z/2011-09-01T00:00:00.000Z/P1Y11MT10H
	</Dimension>


Setting the presentation to **Interval and resolutions** gives to user the possibility to specify the resolutions of the interval:

.. code-block:: xml

	<Dimension name="time" default="current" units="ISO8601">
		2009-10-01T00:00:00.000Z/2011-09-01T00:00:00.000Z/P1DT12H
	</Dimension>

In this case the resolution is set to one day and half

.. note:: For visualize the getCapabilities document go to geoserver homepage, under the rigt tab called **Service Capabilities** click on the WMS 1.3.0 link.

For this tutorial the Presentation attribute is set to **List**

.. figure:: img/step_2_2.png
   :align: center


After this steps the new layer is available in Geoserver. Additionally Geoserver has created in the source directory a property file and on the database he has created a table named with the name of the layer.


Generated property file:
""""""""""""""""""""""""

.. include:: src/snow.properties
   :literal:

.. note:: The parameter **Caching=false** is important because in this way GeoServer doesn't cache any data. So the user is able to update manually the mosaic adding and removing granules to MOSAIC_DIR and update the relative entry on DB.
   
Generated table:
""""""""""""""""

.. figure:: img/step_2_3.png
   :align: center


.. note:: The user must create manually the index on the table in order to speed up the search by attribute.
   
Step 5: query layer on timestamp: 
`````````````````````````````````

In order to display a snapshot of the map at a specific time instant you have to pass in the request an addtional time parameter with a specific notation
**&time=** < **pattern** > where you pass a value that corresponds to them in the filestore. The only thing is the pattern of the time value is slightly different.

For example if an user wants to obtain the snow coverage images from the months **Oct,Nov,Dec 2009** I pass in each request **&time=2009-10-01**, **&time=2009-11-01** and **&time=2009-12-01**. You can recognize in the three images how the snow coverage changes. Here the color blue means a lot of snow.


.. figure:: img/step_3.png
   :align: center
   

Create and publish a Layer from mosaic indexes:
-----------------------------------------------

After the previous steps it is also be possible to create a layer that represents the spatial indexes of the mosaic. This is an useful features when is required to handle large dataset of Mosaics with High Resolutions granules, the user can easilly get the footprints of the Images. In this case will be rendered only the geometries stored on the indexes tables.

Step 1: add a postgis datastore:
````````````````````````````````
.. figure:: img/choose_datasource.png
   :align: center

and specify the connection parameters

.. figure:: img/create_postgis_store.png
   :align: center

   
Step 2: add database layer:
```````````````````````````

Choose from the created datastore the table that you want to publish as a layer.

.. figure:: img/step_4_1.png
   :align: center

   
Step 3: specify dimension:
````````````````````````````
In the tab Dimension specify the time-variant attribute and the form of presentation.

.. figure:: img/step_4_2.png
   :align: center
  
That's it. Now is possible query this layer too. 
