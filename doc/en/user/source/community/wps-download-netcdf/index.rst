.. _wpsdownloadnetcdf:

WPS Download NetCDF
===================

WPS Download NetCDF module provides the ability to download a SpatioTemporal coverage in NetCDF format.

Installing the WPS Download NetCDF module
-----------------------------------------

The WPS Download NetCDF module depends on 3 extensions:

* :ref:`wps`
* :ref:`wpsdownload`
* :ref:`netcdf_out`

To install:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.
   
#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **OGC Services** extensions download **WPS**.

   * |release| example: :download_extension:`wps`
   * |version| example: :nightly_extension:`wps`
   

   From the list of **OGC Services** extensions download **WPS Download**.

   * |release| example: :download_extension:`wps-download`
   * |version| example: :nightly_extension:`wps-download`

#. From the list of **Output Formats** extensions download **NetCDF**.

   * |release| example: :download_extension:`netcdf-out`
   * |version| example: :nightly_extension:`netcdf-out`
 
#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Following the **Community Modules** link and download **wps-download-netcdf** zip archive.
   
   * |version| example: :nightly_community:`wps-download-netcdf`

   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract all the contents of the archives into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. Restart GeoServer


Module description
------------------
This module adds further capabilities to the :ref:`community_wpsrawdownload` Raw Data Download process of the :ref:`wpsdownload` extension, by supporting dimension-aware filtering and NetCDF output.
It enables users to download spatio-temporal grid coverages (such as climate or remote sensing data, for example) in NetCDF format, 
while filtering the data based on specific dimension values—like time, elevation, or other custom-defined dimensions.
The filter is applied directly to the multidimensional data source. For example, in the case of an ImageMosaic, 
it targets the mosaic index to select multiple matching slices, which are then combined into the final NetCDF output file.

Relevant parameters of the Raw Data Download process:

#. layerName - name of the raster layer that has enabled dimensions (i.e. time, elevation)
#. filter - a filter containing subsetting on the attributes associated to the enabled dimensions
#. outputFormat - The mimeType of the requested output: ``application/x-netcdf`` or ``application/x-netcdf4`` (if NetCDF-4 is enabled)


Example
^^^^^^^
Assuming that a *it.geosolutions:temperature* layer exists containing data for water temperature at different times 
(associated to *ingestion* attribute) and different elevations (associated to *elevation* attribute), 
the following example DataInputs is used to collect all the data within the specified temporal range and the specified elevation.
(note that the dimensions need to be enabled to be used by the filtering machinery)


.. code-block:: xml

  <ows:Identifier>gs:Download</ows:Identifier>
  <wps:DataInputs>
    <wps:Input>
      <ows:Identifier>layerName</ows:Identifier>
      <wps:Data>
        <wps:LiteralData>it.geosolutions:watertemperature</wps:LiteralData>
      </wps:Data>
    </wps:Input>
    <wps:Input>
      <ows:Identifier>filter</ows:Identifier>
      <wps:Data>
        <wps:ComplexData mimeType="text/plain; subtype=cql"><![CDATA[ingestion >= '2013-09-11T00:00:00.000Z' and ingestion <= '2013-09-12T00:00:00.000Z' and elevation=0]]></wps:ComplexData>
      </wps:Data>
    </wps:Input>
    <wps:Input>
      <ows:Identifier>outputFormat</ows:Identifier>
      <wps:Data>
        <wps:LiteralData>application/x-netcdf</wps:LiteralData>
      </wps:Data>
    </wps:Input>
  </wps:DataInputs>

Limits
^^^^^^
By default, the number of "granules/slices" returned by the underlying source, using the specified filter, is limited to 1000. 
This value can be modified by adding this property to the ``JAVA_OPTS``: ``-Dwps.download.netcdf.max.features=MAX_FEATURES``

Writing parameters
------------------
By default, the Settings configured in the NetCDF Output Settings of the layer (i.e. DataPacking, Compression level, Chunk Shuffling, 
variable names) are as Default writing parameters. Optionally, they can be overridden via explicit writeParameters input.

The ``writeParameters`` input element of a process execution allows to specify parameters to be applied by the ``outputFormat`` encoder when producing the output file.
Writing parameters are listed as multiple ``<dwn:Parameter key="writingParameterName">value</dwn:Parameter>`` within a ``<dwn:Parameters>`` parent element.
See the below xml containing full syntax of a valid example for NetCDF output format:

.. code-block:: xml

    <wps:Input>
      <ows:Identifier>writeParameters</ows:Identifier>
        <wps:Data>
           <wps:ComplexData xmlns:dwn="http://geoserver.org/wps/download">
             <dwn:Parameters>
               <dwn:Parameter key="compression">5</dwn:Parameter>
               <dwn:Parameter key="shuffle">true</dwn:Parameter>
               <dwn:Parameter key="dataPacking">SHORT</dwn:Parameter>
               <dwn:Parameter key="variableName">air_temperature</dwn:Parameter>
               <dwn:Parameter key="uom">K</dwn:Parameter>
               <dwn:Parameter key="copyGlobalAttributes">true</dwn:Parameter>
               <dwn:Parameter key="copyVariableAttributes">true</dwn:Parameter>
             </dwn:Parameters>
           </wps:ComplexData>
        </wps:Data>
    </wps:Input>

Note that the Shuffle and Compression parameters take effect only when the NetCDF-4 encoding is requested (using the application/x-netcdf4 MIME type).

NetCDF supported writing parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The supported writing parameters are:

 * ``compression`` : NetCDF-4 Lossless compression level. An integer between 0 (no compression, fastest) and 9 (most compression, slowest).
 * ``shuffle`` : Apply Lossless byte reordering to improve NetCDF-4 compression.
 * ``dataPacking``: Lossy compression by storing data in reduced precision. One of NONE, BYTE, SHORT, or INT.
 * ``variableName`` : Set the NetCDF variable name in the output file.
 * ``uom`` : Set the NetCDF uom attribute for the output variable.
 * ``copyGlobalAttributes`` : If the input layer is based on NetCDF/GRIB sources, copy attributes from the source NetCDF/GRIB global attributes.
 * ``copyVariableAttributes`` : If the input layer is based on NetCDF/GRIB sources, copy attributes from the source NetCDF/GRIB variableSet.

Note on NetCDF PPIO supported by the module:
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
This module also add a NetCDF3PPIO and NetCDF4PPIO that enable the encoding of GranuleStack outputs into NetCDF formats
(NetCDF-3 and NetCDF-4) through WPS. 
While currently no other WPS processes directly output GranuleStack, this capability is now generically supported. 
Future processes producing GranuleStack results can take advantage of this without needing additional PPIO setup.





