.. _iauwkt.install:

Installing the IAU authority
----------------------------

The IAU authority is an official extension.  Download the extension here - :download_extension:`iau`

#. Download the extension for your version of GeoServer. 

   .. warning:: Make sure to match the version of the extension to the version of GeoServer.

#. Extract the archive and copy the contents into the GeoServer :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

Verify Installation
^^^^^^^^^^^^^^^^^^^

To verify that the extension was installed successfully:

#. Try to set up a layer (e.g., shapefile) with a IAU CRS (e.g., ``IAU:49900``, Mars (2015) - Sphere / Ocentric)
#. The layer CRS should recognize the CRS and may already provide ``IAU:49900`` in the declared CRS field (or not, depending on whether the original CRS had an authority and code indication, or just a basic definition)
#. Even if not indicated automatically, it should be possible to enter a IAU code and have it accepted as valid.
