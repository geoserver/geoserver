.. _data_arcsde:

ArcSDE
======

.. note:: ArcSDE support is not enabled by default and requires the ArcSDE extension to be installed prior to use.  Please see the section on :ref:`arcsde_install` for details.

ESRI's `ArcSDE <http://www.esri.com/software/arcgis/arcsde/>`_ is a spatial engine that runs on top of a relational database such as Oracle or SQL Server.  GeoServer with the ArcSDE extension supports ArcSDE **versions 9.2 and 9.3**.  It has been tested with **Oracle 10g** and **Microsoft SQL Server 2000 Developer Edition**.  The ArcSDE extension is based on the GeoTools ArcSDE driver and uses the ESRI Java API libraries.  See the `GeoTools ArcSDE page <http://docs.geotools.org/latest/userguide/library/data/arcsde.html>`_ for more technical details.

There are two types of ArcSDE data that can be added to GeoServer:  **vector** and **raster**.

Vector support
--------------

ArcSDE provides efficient access to vector layers, ("featureclasses" in ArcSDE jargon), over a number of relational databases.  GeoServer can set up featuretypes for registered ArcSDE featureclasses and spatial views.  For versioned ArcSDE featureclasses, GeoServer will work on the default database version, for both read and write access.

Transactional support is enabled for featureclasses with a properly set primary key, regardless if the featureclass is managed by a user or by ArcSDE.  If a featureclass has no primary key set, it will be available as read-only.

Raster support
--------------

ArcSDE provides efficient access to multi-band rasters by storing the raw raster data as database blobs, dividing it into tiles and creating a pyramid. It also allows a compression method to be set for the tiled blob data and an interpolation method for the pyramid resampling.

All the bands comprising a single ArcSDE raster layer must have the same pixel depth, which can be one of 1, 4, 8, 16, and 32 bits per sample for integral data types. For 8, 16 and 32 bit bands, they may be signed or unsigned. 32 and 64 bit floating point sample types are also supported.

ArcSDE rasters may also be color mapped, as long as the raster has a single band of data typed 8 or 16 bit unsigned.

Finally, ArcSDE supports raster catalogs.  A raster catalog is a mosaic of rasters with the same spectral properties but instead of the mosaic being precomputed, the rasters comprising the catalog are independent and the mosaic work performed by the application at runtime.

.. list-table::
   :widths: 20 80

   * - **Technical Detail**
     - **Status**
   * - Compression methods
     - LZW, JPEG
   * - Number of bands 
     - Any number of bands except for 1 and 4 bit rasters (supported for single-band only).
   * - Bit depth for color-mapped rasters
     - 8 bit and 16 bit 
   * - Raster Catalogs 
     - Any pixel storage type


.. _arcsde_install:

Installing the ArcSDE extension
-------------------------------

.. warning:: Due to licensing requirements, not all files are included with the extension.  To install ArcSDE support, it is necessary to download additional files.  **Just installing the ArcSDE extension will have no effect.**

GeoServer files
````````````````

#. Download the ArcSDE extension from the `GeoServer download page 
   <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of 
   the GeoServer installation.

Required external files
````````````````````````

There are two files that are required but are not packaged with the GeoServer extension:

.. list-table::
   :widths: 20 80

   * - **File**
     - **Notes**
   * - :file:`jsde_sdk.jar`
     - Also known as :file:`jsde##_sdk.jar` where ``##`` is the version number, such as ``92`` for ArcSDE version 9.2
   * - :file:`jpe_sdk.jar`
     - Also known as :file:`jpe##_sdk.jar` where ``##`` is the version number, such as ``92`` for ArcSDE version 9.2

You should always make sure the :file:`jsde_sdk.jar` and :file:`jpe_sdk.jar` versions match your ArcSDE server version, including 
service pack, although client jar versions higher than the ArcSDE Server version usually work just fine.

These two files are available on your installation of the ArcSDE Java SDK from the ArcSDE installation media
(usually ``C:\Program Files\ArcGIS\ArcSDE\lib``).
They may also be available on ESRI's website if there's a service pack containing them, but this is not
guaranteed. To download these files from ESRI's website:

#. Navigate to `<http://support.esri.com/index.cfm?fa=downloads.patchesServicePacks.listPatches&PID=66>`_
#. Find the link to the latest service pack for your version of ArcSDE
#. Scroll down to :menuselection:`Installing this Service Pack --> ArcSDE SDK --> UNIX` (regardless of your target OS)
#. Download any of the target files (but be sure to match 32/64 bit to your OS)
#. Open the archive, and extract the appropriate JARs.

.. note:: The JAR files may be in a nested archive inside this archive.

.. note:: The :file:`icu4j##.jar` may also be on your ArcSDE Java SDK installation folder, but it is already included as part of the the GeoServer ArcSDE extension and is not necessary to install separately.

#. When downloaded, copy the two files to the :file:`WEB-INF/lib` directory of the GeoServer installation.

After all GeoServer files and external files have been downloaded and copied, restart GeoServer.


Adding an ArcSDE vector data store
----------------------------------

In order to serve vector data layers, it is first necessary to register the ArcSDE instance as a data store in GeoServer.  Navigate to the **New data source** page, accessed from the :ref:`data_webadmin_stores` page in the :ref:`web_admin`. and an option for **ArcSDE** will be in the list of :guilabel:`Vector Data Stores`.

.. note:: If ``ArcSDE`` is not an option in the **Feature Data Set Description** drop down box, the extension is not properly installed.  Please see the section on :ref:`arcsde_install`.

.. figure:: images/arcsdevectorcreate.png
   :align: center

   *ArcSDE in the list of data sources*


Configuring an ArcSDE vector data store
---------------------------------------

