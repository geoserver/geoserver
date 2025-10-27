.. _grib:

GRIB
====

Installing
----------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Coverage Formats** extensions download **GRIB**.

   * |release| example: :download_extension:`grib`
   * |version| example: :nightly_extension:`grib`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the files in the archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

#. Restart GeoServer


Adding a GRIB data store
--------------------------
To add a GRIB data store the user must go to :guilabel:`Stores --> Add New Store --> GRIB`.

.. figure:: gribcreate.png
   :align: center

   *GRIB in the list of raster data stores*

Configuring a GRIB data store
-------------------------------

.. figure:: gribconfigure.png
   :align: center

   *Configuring a GRIB data store*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``Workspace``
     - 
   * - ``Data Source Name``
     - 
   * - ``Description``
     - 
   * - ``Enabled``
     -  
   * - ``URL``
     - 

Relationship with NetCDF
------------------------

.. note:: Note that internally the GRIB extension uses the NetCDF reader, which supports also GRIB data. See also the :ref:`netcdf` documentation page for further information.
 
 
Current limitations
-------------------

* Input coverages/slices should share the same bounding box (lon/lat coordinates are the same for the whole ND cube)
