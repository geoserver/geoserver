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
  #8000 px X 8000 px X 3 bands X 1 byte per band = 192MB
  writeLimits=192000000
  # 50 MB
  hardOutputLimit=52428800
  # STORE =0, BEST =8
  compressionLevel=4
  
Where the available limits are:

 * ``maxFeatures`` : maximum number of features to download
 * ``rasterSizeLimits`` : maximum pixel size of the Raster to read
 * ``writeLimits`` : maximum raw raster size in bytes (a limit of how much space can a raster take in memory). For a given raster, its raw size in bytes is calculated by multiplying pixel number (raster_width x raster_height) with the accumated sum of each band's pixel sample_type size in bytes, for all bands
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

 * ``layerName`` : the name of the layer to process/download
 * ``filter`` : a vector filter for filtering input data(optional)
 * ``outputFormat`` : the MIME type of the format of the final file
 * ``targetCRS`` : the CRS of the output file (optional)
 * ``RoiCRS`` : Region Of Interest CRS (optional)
 * ``ROI`` : Region Of Interest object to use for cropping data (optional)
 * ``cropToROI`` : boolean parameter to allow cropping to actual ROI, or its envelope (optional)
 * ``interpolation`` : interpolation function to use when reprojecting / scaling raster data.  Values are NEAREST (default), BILINEAR, BICUBIC2, BICUBIC (optional)
 * ``targetSizeX`` : size X in pixels of the output (optional, applies for raster input only)
 * ``targetSizeY`` : size Y in pixels of the output (optional, applies for raster input only)
 * ``selectedBands`` : a set of the band indices of the original raster that will be used for producing the final result (optional, applies for raster input only)

The ``targetCRS`` and ``RoiCRS`` parameters are using EPSG code terminology, so, valid parameters are literals like ``EPSG:4326`` (if we are referring to a the  Geogaphic WGS84 CRS), ``EPSG:3857`` (for WGS84 Web Mercator CRS), etc.

ROI Definition
++++++++++++++++++++++

A ``ROI`` parameter is a geometry object which can also be defined if three different forms:

 * as ``TEXT``, in various geometry textual formats/representations
 * as ``REFERENCE``, which is the textual result of an HTTP GET/POST request to a specific url
 * as a ``SUPPROCESS`` result: the format produced as result of the process execution must be a compatible geometry textual format. 

As noted above, in all above forms/cases ROI geometry is defined as text, in specific formats. These can be:

 * ``text/xml; subtype=gml/3.1.1``: conforming to gml specs 3.1.1
 * ``text/xml; subtype=gml/2.1.2``: conforming to gml specs 2.1.2 
 * ``application/wkt``: the WKT geometry representation
 * ``application/json``: the JSON geometry representation
 * ``application/gml-3.1.1``: conforming to gml specs 3.1.1
 * ``application/gml-2.1.2``: conforming to gml specs 2.1.2
 
For example, a polygon used as ROI with the following WKT representation:

``POLYGON (( 500116.08576537756 499994.25579707103, 500116.08576537756 500110.1012210889, 500286.2657688021 500110.1012210889, 500286.2657688021 499994.25579707103, 500116.08576537756 499994.25579707103 ))``

