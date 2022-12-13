.. _community_wpsrendereddownload:

Rendered map/animation download processes
=========================================

These processes allow download large maps and animations.

The rendered download processes
-------------------------------

The map and animation downloads work off a set of common parameters:

 * ``bbox`` : a geo-referenced bounding box, controlling both output area and desired projection
 * ``decoration`` : the name of a decoration layout to be added on top of the map
 * ``decorationEnvironment`` : a valid value for the ``env`` parameter used when painting the decoration. Used for :ref:`dynamic decoration layouts<wms_dynamic_decorations>`. 
 * ``time`` : a WMS ``time`` specification used to drive the selection of times across the layers in the map, and to control the frame generation in the animation
 * ``width`` and ``height`` : size of the output map/animation (and in combination with bounding box, also controls the output map scale)
 * ``layer``: a list of layer specifications, from a client side point of view (thus, a layer can be composed of multiple server side layers). When dwn:DecorationName layer option is used, it allows to define a specific layout that will be used when decorations are applied to the layer. It allows to render more than one Legend on the resulting image, when having more than one Layer declared.
 * ``headerheight`` : height size of a header space allocated at top of rendered map. It's an optional parameter, that forces to shrink the maps view height in order to avoid overlapping header over the maps. In combination with the use of layer specification options allows to group decorators at the top of resulting image.

The layer specification
-----------------------

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

Decoration Layout
-----------------

The ``decoration`` parameter specifies the file name (without extension) of the layout to be used to decorate the map.
The layout is a list of decorators that should draw on top of the requested image.
The decorators draw on the image one after the other, so the order of the decorators in the layout file is important: the first decorator output will appear under the others.

Decorators are described in detail in the :ref:`wms_decorations` section.
It is also possible to use :ref:`dynamic decoration layouts<wms_dynamic_decorations>`, in this
case the environment parameters for the decoration will be provided using ``dwn:Parameter``, e.g.:

.. code-block:: xml

  <dwn:Layer>
    <dwn:Name>theLayer</dwn:Name>
    <dwn:DecorationName>theDynamicDecoration</dwn:DecorationName>
    <dwn:Parameter key="env">sla:top,right;bg:#FF0000</dwn:Parameter>
  </dwn:Layer>

Map Download Process
--------------------

