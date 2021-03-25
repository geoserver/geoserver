.. _wmts_multidimensional_install:

Installing the WMTS multidimensional extension
==============================================

The WMS multidimensional extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

#. Download the :download_extension:`wmts-multi-dimensional`
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

If installation was successful, the WTMS capabilities document will contain the extra requests
provided by the module, e.g.:

.. code-block:: xml 

    <ows:Operation name="DescribeDomains">
      <ows:DCP>
        <ows:HTTP>
          <ows:Get xlink:href="http://localhost:8080/geoserver/gwc/service/wmts?">
            <ows:Constraint name="GetEncoding">
              <ows:AllowedValues>
                <ows:Value>KVP</ows:Value>
              </ows:AllowedValues>
            </ows:Constraint>
          </ows:Get>
        </ows:HTTP>
      </ows:DCP>
    </ows:Operation>
    <ows:Operation name="GetDomainValues">
      <ows:DCP>
        <ows:HTTP>
          <ows:Get xlink:href="http://localhost:8080/geoserver/gwc/service/wmts?">
            <ows:Constraint name="GetEncoding">
              <ows:AllowedValues>
                <ows:Value>KVP</ows:Value>
              </ows:AllowedValues>
            </ows:Constraint>
          </ows:Get>
        </ows:HTTP>
      </ows:DCP>
    </ows:Operation>

A simple ``DescribeDomains`` request can be used to test if the module was correctly installed, the request can be made against any layer known by the WMTS service. For example, using the demo layer ``tiger:poly_landmarks`` shipped with GeoServer: 

.. code-block:: guess

  http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=DescribeDomains&Version=1.0.0&Layer=tiger:poly_landmarks&TileMatrixSet=EPSG:4326


The result should be similar to the following, this layer doesn't have any domains:

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?><Domains xmlns="http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd" xmlns:ows="http://www.opengis.net/ows/1.1">
    <SpaceDomain>
      <BoundingBox CRS="EPSG:4326" minx="0.0" miny="0.0" maxx="-1.0" maxy="-1.0"/>
    </SpaceDomain>
  </Domains>

If the module is not correctly installed the result will be an exception saying that this operation is not available:

.. code-block:: xml

  <ExceptionReport version="1.1.0" xmlns="http://www.opengis.net/ows/1.1"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/ows/1.1 http://geowebcache.org/schema/ows/1.1.0/owsExceptionReport.xsd">
    <Exception exceptionCode="OperationNotSupported" locator="request">
      <ExceptionText>describedomains is not implemented</ExceptionText>
    </Exception>
  </ExceptionReport>