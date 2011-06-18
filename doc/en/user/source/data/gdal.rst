.. _data_gdal:

GDAL Image Formats
==================

GeoServer can leverage the `ImageIO-ext <https://imageio-ext.dev.java.net>`_ GDAL libraries to read selected coverage formats. `GDAL <http://www.gdal.org>`_ is able to read many formats, but for the moment GeoServer supports only a few general interest formats and those that can be legally redistributed and operated in an open source server.

The following image formats can be read by GeoServer using GDAL:

* DTED, Military Elevation Data (.dt0, .dt1, .dt2): http://www.gdal.org/frmt_dted.html
* EHdr, ESRI .hdr Labelled: <http://www.gdal.org/frmt_various.html#EHdr>
* ENVI, ENVI .hdr Labelled Raster: <http://www.gdal.org/frmt_various.html#ENVI>
* HFA, Erdas Imagine (.img): <http://www.gdal.org/frmt_hfa.html>
* JP2MrSID, JPEG2000 (.jp2, .j2k): <http://www.gdal.org/frmt_jp2mrsid.html>
* MrSID, Multi-resolution Seamless Image Database: <http://www.gdal.org/frmt_mrsid.html>
* NITF: <http://www.gdal.org/frmt_nitf.html>
* ECW, ERDAS Compressed Wavelets (.ecw): <http://www.gdal.org/frmt_ecw.html>
* JP2ECW, JPEG2000 (.jp2, .j2k): http://www.gdal.org/frmt_jpeg2000.html
* AIG, Arc/Info Binary Grid: <http://www.gdal.org/frmt_various.html#AIG>
* JP2KAK, JPEG2000 (.jp2, .j2k): <http://www.gdal.org/frmt_jp2kak.html>

Installing GDAL
---------------

GDAL is not a standard GeoServer extension, as the GDAL library files are built into GeoServer by default.  However, in order for GeoServer to leverage these libraries, the GDAL (binary) program itself must be installed through your host system's OS.  Once this program is installed, GeoServer will be able to recognize GDAL data types. In order to install the GDAL Native libraries:

#. Navigate to the `imageio-ext document and files download page <http://java.net/projects/imageio-ext/downloads>`_.
#. Select the most recent stable binary release.
#. Select "native libraries".
#. Download and extract/install the correct version for your OS.

   .. note:: If you are on Windows, make sure that the GDAL DLL files are on your PATH. If you are on Linux, be sure to set the LD_LIBRARY_PATH environment variable to be the folder where the SOs are extracted.

#. Select "libraries" from the last stable release root.
#. Download and extract the gdal_data-1.X.X archive.

   .. note:: Make sure to set a GDAL_DATA environment variable to the folder where you have extracted this file.

Once these steps have been completed, restart GeoServer.  If done correctly, new data formats will be in the :guilabel:`Raster Data Sources` list when creating a new data store.

.. figure:: images/gdalcreate.png
   :align: center

   *GDAL image formats in the list of raster data stores*
   
Note on running GeoServer as a Service on Windows
-------------------------------------------------
Simply deploying the GDAL ImageI/O-Ext native libraries in a location referred by the PATH environment variable (like, as an instance, the JDK/bin folder) doesn't allow GeoServer to leverage on GDAL, when run as a service. As a result, during the service startup, GeoServer log reports this worrysome message:

*it.geosolutions.imageio.gdalframework.GDALUtilities loadGDAL
WARNING: Native library load failed.java.lang.UnsatisfiedLinkError: no gdaljni in java.library.path*

Taking a look at the wrapper.conf configuration file available inside the GeoServer installation (at bin/wrapper/wrapper.conf), there is this useful entry:

# Java Library Path (location of Wrapper.DLL or libwrapper.so)
wrapper.java.library.path.1=bin/wrapper/lib

To allow the GDAL native DLLs getting loaded, you have 2 possible ways:

#. Move the native DLLs on the referred path (bin/wrapper/lib)
#. Add a wrapper.java.library.path.2=path/where/you/deployed/nativelibs entry just after the wrapper.java.library.path1=bin/wrapper/lib line.

Adding support for ECW and Kakadu
---------------------------------

Configuring a DTED data store
-----------------------------

.. figure:: images/gdaldtedconfigure.png
   :align: center

   *Configuring a DTED data store*

Configuring a EHdr data store
-----------------------------

.. figure:: images/gdalehdrconfigure.png
   :align: center

   *Configuring a EHdr data store*

Configuring a ERDASImg data store
---------------------------------

.. figure:: images/gdalerdasimgconfigure.png
   :align: center

   *Configuring a ERDASImg data store*

Configuring a JP2MrSID data store
---------------------------------

.. figure:: images/gdaljp2mrsidconfigure.png
   :align: center

   *Configuring a JP2MrSID data store*

Configuring a NITF data store
-----------------------------

.. figure:: images/gdalnitfconfigure.png
   :align: center

   *Configuring a NITF data store*