would be represented in the following forms:

 * in application/wkt: ``POLYGON (( 500116.08576537756 499994.25579707103, 500116.08576537756 500110.1012210889, 500286.2657688021 500110.1012210889, 500286.2657688021 499994.25579707103, 500116.08576537756 499994.25579707103 ))``
 * in application/json: ``{"type":"Polygon","coordinates":[[[500116.0858,499994.2558],[500116.0858,500110.1012],[500286.2658,500110.1012],[500286.2658,499994.2558],[500116.0858,499994.2558]]]}``
 * in text/xml:``500116.08576537756,499994.25579707103 500116.08576537756,500110.1012210889 500286.2657688021,500110.1012210889 500286.2657688021,499994.25579707103 500116.08576537756,499994.25579707103``
 * in application/xml: the following xml

 .. code-block:: xml
  
  <?xml version="1.0" encoding="UTF-8"?><gml:Polygon xmlns:gml="http://www.opengis.net/gml" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xlink="http://www.w3.org/1999/xlink">
    <gml:outerBoundaryIs>
      <gml:LinearRing>
        <gml:coordinates>500116.08576537756,499994.25579707103 500116.08576537756,500110.1012210889 500286.2657688021,500110.1012210889 500286.2657688021,499994.25579707103 500116.08576537756,499994.25579707103</gml:coordinates>
      </gml:LinearRing>
    </gml:outerBoundaryIs>
  </gml:Polygon>
  
The general structure of a WPS Download request POST payload consists of two parts: the first (``<wps:DataInputs>``) contains the input parameters for the process, and the second (``<wps:ResponseForm>``) contains details about delivering the output. A typical pseudo payload is the following:

 .. code-block:: xml
 
  <?xml version="1.0" encoding="UTF-8"?><wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
   <ows:Identifier>gs:WPS_Process_Name_Here</ows:Identifier>
   <wps:DataInputs>
    <wps:Input>
     <ows:Identifier>First_Param_Name</ows:Identifier>
     <wps:Data>
       (First_Param_Data)
     </wps:Data>
    </wps:Input>
    ...
    ...
   </wps:DataInputs>
   <wps:ResponseForm>
    <wps:RawDataOutput mimeType="application/zip">
     <ows:Identifier>result</ows:Identifier>
    </wps:RawDataOutput>
   </wps:ResponseForm>
  </wps:Execute>
  
Each parameter for the process is defined in its own ``<wps:Input>`` xml block. In case of simple type data, such as layerName, outputFormat, targetCRS, etc, input params xml blocks have the following form:

 .. code-block:: xml
 
    <wps:Input>
     <ows:Identifier>layerName</ows:Identifier>
     <wps:Data>
      <wps:LiteralData>nurc:Img_Sample</wps:LiteralData>
     </wps:Data>
    </wps:Input>

  
