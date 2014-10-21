.. _community_wpsdownload:

WPS download community module
=============================

WPS download module provides some useful features for easily downloading Raster or Vectorial layer as zip files, also controlling the output file size.

Installing the WPS download module
-----------------------------------

#. Download the WPS download module from the `nightly GeoServer community module builds <http://ares.boundlessgeo.com/geoserver/master/community-latest/>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance.

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Module description
------------------

This module provides two new WPS process:

	* ``gs:Download`` : this process can be used for downloading Raster and Vector Layers
	* ``gs:DownloadEstimator`` : this process can be used for checking if the downloaded file does not exceeds the configured limits.
	

Configuring the limits
++++++++++++++++++++++

The first step to reach for using this module is to create a new file called **download.properties** and save it in the GeoServer data directory. If the file is not present
GeoServer will automatically create a new one with the default properties:

	.. code-block:: xml
	
		# Max #of features
		maxFeatures=100000
		#8000 px X 8000 px
		rasterSizeLimits=64000000
		#8000 px X 8000 px (USELESS RIGHT NOW)
		writeLimits=64000000
		# 50 MB
		hardOutputLimit=52428800
		# STORE =0, BEST =8
		compressionLevel=4
		
Where the available limits are:

	* ``maxFeatures`` : maximum number of features to download
	* ``rasterSizeLimits`` : maximum pixel size of the Raster to read
	* ``writeLimits`` : maximum pixel size of the Raster to write (currently not used)
	* ``hardOutputLimit`` : maximum file size to download
	* ``compressionLevel`` : compression level for the output zip file

.. note:: Note that limits can be changed when GeoServer is running. Periodically the server will reload the properties file.
		
Download Estimator Process
+++++++++++++++++++++++++++

The *Download Estimator Process* checks the size of the file to download. This process takes in input the following parameters:

	* ``layername`` : name of the layer to check
	* ``ROI`` : ROI object to use for cropping data
	* ``filter`` : filter for filtering input data
	* ``targetCRS`` : CRS of the final layer if reprojection is needed

This process will return a boolean which will be **true** if the downloaded file will not exceed the configured limits.
	
Download Process
++++++++++++++++++++++

The *Download Process* calls the *Download Estimator Process*, checks the file size, and, if the file does not exceed the limits, download the file as a zip.
The parameters to set are 

	* ``layername`` : name of the layer to check
	* ``format`` : format of the final file
	* ``ROI`` : ROI object to use for cropping data
	* ``filter`` : filter for filtering input data
	* ``targetCRS`` : CRS of the final layer if reprojection is needed

The available format for the process are:

	* **Raster**:
		
		* `image/tiff`

	* **Vector**:
		
		* `application/json`
		* `application/wfs-collection-1.1`
		* `application/wfs-collection-1.0`
		
	* **Both**

		* `application/zip`
		
The result is a file to download.