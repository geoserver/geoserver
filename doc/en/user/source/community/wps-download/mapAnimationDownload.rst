.. _community_wpsrendereddownload:

Rendered map/animation download processes
------------------------------------------

These processes allow download large maps and animations.

The rendered download processes
+++++++++++++++++++++++++++++++

The map and animation downloads work off a set of common parameters:

 * ``bbox`` : a geo-referenced bounding box, controlling both output area and desired projection
 * ``decoration`` : the name of a decoration layout to be added on top of the map
 * ``time`` : a WMS ``time`` specification used to drive the selection of times across the layers in the map, and to control the frame generation in the animation
 * ``width`` and ``height`` : size of the output map/animation (and in combination with bounding box, also controls the output map scale)
 * ``layer``: a list of layer specifications, from a client side point of view (thus, a layer can be composed of multiple server side layers)

The layer specification
+++++++++++++++++++++++

A layer specification is a XML structure made of three parts:

 * Name: a comma separated list of layer names (eventually just one)
 * Capabilities: link to a capabilities document (optional, used when targetting remote WMS layers)
 * Parameter (key, value): an extra parameter to be added in the WMS request represented by this layer (e.g., ``elevation``, ``CQL_FILTER``, ``env``)

For example:

.. code-block:: xml

    <wps:ComplexData xmlns:dwn="http://geoserver.org/wps/download">
      <dwn:Layer>
        <dwn:Capabilities>http://demo.geo-solutions.it/geoserver/ows?service=wms&amp;version=1.1.1&amp;request=GetCapabilities</dwn:Name>
        <dwn:Name>topp:states</dwn:Name>
        <dwn:Parameter key="CQL_FILTER"><![CDATA[PERSONS > 1000000]]></dwn:Parameter>
      </dwn:Layer>
    </wps:ComplexData>

Sample DownloadMap requests
++++++++++++++++++++++++++++

The map download process has only the basic inputs described above, the ``time`` parameter is optional.
The map download process uses the WMS machinery to produce the output, but it's not subject to the WMS service
limits (width and height in this process can be limited using the WPS process security).

A download map issued against a set of local layers can look as follows:

 .. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <wps:Execute version="1.0.0" service="WPS"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0"
                 xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0"
                 xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml"
                 xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1"
                 xmlns:xlink="http://www.w3.org/1999/xlink"
                 xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
      <ows:Identifier>gs:DownloadMap</ows:Identifier>
      <wps:DataInputs>
        <wps:Input>
          <ows:Identifier>bbox</ows:Identifier>
          <wps:Data>
            <wps:BoundingBoxData crs="EPSG:4326">
              <ows:LowerCorner>0.237 40.562</ows:LowerCorner>
              <ows:UpperCorner>14.593 44.55</ows:UpperCorner>
            </wps:BoundingBoxData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>time</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>2008-10-31T00:00:00.000Z</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>width</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>200</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>height</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>80</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>layer</ows:Identifier>
          <wps:Data>
            <wps:ComplexData xmlns:dwn="http://geoserver.org/wps/download">
              <dwn:Layer>
                <dwn:Name>giantPolygon</dwn:Name>
                <dwn:Parameter key="featureId">giantPolygon.0</dwn:Parameter>
              </dwn:Layer>
            </wps:ComplexData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>layer</ows:Identifier>
          <wps:Data>
            <wps:ComplexData xmlns:dwn="http://geoserver.org/wps/download">
              <dwn:Layer>
                <dwn:Name>watertemp</dwn:Name>
              </dwn:Layer>
            </wps:ComplexData>
          </wps:Data>
        </wps:Input>
      </wps:DataInputs>
      <wps:ResponseForm>
        <wps:RawDataOutput mimeType="image/png">
          <ows:Identifier>result</ows:Identifier>
        </wps:RawDataOutput>
      </wps:ResponseForm>
    </wps:Execute>

For this example the layers could have been a single one, with a "Name" equal to "giantPolygon,watertermp".

Sample DownloadAnimation request
++++++++++++++++++++++++++++++++

The download animation has all the basic parameters with the following variants/additions:

* time: The time parameter is required and can be provided either as range with periodicity, ``start/stop/period``, or
  as a comma separated list of times,``t1,t2,...,tn`` 
* fps: Frame per seconds (defaults to one)

A sample animation request can look as follows:

 .. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <wps:Execute version="1.0.0" service="WPS"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0"
                 xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0"
                 xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml"
                 xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1"
                 xmlns:xlink="http://www.w3.org/1999/xlink"
                 xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
      <ows:Identifier>gs:DownloadAnimation</ows:Identifier>
      <wps:DataInputs>
        <wps:Input>
          <ows:Identifier>bbox</ows:Identifier>
          <wps:Data>
            <wps:BoundingBoxData crs="EPSG:4326">
              <ows:LowerCorner>-180 -90</ows:LowerCorner>
              <ows:UpperCorner>180 90</ows:UpperCorner>
            </wps:BoundingBoxData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>decoration</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>formattedTimestamper</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>time</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>2004-02-01,2004-03-01,2004-04-01,2004-05-01</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>width</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>271</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>height</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>136</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>fps</ows:Identifier>
          <wps:Data>
            <wps:LiteralData>0.5</wps:LiteralData>
          </wps:Data>
        </wps:Input>
        <wps:Input>
          <ows:Identifier>layer</ows:Identifier>
          <wps:Data>
            <wps:ComplexData xmlns:dwn="http://geoserver.org/wps/download">
              <dwn:Layer>
                <dwn:Name>sf:bmtime</dwn:Name>
              </dwn:Layer>
            </wps:ComplexData>
          </wps:Data>
        </wps:Input>
      </wps:DataInputs>
      <wps:ResponseForm>
        <wps:RawDataOutput mimeType="video/mp4">
          <ows:Identifier>result</ows:Identifier>
        </wps:RawDataOutput>
      </wps:ResponseForm>
    </wps:Execute>

The ``formattedTimestamper`` decoration ensures the frame time is included in the output animation, and looks as follows:

 .. code-block:: xml

    <layout>
      <decoration type="text" affinity="bottom,right" offset="6,6" size="auto">
        <option name="message"><![CDATA[
    <#setting datetime_format="yyyy-MM-dd'T'HH:mm:ss.SSSX">
    <#setting locale="en_US">
    <#if time??>
    ${time?datetime?string["dd-MM-yyyy"]}
    </#if>]]></option>
        <option name="font-family" value="Bitstream Vera Sans"/>
        <option name="font-size" value="12"/>
        <option name="halo-radius" value="2"/>
      </decoration>
    </layout>



Decoration Layout
+++++++++++++++++

| The ``decoration`` parameter specifies the file name (without extension) of the layout to be used to decorate the map.
| The layout is a list of decorators that should draw on top of the requested image.
| The decorators draw on the image one after the other, so the order of the decorators in the layout file is important: the first decorator output will appear under the others.
| Decorators are described in detail in the :ref:`wms_decorations` section.

