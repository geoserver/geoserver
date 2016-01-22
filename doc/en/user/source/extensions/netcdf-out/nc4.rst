:orphan:

.. _nc4:

Installing required NetCDF-4 Native libraries
=============================================
In order to write NetCDF-4 files, you must have the NetCDF-4 C library (version 4.3.1 or above) available on your system, along with all supporting libraries (HDF5, zlib, etc). 
The following sections provide quick reference installation instructions.
For more detailed info, please take a look at the `NetCDF-4 C Library Loading <http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/netcdf4Clibrary.html>`_ page.


Windows install
---------------
#. Download the latest NetCDF4 installer from the `NetCDF-C Windows Libraries <http://www.unidata.ucar.edu/software/netcdf/docs/winbin.html>`_ page.
#. Install the executable
#. Make sure to add the *bin* folder of the package you have extracted, to the ``PATH`` environment variable.

Linux install
-------------
#. Download the latest required dependencies (SZIP, ZLIB, HDF5) from the `NetCDF-4 libraries section <ftp://ftp.unidata.ucar.edu/pub/netcdf/netcdf-4/>`_.

   As an instance:
     .. code-block:: bash
    
       wget ftp://ftp.unidata.ucar.edu/pub/netcdf/netcdf-4/hdf5-1.8.13.tar.gz
       wget ftp://ftp.unidata.ucar.edu/pub/netcdf/netcdf-4/zlib-1.2.8.tar.gz

#. Download the latest NetCDF-C source code from `here <https://github.com/Unidata/netcdf-c/releases/>`_.

   As an instance:
     .. code-block:: bash
    
       wget https://github.com/Unidata/netcdf-c/archive/v4.3.3.1.tar.gz
       
#. Build and install the required dependencies (The following instructions assume that you will install all NetCDF4 C libs on ``/work/libs/nc4libs``, as an instance).

   #. ZLIB
       .. code-block:: bash
    
         ./configure --prefix=/work/libs/nc4libs
         
         make check install
   
   #. HDF5
       .. code-block:: bash
       
         ./configure --with-zlib=/work/libs/nc4libs --prefix=/work/libs/nc4libs --enable-threadsafe --with-pthread=/DIR/TO/PTHREAD
         
         make check install

#. Build the NetCDF C Library.

    .. code-block:: bash
    
       CPPFLAGS=-I/work/libs/nc4libs/include LDFLAGS=-L/work/libs/nc4libs/lib ./configure --prefix=/work/libs/nc4libs
       
       make check install

#. Make sure to add the *lib* folder of the package you have extracted, to the ``PATH`` environment variable.

GeoServer startup checks
------------------------
If everything has been properly configured, you may notice a similar log message during GeoServer startup:

| ``NetCDF-4 C library loaded (jna_path='null', libname='netcdf').``
| ``Netcdf nc_inq_libvers='4.3.1 of Jan 16 2014 15:04:00 $' isProtected=true``
|

In case the native libraries haven't been properly configured you will see a message like this, instead:

``NetCDF-4 C library not present (jna_path='null', libname='netcdf').``


Requesting a NetCDF4-Classic output file as WCS2.0 output
=========================================================
Specifying application/x-netcdf4 as Format, will return a NetCDF4-Classic output files, provided that the underlying libraries are available.
