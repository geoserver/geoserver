.. _wcs_configuration:

WCS configuration
=================

Coverage processing
-------------------

The WCS processing chain can be tuned in respect of how raster overviews and read subsampling are used.

The overview policy has four possible values:

.. list-table::
   :widths: 10 80 10

   * - **Option**
     - **Description**
     - **Version**
   * - **Lower resolution overview**
     - Looks up the two overviews with a resolution closest to the one requested and chooses the one at the lower resolution.
     - 2.0.3
   * - **Don't use overviews**
     - Overviews will be ignored, the data at its native resolution will be used instead. This is the default value.
     - 2.0.3
   * - **Higher resolution overview**
     - Looks up the two overviews with a resolution closest to the one requested and chooses the one at the higher resolution.
     - 2.0.3
   * - **Closest overview**
     - Looks up the overview closest to the one requested
     - 2.0.3
     
While reading coverage data at a resolution lower than the one available on persistent storage its common to use subsampling, that is, read one every N pixels as a way to reduce the resolution of the data read in memory. **Use subsampling** controls wheter subsampling is enabled or not.


Request limits
--------------

The request limit options allow the administrator to limit the resources consumed by each WCS ``GetCoverage`` request.

The request limits limit the size of the image read from the source and the size of the image returned to the client. Both of these limits are to be considered a worst case scenario and are setup to make sure the server never gets asked to deal with too much data.

.. list-table::
   :widths: 10 80 10

   * - **Option**
     - **Description**
     - **Version**
   * - **Maximum input memory**
     - Sets the maximum amount of memory, in kilobytes, a GetCovearge request might use, at most, to read a coverage from the data source. The memory is computed as ``rw * rh * pixelsize``, where ``rw`` and ``rh`` are the size of the raster to be read and ``pixelsize`` is the dimension or a pixel (e.g., a RGBA image will have 32bit pixels, a batimetry might have 16bit signed int ones)
     - 2.0.3
   * - **Maximum output memory**
     - Sets the maximum amount of memory, in kilobytes, a GetCoverage request might use, at most, to host the resulting raster. The memory is computed as ``ow * oh * pixelsize``, where ``ow`` and ``oh`` are the size of the raster to be generated in output.
     - 2.0.3
   * - **Max number of dimension values**
     - Sets the maximum number of dimension (time, at least for now) values that a client can request in a GetCoverage request (the work to be done is usually proportional to said number of times, and the list of values is kept in memory during the processing)
     - 2.14.0

     
To understand the limits let's consider a very simplified examle in which no tiles and overviews enter the game:

* The request hits a certain area of the original raster. Reading it at full resolution requires grabbing a raster of size ``rw * rh``, which has a certain number of bands, each with a certain size. The amount of memory used for the read will be ``rw * rh * pixelsize``. This is the value measured by the input memory limit
* The WCS performs the necessary processing: band selection, resolution change (downsampling or upsampling), reprojection
* The resuling raster will have size ``ow * oh`` and will have a certain number of bands, possibly less than the input data, each with a certain size. The amount of memory used for the final raster will be ``ow * oh * pixelsize``. This is the value measured by the output memory limit.
* Finally the resulting raster will be encoded in the output format. Depending on the output format structure the size of the result might be higher than the in memory size (ArcGrid case) or smaller (for example in the case of GeoTIFF output, which is normally LZW compressed)

In fact reality is a bit more complicated:

* The input source might be tiled, which means there is no need to fully read in memory the region, but it is sufficient to do so one tile at a time. The input limits won't consider inner tiling when computing the limits, but if all the input coverages are tiled the input limits should be designed considering the amount of data to be read from the persistent storage as opposed to the amount of data to be stored in memory
* The reader might be using overviews or performing subsampling during the read to avoid actually reading all the data at the native resolution should the output be subsampled
* The output format might be tile aware as well (GeoTIFF is), meaning it might be able to write out one tile at a time. In this case not even the output raster will be stored in memory fully at any given time.

Only a few input formats are so badly structure that they force the reader to read the whole input data in one shot, and should be avoided. Examples are:
* JPEG or PNG images with world file
* Single tiled and JPEG compressed GeoTIFF files





