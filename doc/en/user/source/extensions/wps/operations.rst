.. _wps_operations:

WPS Operations
==============

.. note:: For the official WPS specification, please go to http://www.opengeospatial.org/standards/wps.

WPS defines three main operations for the publishing of geospatial processes.  These operations are modeled on similar operations in WFS and WMS.  They are named:

* GetCapabilities
* DescribeProcess
* Execute

.. _wps_getcaps:

GetCapabilities
---------------

The **GetCapabilities** operation requests the WPS server to provide details of service offerings.  This information includes server metadata and metadata describing all processes implemented.  The response from the service is an XML document called the **capabilities document**.

To make a GetCapabilities request, use the following URL::

  http://localhost:8080/geoserver/ows?
    service=WPS&
    version=1.0.0&
    request=GetCapabilities

This URL assumes that GeoServer is located at ``http://localhost:8080/geoserver/``.

The required parameters, as in all GetCapabilities requests,  are **service** (``service=WPS``), **version** (``version=1.0.0``), and **request** (``request=GetCapabilities``).


DescribeProcess
----------------

The **DescribeProcess** operation makes a request to the WPS server for a full description of a process known to the WPS.

An example GET request (again, assuming a GeoServer at ``http://localhost:8080/geoserver/``) using the process ``JTS:buffer``, would look like this::

  http://localhost:8080/geoserver/ows?
    service=WPS&
    version=1.0.0&
    request=DescribeProcess&
    identifier=JTS:buffer

Here, the important parameter here is the ``identifier=JTS:buffer``, as this defines what process to describe.  Multiple processes can be requested, separated by commas (for example, ``identifier=JTS:buffer,gs:Clip``), but at least one process must be specified.

.. warning:: As with all OGC parameters, the keys (``request``, ``version``, etc) are case insensitive, and the values (``GetCapabilities``, ``JTS:buffer``, etc.) are case sensitive.  GeoServer is generally more relaxed about case, but it is good to be aware of the specification.

The response to this request contains the following information:

.. list-table:: 
   :widths: 20 80 

   * - **Title**
     - "Buffers a geometry using a certain distance"
   * - **Inputs**
     - **distance**: "The distance (same unit of measure as the geometry)" *(double, mandatory)*

       **quadrant segments**: "Number of quadrant segments. Use > 0 for round joins, 0 for flat joins, < 0 for mitred joins" *(integer, optional)*

       **capstyle**: "The buffer cap style, round, flat, square" *(selection, optional)*
   * - **Output formats**
     - One of GML 3.1.1, GML 2.1.2, or WKT

.. note:: The specific processes available in GeoServer are subject to change.

Execute
-------

The **Execute** operation makes a request to the WPS server to perform the actual process.

The inputs required for this request depend on the process being executed.  For more information about WPS processes in GeoServer, please see the section on :ref:`wps_processes`.

This operation is cumbersome to view as a GET request, so below is an example of a POST request.  The specific process takes as an input a point at the origin (described in WKT as ``POINT(0 0)``) and runs a buffer operation (JTS:buffer) of 10 units with single quadrant segments and a flat style, and outputs GML 3.1.1.

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
      <ows:Identifier>JTS:buffer</ows:Identifier>
      <wps:DataInputs>
        <wps:Input>
          <ows:Identifier>geom</ows:Identifier>
          <wps:Data>
            <wps:ComplexData mimeType="application/wkt"><![CDATA[POINT(0 0)]]></wps:ComplexData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>distance</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>10</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>quadrantSegments</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>1</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>capStyle</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>flat</wps:LiteralData>
          </wps:Data>
        </wps:Input>
      </wps:DataInputs>
      <wps:ResponseForm>
        <wps:RawDataOutput mimeType="application/gml-3.1.1">
          <ows:Identifier>result</ows:Identifier>
        </wps:RawDataOutput>
      </wps:ResponseForm>
    </wps:Execute>

The response from such a request would be (numbers rounded for clarity):

.. code-block:: xml

    <?xml version="1.0" encoding="utf-8"?>
    <gml:Polygon xmlns:sch="http://www.ascc.net/xml/schematron"
     xmlns:gml="http://www.opengis.net/gml"
     xmlns:xlink="http://www.w3.org/1999/xlink">
      <gml:exterior>
        <gml:LinearRing>
          <gml:posList>
            10.0 0.0
            0.0 -10.0
            -10.0 0.0 
            0.0 10.0
            10.0 0.0
          </gml:posList>
        </gml:LinearRing>
      </gml:exterior>
    </gml:Polygon>

For help in generating WPS requests, you can use the built-in :ref:`wps_request_builder`.



