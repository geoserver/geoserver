.. _grib:

GRIB
====

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

-----------------------------

.. note:: Note that internally the GRIB extension uses the NetCDF reader, which supports also GRIB data. See also the :ref:`netcdf` documentation page for further information.
 
 
Current limitations
-------------------

* Input coverages/slices should share the same bounding box (lon/lat coordinates are the same for the whole ND cube)