The next page contains configuration options for the ArcSDE vector data store.  Fill out the form, then click :guilabel:`Save`. 
   
.. figure:: images/arcsdevectorconfigure.png
   :align: center

   *Configuring a new ArcSDE data store*

.. list-table::
   :widths: 20 10 80

   * - **Option**
     - **Required?**
     - **Description**
   * - ``Feature Data Set ID``
     - N/A
     - The name of the data store as set on the previous page.
   * - ``Enabled``
     - N/A
     - When this box is checked the data store will be available to GeoServer
   * - ``Namespace``
     - Yes
     - The namespace associated with the data store.
   * - ``Description``
     - No
     - A description of the data store.
   * - ``server``
     - Yes
     - The URL of the ArcSDE instance. 	 
   * - ``port``
     - Yes
     - The port that the ArcSDE instance is set to listen to.  Default is 5151.
   * - ``instance``
     - No
     - The name of the specific ArcSDE instance, where applicable, depending on the underlying database.
   * - ``user``
     - Yes
     - The username to authenticate with the ArcSDE instance.	 
   * - ``password``
     - No
     - The password associated with the above username for authentication with the ArcSDE instance.
   * - ``pool.minConnections``
     - No
     - Connection pool configuration parameters. See the :ref:`connection_pooling` section for details.
   * - ``pool.maxConnections``
     - No
     - Connection pool configuration parameters. See the :ref:`connection_pooling` section for details. 
   * - ``pool.timeOut``
     - No
     - Connection pool configuration parameters. See the :ref:`connection_pooling` section for details. 
  
You may now add featuretypes as you would normally do, by navigating to the :guilabel:`New Layer` page, accessed from the :ref:`data_webadmin_layers` page in the :ref:`web_admin`.

Configuring an ArcSDE vector data store with Direct Connect
-----------------------------------------------------------

ESRI Direct Connect[ESRI DC] allows clients to directly connect to an SDE GEODB 9.2+ without a need of an SDE server instance, and is recommended for high availability environments, as it removes the ArcSDE gateway server as a single point of failure.
ESRI DC needs additional platform dependent binary drivers and a working Oracle Client ENVIRONMENT (if connecting to an ORACLE DB). See `Properties of a direct connection to an ArcSDE geodatabase <http://webhelp.esri.com/arcgisserver/9.3/java/index.htm#geodatabases/setting1995868008.htm>`_ in the ESRI ArcSDE documentation for more information on Direct Connect, and `Setting up clients for a direct connection <http://webhelp.esri.com/arcgisserver/9.3/java/index.htm#geodatabases/setting1995868008.htm>`_ for information about connecting to the different databases supported by ArcSDE.

The GeoServer configuration parameters are the same as in the `Configuring an ArcSDE vector data store` section above, with a couple differences in how to format the parameters:

 * server: In ESRI Direct Connect Mode a value must be given or the Direct Connect Driver will throw an error, so just put a 'none' there - any String will work!
 * port: In ESRI Direct Connect Mode the port has a String representation: `sde:oracle10g`, `sde:oracle11g:/:test`, etc. For further information check `ArcSDE connection syntax <http://webhelp.esri.com/arcgisserver/9.3/java/geodatabases/arcsde-2034353163.htm>`_ at the official ArcSDE documentation from ESRI.
 * instance: In ESRI Direct Connect Mode a value must be given or the Direct Connect Driver will throw an error, so just put a 'none' there - any String will work!
 * user: The username to authenticate with the geo database.
 * password: The password associated with the above username for authentication with the geo database.

.. note:: Be sure to assemble the password like: password@<Oracle Net Service name> for Oracle

You may now add featuretypes as you would normally do, by navigating to the New Layer page, accessed from the Layers page in the Web Administration Interface.


Adding an ArcSDE vector data store with JNDI
--------------------------------------------

Configuring an ArcSDE vector data store with JNDI
-------------------------------------------------

Adding an ArcSDE raster coveragestore
-------------------------------------

In order to serve raster layers (or coverages), it is first necessary to register the ArcSDE instance as a store in GeoServer.
Navigate to the **Add new store** page, accessed from the :ref:`data_webadmin_stores` page in the :ref:`web_admin` and an option for 
**ArcSDE Raster Format** will be in list.

.. note:: If ``ArcSDE Raster Format`` is not an option in the **Coverage Data Set Description** drop down box, the extension is not properly installed.  Please see the section on :ref:`arcsde_install`.

.. figure:: images/arcsderastercreate.png
   :align: center

   *ArcSDE Raster in the list of data sources*

Configuring an ArcSDE raster coveragestore
------------------------------------------

The next page contains configuration options for the ArcSDE instance.  Fill out the form, then click :guilabel:`Save`.
   
.. figure:: images/arcsderasterconfigure.png
   :align: center

   *Configuring a new ArcSDE coveragestore*

.. list-table::
   :widths: 20 10 80

   * - **Option**
     - **Required?**
     - **Description**
   * - ``Coverage Data Set ID``
     - N/A
     - The name of the coveragestore as set on the previous page.
   * - ``Enabled``
     - N/A
     - When this box is checked the coveragestore will be available to GeoServer.
   * - ``Namespace``
     - Yes
     - The namespace associated with the coveragestore.
   * - ``Type``
     - No
     - The type of coveragestore.  Leave this to say ``ArcSDE Raster``. 	 
   * - ``URL``
     - Yes
     - The URL of the raster, of the form ``sde://<user>:<pwd>@<server>/#<tableName>``.
   * - ``Description``
     - No
     - A description of the coveragestore.

You may now add coverages as you would normally do, by navigating to the **Add new layer** page, accessed from the :ref:`data_webadmin_layers` page in the :ref:`web_admin`.