In addition to the common parameters, the MapDownloadProcess sports an
extra boolean parameter, ``transparent``, which can be either true or false, determining if
the output map has a transparent or a solid background (animation lacks this parameter, as videos
need solid background). The ``transparent`` parameter defaults to ``false`` [#f1]_.

Also, unlike animation, in the map download process the ``time`` parameter is optional.

The map download process uses the WMS machinery to produce the output, but it's not subject to the WMS service
limits (width and height in this process can be limited using the WPS process security).

Sample DownloadMap requests
++++++++++++++++++++++++++++

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

Secondary output: map metadata
++++++++++++++++++++++++++++++

The process offers also a secondary output, called ``metadata``, which can be used to determine
if there were any issue related to the requested times. The warnings are issued when the layer
has a "nearest match" behavior activated, with an eventual search range.

In case the requested time could not be matched exactly, a warning will be issued that might contain:

- An indication that a nearby time has been used, and which time that is.
- An indication that no time was found, that was sufficiently close to the requested one, according
  to the search range specification in the layer "nearest match" configuration.

In order to get both outputs, the following response form is recommended, which requires
a reference (a link) for the map, while the warnings are included inline:

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
      <!-- Inputs section removed for brevity -->
      <wps:ResponseForm>
        <wps:ResponseDocument>
          <wps:Output asReference="true">
            <ows:Identifier>result</ows:Identifier>
          </wps:Output>
          <wps:Output>
            <ows:Identifier>metadata</ows:Identifier>
          </wps:Output>
        </wps:ResponseDocument>
      </wps:ResponseForm>
    </wps:Execute>

A sample response, reporting warnings, follows:

 .. code-block:: xml


    <?xml version="1.0" encoding="UTF-8"?><wps:ExecuteResponse xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema" service="WPS" serviceInstance="http://localhost:8080/geoserver/ows?" version="1.0.0" xml:lang="en">
      <wps:Process wps:processVersion="1.0.0">
        <ows:Identifier>gs:DownloadMap</ows:Identifier>
        <ows:Title>Map Download Process</ows:Title>
        <ows:Abstract>Builds a large map given a set of layer definitions, area of interest, size and eventual target time.</ows:Abstract>
      </wps:Process>
      <wps:Status creationTime="2021-06-07T16:50:47.391Z">
        <wps:ProcessSucceeded>Process succeeded.</wps:ProcessSucceeded>
      </wps:Status>
      <wps:ProcessOutputs>
        <wps:Output>
          <ows:Identifier>result</ows:Identifier>
          <ows:Title>The output map</ows:Title>
          <wps:Reference href="http://localhost:8080/geoserver/ows?service=WPS&amp;version=1.0.0&amp;request=GetExecutionResult&amp;executionId=5db686ed-8591-4756-8651-4bd26281bf37&amp;outputId=result.png&amp;mimetype=image%2Fpng" mimeType="image/png"/>
        </wps:Output>
        <wps:Output>
          <ows:Identifier>metadata</ows:Identifier>
          <ows:Title>map metadata, including dimension match warnings</ows:Title>
          <wps:Data>
            <wps:ComplexData mimeType="text/xml">
              <DownloadMetadata>
                <Warnings>
                  <DimensionWarning>
                    <LayerName>sf:bmtime</LayerName>
                    <DimensionName>time</DimensionName>
                    <Value class="Date">2004-02-01T00:00:00.000Z</Value>
                    <WarningType>Nearest</WarningType>
                  </DimensionWarning>
                </Warnings>
                <WarningsFound>true</WarningsFound>
              </DownloadMetadata>
            </wps:ComplexData>
          </wps:Data>
        </wps:Output>
      </wps:ProcessOutputs>
    </wps:ExecuteResponse>

Animation Download Process
--------------------------

The download animation has all the basic parameters with the following variants/additions:

* time: The time parameter is required and can be provided either as range with periodicity, ``start/stop/period``, or
  as a comma separated list of times,``t1,t2,...,tn`` 
* fps: Frame per seconds (defaults to one)

Sample DownloadAnimation request
++++++++++++++++++++++++++++++++

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


Secondary output: animation metadata
++++++++++++++++++++++++++++++++++++

The process offers also a secondary output, called ``metadata``, which can be used to determine
if there were any issue related to the requested times. The warnings are issued when the layer
has a "nearest match" behavior activated, with an eventual search range.

In case the requested time could not be matched exactly, a warning will be issued that might contain:

- An indication that a nearby time has been used, and which time that is.
- An indication that no time was found, that was sufficiently close to the requested one, according
  to the search range specification in the layer "nearest match" configuration.

In order to get both outputs, the following response form is recommended, which requires
a reference (a link) for the animation, while the warnings are included inline:

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
      <!-- Inputs section removed for brevity -->
      <wps:ResponseForm>
        <wps:ResponseDocument>
          <wps:Output asReference="true">
            <ows:Identifier>result</ows:Identifier>
          </wps:Output>
          <wps:Output>
            <ows:Identifier>metadata</ows:Identifier>
          </wps:Output>
        </wps:ResponseDocument>
      </wps:ResponseForm>
    </wps:Execute>

A sample response, reporting warnings and the frame count where they happened, follows:

 .. code-block:: xml


    <?xml version="1.0" encoding="UTF-8"?><wps:ExecuteResponse xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema" service="WPS" serviceInstance="http://localhost:8080/geoserver/ows?" version="1.0.0" xml:lang="en">
      <wps:Process wps:processVersion="1.0.0">
        <ows:Identifier>gs:DownloadAnimation</ows:Identifier>
        <ows:Title>Animation Download Process</ows:Title>
        <ows:Abstract>Builds an animation given a set of layer definitions, area of interest, size and a series of times for animation frames.</ows:Abstract>
      </wps:Process>
      <wps:Status creationTime="2021-06-07T16:50:47.391Z">
        <wps:ProcessSucceeded>Process succeeded.</wps:ProcessSucceeded>
      </wps:Status>
      <wps:ProcessOutputs>
        <wps:Output>
          <ows:Identifier>result</ows:Identifier>
          <ows:Title>The animation</ows:Title>
          <wps:Reference href="http://localhost:8080/geoserver/ows?service=WPS&amp;version=1.0.0&amp;request=GetExecutionResult&amp;executionId=b98eded5-8122-442b-a6c7-87a872779153&amp;outputId=result.mp4&amp;mimetype=video%2Fmp4" mimeType="video/mp4"/>
        </wps:Output>
        <wps:Output>
          <ows:Identifier>metadata</ows:Identifier>
          <ows:Title>Animation metadata, including dimension match warnings</ows:Title>
          <wps:Data>
            <wps:ComplexData mimeType="text/xml">
              <AnimationMetadata>
                <Warnings>
                  <FrameWarning>
                    <LayerName>sf:bmtime</LayerName>
                    <DimensionName>time</DimensionName>
                    <Value class="Date">2004-02-01T00:00:00.000Z</Value>
                    <WarningType>Nearest</WarningType>
                    <Frame>0</Frame>
                  </FrameWarning>
                  <FrameWarning>
                    <LayerName>sf:bmtime</LayerName>
                    <DimensionName>time</DimensionName>
                    <WarningType>FailedNearest</WarningType>
                    <Frame>1</Frame>
                  </FrameWarning>
                  <FrameWarning>
                    <LayerName>sf:bmtime</LayerName>
                    <DimensionName>time</DimensionName>
                    <Value class="Date">2004-04-01T00:00:00.000Z</Value>
                    <WarningType>Nearest</WarningType>
                    <Frame>2</Frame>
                  </FrameWarning>
                  <FrameWarning>
                    <LayerName>sf:bmtime</LayerName>
                    <DimensionName>time</DimensionName>
                    <Value class="Date">2004-05-01T00:00:00.000Z</Value>
                    <WarningType>Nearest</WarningType>
                    <Frame>3</Frame>
                  </FrameWarning>
                </Warnings>
                <WarningsFound>true</WarningsFound>
              </AnimationMetadata>
            </wps:ComplexData>
          </wps:Data>
        </wps:Output>
      </wps:ProcessOutputs>
    </wps:ExecuteResponse>

In the above output, frames 0, 2 and 3 were nearest matched to an available time, being specified
in the ``Value`` field, while the time requested for frame number 1 was too far away from any
available time, resulting in a ``NearestFail``. The frame is still present in the animation, but
will likely be blank.
In case multiple time based layers are requested in the animation, there might be multiple warnings
for each frame.

.. rubric:: Footnotes

.. [#f1] The default value of ``transparent`` can be flipped using a system variable, e.g. ``-DDOWNLOAD_MAP_TRANSPARENT=true``