Note the ``<wps:LiteralData>`` tags wrapping the parameter value.
In case of geometry parameters, such as filter, ROI, the parameter's ``<wps:Input>`` block is different:

 .. code-block:: xml
 
    <wps:Input>
      <ows:Identifier>ROI</ows:Identifier>
      <wps:Data>
        <wps:ComplexData mimeType="application/wkt"><![CDATA["POLYGON (( 500116.08576537756 499994.25579707103, 500116.08576537756 500110.1012210889, 500286.2657688021 500110.1012210889, 500286.2657688021 499994.25579707103, 500116.08576537756 499994.25579707103 ))]]></wps:ComplexData>
      </wps:Data>
    </wps:Input>

  
Note the ``<wps:ComplexData>`` tag, the ``mimeType="application/wkt"`` parameter, and the ``![CDATA[]`` wrapping of the actual geometry data (in textual representation, according to the selected MIME type.

In case the ROI is defined using a REFENENCE source, the input block is slightly different:

 .. code-block:: xml

    <wps:Input>
      <ows:Identifier>ROI</ows:Identifier>
      <wps:Reference mimeType="application/wkt" xlink:href="url_to_fetch_data" method="GET"/>
    </wps:Input>

  
Note the ``<wps:Reference>`` tag replacing ``<wps:ComplexData>`` tag, and the extra ``xlink:href="url_to_fetch_data"`` parameter, which defines the url to peform the HTTP GET request. For POST request cases, tech method is switched to POST, and a ``<wps:Body>`` tag is used to wrap POST data:

 .. code-block:: xml

    <wps:Reference mimeType="application/wkt" xlink:href="url_to_fetch_data" method="POST">
      <wps:Body><![CDATA[request_body_data]]></wps:Body>
    </wps:Reference>

Filter parameter definition
++++++++++++++++++++++++++++

A ``filter`` parameter is a definition of a vector filter operation:

 * as ``TEXT``, in various textual formats/representations
 * as ``REFERENCE``, which is the textual result of an HTTP GET/POST request to a specific url
 * as a ``SUPPROCESS`` result: the format produced as result of the process execution must be a compatible geometry textual format. 
 
Compatible text formats for filter definitions are:

 * ``text/xml; filter/1.0``
 * ``text/xml; filter/1.1``
 * ``text/xml; cql``

For more details on filter formats/languages, one can see :doc:`../../filter/syntax`  and :doc:`../../filter/function`. 
Filter parameter applies to vector data. If this is the case with input data, a sample ``<wps:Input>`` block of a filter intersecting the  polygon we used earlier as an example for ROI definition would be: 

 .. code-block:: xml

    <wps:Input>
      <ows:Identifier>filter</ows:Identifier>
      <wps:Data>
        <wps:ComplexData mimeType="text/plain; subtype=cql"><![CDATA[<Intersects>
           <PropertyName>GEOMETRY</PropertyName>
             <gml:Polygon>
               <gml:outerBoundaryIs>
                 <gml:LinearRing>
                    <gml:coordinates>500116.08576537756,499994.25579707103 500116.08576537756,500110.1012210889 500286.2657688021,500110.1012210889 500286.2657688021,499994.25579707103 500116.08576537756,499994.25579707103</gml:coordinates>
                  </gml:LinearRing>
               </gml:outerBoundaryIs>
             </gml:Polygon>
         </Intersects>]]></wps:ComplexData>
      </wps:Data>
    </wps:Input>

  
  
Sample request
+++++++++++++++++
Synchronous execution
^^^^^^^^^^^^^^^^^^^^^

The following is a sample WPS request for processing a raster dataset. 
Suppose we want to use the North America sample imagery (**nurc:Img_Sample**) layer, to produce an **80x80** pixels downloadable **tiff** in **EPSG:4326**

Assuming that a local geoserver instance (setup for wps/wps-download support) is running, we issue a POST request to the url:

``http://127.0.0.1:8080/geoserver/ows?service=wps``

using the following payload:

 .. code-block:: xml
 
  <?xml version="1.0" encoding="UTF-8"?><wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
   <ows:Identifier>gs:Download</ows:Identifier>
   <wps:DataInputs>
    <wps:Input>
     <ows:Identifier>layerName</ows:Identifier>
     <wps:Data>
      <wps:LiteralData>nurc:Img_Sample</wps:LiteralData>
     </wps:Data>
    </wps:Input>
    <wps:Input>
     <ows:Identifier>outputFormat</ows:Identifier>
     <wps:Data>
      <wps:LiteralData>image/tiff</wps:LiteralData>
     </wps:Data>
    </wps:Input>
    <wps:Input>
     <ows:Identifier>targetCRS</ows:Identifier>
     <wps:Data>
      <wps:LiteralData>EPSG:4326</wps:LiteralData>
     </wps:Data>
    </wps:Input>
    <wps:Input>
     <ows:Identifier>targetSizeX</ows:Identifier>
     <wps:Data>
      <wps:LiteralData>80</wps:LiteralData>
     </wps:Data>
    </wps:Input>
    <wps:Input>
     <ows:Identifier>targetSizeY</ows:Identifier>
     <wps:Data>
      <wps:LiteralData>80</wps:LiteralData>
     </wps:Data>
    </wps:Input>
   </wps:DataInputs>
   <wps:ResponseForm>
    <wps:RawDataOutput mimeType="application/zip">
     <ows:Identifier>result</ows:Identifier>
    </wps:RawDataOutput>
   </wps:ResponseForm>
  </wps:Execute>

  
More parameters (from the parameter list above) can be used, for example, we can only select bands **0 and 2** from the original raster: 

 .. code-block:: xml
 
   <wps:Input>
    <ows:Identifier>bandIndices</ows:Identifier>
    <wps:Data>
     <wps:LiteralData>0</wps:LiteralData>
    </wps:Data>
   </wps:Input>
   <wps:Input>
    <ows:Identifier>bandIndices</ows:Identifier>
    <wps:Data>
     <wps:LiteralData>2</wps:LiteralData>
    </wps:Data>
   </wps:Input>

  
Or, use a **Region Of Interest** to crop the dataset:
  
 .. code-block:: xml
 
    <wps:Input>
      <ows:Identifier>ROI</ows:Identifier>
      <wps:Data>
        <wps:ComplexData mimeType="application/wkt"><![CDATA["POLYGON (( 500116.08576537756 499994.25579707103, 500116.08576537756 500110.1012210889, 500286.2657688021 500110.1012210889, 500286.2657688021 499994.25579707103, 500116.08576537756 499994.25579707103 ))]]></wps:ComplexData>
      </wps:Data>
    </wps:Input>
    <wps:Input>
      <ows:Identifier>RoiCRS</ows:Identifier>
      <wps:Data>
        <wps:LiteralData>EPSG:32615</wps:LiteralData>
      </wps:Data>
    </wps:Input>

The result produced is a zipped file to download.


Asynchronous execution
^^^^^^^^^^^^^^^^^^^^^^
The process can also be performed asynchronously.
In this case, the second part (``wps:ResponseForm``) of the wps download payload slightly changes, by using the **storeExecuteResponse** and **status** parameters, set to **true** for the ``<wps:ResponseDocument>``:

 .. code-block:: xml

  <wps:ResponseForm>
    <wps:ResponseDocument storeExecuteResponse="true" status="true">
      <wps:RawDataOutput mimeType="application/zip">
        <ows:Identifier>result</ows:Identifier>
      </wps:RawDataOutput>
    </wps:ResponseDocument>>
  </wps:ResponseForm>

  
In case of asynchronous execution, the initial request to download data returns an xml indication that the process has successfully started:

 .. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?><wps:ExecuteResponse xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:xlink="http://www.w3.org/1999/xlink" xml:lang="en" service="WPS" serviceInstance="http://127.0.0.1:8080/geoserver/ows?" statusLocation="http://127.0.0.1:8080/geoserver/ows?service=WPS&amp;version=1.0.0&amp;request=GetExecutionStatus&amp;executionId=dd0d61f5-7da3-41ed-bd3f-15311fa660ba" version="1.0.0">
    <wps:Process wps:processVersion="1.0.0">
        <ows:Identifier>gs:Download</ows:Identifier>
        <ows:Title>Enterprise Download Process</ows:Title>
        <ows:Abstract>Downloads Layer Stream and provides a ZIP.</ows:Abstract>
    </wps:Process>
    <wps:Status creationTime="2016-08-08T11:03:18.167Z">
        <wps:ProcessAccepted>Process accepted.</wps:ProcessAccepted>
    </wps:Status>
  </wps:ExecuteResponse>

The response contains a ``<wps:Status>`` block indicating successfull process creation and process start time. However, the important part in this response is the **executionId=dd0d61f5-7da3-41ed-bd3f-15311fa660ba** attribute in the ``<wps:ExecuteResponse>`` tag. The ``dd0d61f5-7da3-41ed-bd3f-15311fa660ba`` ID can be used as a reference for this process, in order to issue new GET requests and to check process status. These requests have the form:

``http://127.0.0.1:8080/geoserver/ows?service=WPS&request=GetExecutionStatus&executionId=277e24eb-365d-42e1-8329-44b8076d4fc0``

When issued (and process has finished on the server), this GET request returns the result to download/process as a base64 encoded zip:

 .. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <wps:ExecuteResponse xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:xlink="http://www.w3.org/1999/xlink" xml:lang="en" service="WPS" serviceInstance="http://127.0.0.1:8080/geoserver/ows?" statusLocation="http://127.0.0.1:8080/geoserver/ows?service=WPS&amp;version=1.0.0&amp;request=GetExecutionStatus&amp;executionId=0c596a4d-7ddb-4a4e-bf35-4a64b47ee0d3" version="1.0.0">
    <wps:Process wps:processVersion="1.0.0">
        <ows:Identifier>gs:Download</ows:Identifier>
        <ows:Title>Enterprise Download Process</ows:Title>
        <ows:Abstract>Downloads Layer Stream and provides a ZIP.</ows:Abstract>
    </wps:Process>
    <wps:Status creationTime="2016-08-08T11:18:46.015Z">
        <wps:ProcessSucceeded>Process succeeded.</wps:ProcessSucceeded>
    </wps:Status>
    <wps:ProcessOutputs>
        <wps:Output>
            <ows:Identifier>result</ows:Identifier>
            <ows:Title>Zipped output files to download</ows:Title>
            <wps:Data>
                <wps:ComplexData encoding="base64" mimeType="application/zip">UEsDBBQACAgIAFdyCEkAAAAAAAAAAAAAAAApAAAAMGEwYmJkYmQtMjdkNi00...(more zipped raster data following, ommited for space saving)...</wps:ComplexData>
            </wps:Data>
        </wps:Output>
    </wps:ProcessOutputs>
  </wps:ExecuteResponse>

Asynchronous execution (output as a reference)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The ``<wps:ResponseForm>`` of the previous asynchronous request payload example can be modified to get back a link to the file to be downloaded instead of the base64 encoded data.

 .. code-block:: xml

  ...
  <wps:ResponseForm>
    <wps:ResponseDocument storeExecuteResponse="true" status="true">
      <wps:Output asReference="true" mimeType="application/zip">
        <ows:Identifier>result</ows:Identifier>
      </wps:Output>
    </wps:ResponseDocument>
  </wps:ResponseForm>

Note ``<wps:ResponseDocument>`` contains a ``<wps:Output>`` instead of a ``<wps:RawDataOutput>`` being used by previous example. 
Moreover the attribute **asReference** set to **true** has been added to the ``<wps:Output>``.

This time, when issued (and process has finished on the server), the GET request returns the result to download as a link as part of ``<wps:Output><wps:Reference>`` .

 .. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
    <wps:ExecuteResponse xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:xlink="http://www.w3.org/1999/xlink" xml:lang="en" service="WPS" serviceInstance="http://127.0.0.1:8080/geoserver/ows?" statusLocation="http://127.0.0.1:8080/geoserver/ows?service=WPS&amp;version=1.0.0&amp;request=GetExecutionStatus&amp;executionId=c1074100-446a-4963-94ad-cbbf8b8a7fd1" version="1.0.0">
    <wps:Process wps:processVersion="1.0.0">
      <ows:Identifier>gs:Download</ows:Identifier>
      <ows:Title>Enterprise Download Process</ows:Title>
      <ows:Abstract>Downloads Layer Stream and provides a ZIP.</ows:Abstract>
    </wps:Process>
    <wps:Status creationTime="2016-08-08T11:38:34.024Z">
      <wps:ProcessSucceeded>Process succeeded.</wps:ProcessSucceeded>
    </wps:Status>
    <wps:ProcessOutputs>
      <wps:Output>
        <ows:Identifier>result</ows:Identifier>
        <ows:Title>Zipped output files to download</ows:Title>
        <wps:Reference href="http://127.0.0.1:8080/geoserver/ows?service=WPS&amp;version=1.0.0&amp;request=GetExecutionResult&amp;executionId=c1074100-446a-4963-94ad-cbbf8b8a7fd1&amp;outputId=result.zip&amp;mimetype=application%2Fzip" mimeType="application/zip" />
      </wps:Output>
    </wps:ProcessOutputs>
  </wps:ExecuteResponse>
