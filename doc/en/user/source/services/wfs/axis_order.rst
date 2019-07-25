.. _wfs_basics_axis:

Axis ordering
=============

WFS 1.0.0 servers return geographic coordinates in longitude/latitude (x/y) order, the most common way to distribute data. For example, many shapefiles adopt this order by default. 

However, the traditional axis order for geographic and cartographic systems is the opposite—latitude/longitude (y/x)—and the later WFS specifications respect this. The default axis ordering support is:

* Longitude/latitude—WFS 1.0.0
* Latitude/longitude—WFS 1.1.0 and WFS 2.0.0

This may cause difficulties when switching between servers with different WFS versions, or when upgrading your WFS. To minimize confusion and increase interoperability, GeoServer has adopted the following assumptions when specifying projections in the following formats: 

.. list-table::
   :widths: 75 25
   :header-rows: 1

   * - Representation
     - Assumed axis order
   * - ``EPSG:xxxx``
     - longitude/latitude (x/y)
   * - ``http://www.opengis.net/gml/srs/epsg.xml#xxxx``
     - longitude/latitude (x/y)
   * - ``urn:x-ogc:def:crs:EPSG:xxxx``
     - latitude/longitude (y/x) 
   * - ``urn:ogc:def:crs:EPSG::4326``
     - latitude/longitude (y/x) 


To compare the spatial reference system definition for ``EPSG:4326`` navigate :menuselection:`Demos --> SRS List` page and search for :kbd`4326`:

.. figure:: img/wgs84-epsg-description.png
   
   WGS84 EPSG definition

.. figure:: img/wgs84-internal-description.png
   
   WGS84 Internal definition

The default data directory includes the following dataset illustrating this challenge:

* :file:`shapefile/states.shp``: Data stored in x/y order::
  
    MULTIPOLYGON (((-121.664154 38.169369,-121.781296 38.066856, ...
  
* :file:`shapefiles/states.prj` ::
   
     GEOGCS["GCS_WGS_1984",DATUM["WGS_1984",SPHEROID["WGS_1984",6378137,298.257223563]],PRIMEM["Greenwich",0],UNIT["Degree",0.017453292519943295]]
  
  Published as ``topp:states`` with Spatial Reference System ``EPSG:4326``.
   
WFS 1.0 Axis Order
------------------

**GetCapabilities** describes ``topp:states`` using:

http://localhost:8080/geoserver/ows?service=wfs&version=1.0.0&request=GetCapabilities

.. code-block:: xml

    <FeatureType><Name>topp:states</Name>
      <Title>USA Population</Title>
      <Abstract>This is some census data on the states.</Abstract>
      <Keywords>census, united, boundaries, state, states</Keywords>
      <SRS>EPSG:4326</SRS>
      <LatLongBoundingBox minx="-124.731422" miny="24.955967" maxx="-66.969849" maxy="49.371735" />
    </FeatureType> 
   
WFS 1.0 describes the latitude / longitude bounds with the understanding that you will associate `minx` and `maxx` with longitude, and also `miny` and `maxy` with latitude.

WFS 1.0 *GetFeature* request defaults to GML2 output, and the default ``EPSG:4326`` spatial reference system used to publish the layer:

* WFS 1.0 Default: http://localhost:8080/geoserver/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1

  The GML2 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:

  .. code-block:: xml

     <gml:MultiPolygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:polygonMember>
         <gml:Polygon>
           <gml:outerBoundaryIs><gml:LinearRing>
             <gml:coordinates decimal="." cs="," ts=" ">
               -88.071564,37.51099 -88.087883,37.476273

WFS 1.0 output format GML3
``````````````````````````

* GML3.1 (default ``EPSG:4326``):
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml3

  GML3 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 -88.071564 37.51099 -88.087883 37.476273

* GML3.1 reproject to ``EPSG:4326``
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml3&srsName=urn:x-ogc:def:crs:EPSG:4326
  
  GML3 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 -88.071564 37.51099 -88.087883
  
* GML 3.1 reproject to ``urn:x-ogc:def:crs:EPSG:4326``
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml3&srsName=urn:x-ogc:def:crs:EPSG:4326
  
  GML3.1 output using ``urn:x-ogc:def:crs:EPSG:4326`` reference and data in y/x order:
  
  .. code-block:: xml
     
     <gml:MultiSurface srsName="urn:x-ogc:def:crs:EPSG:4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 37.51099 -88.071564 37.476273 -88.087883 

WFS 1.0 output format GML32
```````````````````````````````

* GML3.2: 
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml32

  The GML32 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>-88.071564 37.51099 -88.087883 37.476273 

  .. warning:: This combination is inconsistent with ``srsName`` definition and may confuse client applications.


* GML3.2 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml32&srsName=EPSG:4326

  The GML32 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:

  .. code-block:: xml
    
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 -88.071564 37.51099 -88.087883 37.476273
                 
  .. warning:: This combination is inconsistent with ``srsName`` definition and may confuse client applications.

* GML3.2 reproject to ``urn:x-ogc:def:crs:EPSG:4326``:
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml32&srsName=urn:x-ogc:def:crs:EPSG:4326
  
  GML3.2 output using ``urn:x-ogc:def:crs:EPSG:4326`` reference and data in y/x order:

  .. code-block:: xml
    
     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior>
             <gml:LinearRing><gml:posList>
               37.51099 -88.071564 37.476273 -88.087883 

WFS 1.1 Axis Order
------------------

**GetCapabilities** describes ``topp:states`` using:

http://localhost:8080/geoserver/ows?service=wfs&version=1.1.0&request=GetCapabilities

.. code-block:: xml

   <FeatureType>
     <Name>topp:states</Name>
     <Title>USA Population</Title>
     <Abstract>This is some census data on the states.</Abstract>
     <ows:Keywords>
       <ows:Keyword>census</ows:Keyword><ows:Keyword>united</ows:Keyword><ows:Keyword>boundaries</ows:Keyword><ows:Keyword>state</ows:Keyword><ows:Keyword>states</ows:Keyword>
     </ows:Keywords>
     <DefaultSRS>urn:x-ogc:def:crs:EPSG:4326</DefaultSRS>
     <ows:WGS84BoundingBox>
       <ows:LowerCorner>-124.731422 24.955967</ows:LowerCorner>
       <ows:UpperCorner>-66.969849 49.371735</ows:UpperCorner>
     </ows:WGS84BoundingBox></FeatureType>    
  
WFS 1.1 describes the ``WGS84BoundingBox`` as a lower and upper corner in x/y order.

.. warning:: This combination is inconsistent with ``DefaultSRS`` definition and may confuse client applications.

WFS 1.1 *GetFeature* request defaults to GML3 output, and the default ``EPSG:4326`` spatial reference system used to publish the layer:

* WFS 1.1 Default:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1

  The GML3.1 output uses ``urn:x-ogc:def:crs:EPSG:4326`` reference, with data in y/x order:

  .. code-block:: xml

     <gml:MultiSurface srsName="urn:x-ogc:def:crs:EPSG:4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                  37.51099 -88.071564 37.476273 -88.087883  

* WFS 1.1 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&srsName=EPSG:4326
  
  The GML3.1 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 -88.071564 37.51099 -88.087883 37.476273
   
  .. warning:: This output combination of ``srsName`` and x/y order is technically inconsistent and may confuse applications expecting a valid GML3 document.
     
     This approach can be used to force x/y order.


* WFS 1.1 reproject to ``urn:x-ogc:def:crs:EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&srsName=urn:x-ogc:def:crs:EPSG:4326
  
  The GML3.1 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in y/x order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 37.51099 -88.071564 37.476273 -88.087883

WFS 1.1 output format GML2
``````````````````````````

* GML2:
  
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml2

  GML2 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in y/x order:

  .. code-block:: xml
  
     <gml:MultiPolygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:polygonMember>
         <gml:Polygon><gml:outerBoundaryIs>
           <gml:LinearRing>
             <gml:coordinates decimal="." cs="," ts=" ">
               37.51099,-88.071564 37.476273,-88.087883

  
* GML2 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml2&srsName=EPSG:4326

  GML2 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:

  .. code-block:: xml
  
     <gml:MultiPolygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:polygonMember>
         <gml:Polygon>
           <gml:outerBoundaryIs>
             <gml:LinearRing>
               <gml:coordinates decimal="." cs="," ts=" ">
                 -88.071564,37.51099 -88.087883,37.476273

  .. warning:: The output combination of ``srsName`` and coordinates x/y order is technically inconsistent and may confuse applications expecting a valid GML2 document.
    
     This approach can be used to force x/y order.

WFS 1.1 output format GML3
````````````````````````````

* GML3:


  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml3

  GML3.1 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in y/x order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 37.51099 -88.071564 37.476273 -88.087883

* GML3 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml3&srsName=EPSG:4326
  
  GML3.1 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, *but has changed the data to x/y order*:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 -88.071564 37.51099 -88.087883 37.476273
   
  .. warning:: This combination technically inconsistent and may confuse applications expecting a valid GML3 document.
     
     This approach can be used to force x/y order.
   
* GML3 reproject to ``urn:x-ogc:def:crs:EPSG:4326``
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml3&srsName=urn:x-ogc:def:crs:EPSG:4326
  
  GML3.1 output using ``urn:x-ogc:def:crs:EPSG:4326`` reference and data in y/x order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 -88.071564 37.51099 -88.087883 37.476273
   
  .. warning:: This combination is inconsistent and may confuse applications expecting a valid GML3 document.
     
     This approach can be used to force x/y order.

WFS 1.1 output format GML32
````````````````````````````

* GML3.2:
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml32

  The GML32 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in y/x order:
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember><gml:Polygon gml:id="states.1.the_geom.1">
         <gml:exterior>
           <gml:LinearRing>
             <gml:posList>37.51099 -88.071564 37.476273 -88.087883


* GML3.2 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml32&srsName=EPSG:4326

  The GML32 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:

  .. code-block:: xml
    
     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>-88.071564 37.51099 -88.087883 37.476273
               
* GML3.2 reproject to ``urn:x-ogc:def:crs:EPSG:4326``:
  
  http://localhost:8080/geoserver/topp/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=topp%3Astates&featureId=states.1&outputFormat=gml32&srsName=urn:x-ogc:def:crs:EPSG:4326
  
  GML3.2 output using ``urn:x-ogc:def:crs:EPSG:4326`` reference and data in y/x order:

  .. code-block:: xml
    
     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior>
             <gml:LinearRing><gml:posList>37.51099 -88.071564 37.476273 -88.087883 



WFS 2.0 Axis Order
------------------

**GetCapabilities** describes ``topp:states`` using:

http://localhost:8080/geoserver/ows?service=wfs&version=2.0.0&request=GetCapabilities

.. code-block:: xml

   <FeatureType>
     <Name>topp:states</Name>
     <Title>USA Population</Title>
     <Abstract>This is some census data on the states.</Abstract>
     <ows:Keywords>
       <ows:Keyword>census</ows:Keyword><ows:Keyword>united</ows:Keyword><ows:Keyword>boundaries</ows:Keyword><ows:Keyword>state</ows:Keyword><ows:Keyword>states</ows:Keyword>
     </ows:Keywords>
     <DefaultCRS>urn:ogc:def:crs:EPSG::4326</DefaultCRS>
     <ows:WGS84BoundingBox>
       <ows:LowerCorner>-124.731422 24.955967</ows:LowerCorner>
       <ows:UpperCorner>-66.969849 49.371735</ows:UpperCorner>
     </ows:WGS84BoundingBox>
   </FeatureType>
   
WFS 2.0 describes the ``WGS84BoundingBox`` as a lower and upper corner in x/y order.

.. warning:: This combination is inconsistent with ``DefaultSRS`` definition and may confuse client applications.
   
   The result matches the WFS 2.0 GetCapabilities examples.

WFS 2.0 *GetFeature* request defaults to GML3.2 output, and the default ``EPSG:4326`` spatial reference system used to publish the layer:

* WFS 2.0 Default:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1

  The GML3.2 output uses ``urn:ogc:def:crs:EPSG::4326`` reference, with data in y/x order:

  .. code-block:: xml

     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior><gml:LinearRing>
             <gml:posList>
               37.51099 -88.071564 37.476273 -88.087883  

* WFS 2.0 reproject to ``EPSG:4326``:

  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&srsName=EPSG:4326

  The GML3.2 output uses ``http://www.opengis.net/gml/srs/epsg.xml#4326`` reference, with data in x/y order:

  .. code-block:: xml

     <gml:MultiSurface srsName="http://www.opengis.net/gml/srs/epsg.xml#4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior><gml:LinearRing>
             <gml:posList>
               -88.071564 37.51099 -88.087883 37.476273 
                  
  .. warning:: This combination is of ``srsName`` definition and ``posList`` coordinate order is inconsistent and may confuse client applications.

* WFS 2.0 reproject to ``urn:ogc:def:crs:EPSG::4326``
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&srsName=urn:ogc:def:crs:EPSG::4326

  The GML3.2 output uses ``urn:ogc:def:crs:EPSG::4326`` reference, with data in y/x order:

  .. code-block:: xml

     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1">
           <gml:exterior><gml:LinearRing>
             <gml:posList>
               37.51099 -88.071564 37.476273 -88.087883 37.442852
                  
WFS 2.0 output format GML2
``````````````````````````

* GML2:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml2

  .. code-block:: xml
  
     <gml:MultiPolygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:polygonMember>
         <gml:Polygon>
           <gml:outerBoundaryIs>
             <gml:LinearRing>
               <gml:coordinates decimal="." cs="," ts=" ">
                 37.51099,-88.071564 37.476273,-88.087883 

* GML2 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml2&srsName=EPSG:4326

  .. code-block:: xml
  
     <gml:MultiPolygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:polygonMember>
         <gml:Polygon>
           <gml:outerBoundaryIs>
             <gml:LinearRing>
               <gml:coordinates decimal="." cs="," ts=" ">
                 -88.071564,37.51099 -88.087883,37.476273

  .. warning:: This combination technically inconsistent and may confuse applications expecting a valid GML2 document.
     
     This approach can be used to force x/y order.

* GML2 reproject to ``urn:x-ogc:def:crs:EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml2&srsName=urn:x-ogc:def:crs:EPSG:4326

  .. code-block:: xml
  
     <gml:MultiPolygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
       <gml:polygonMember>
         <gml:Polygon>
           <gml:outerBoundaryIs>
             <gml:LinearRing>
               <gml:coordinates decimal="." cs="," ts=" ">
                 37.51099,-88.071564 37.476273,-88.087883

WFS 2.0 output format GML3
``````````````````````````

* GML3:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml3
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="urn:x-ogc:def:crs:EPSG:4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 37.51099 -88.071564 37.476273 -88.087883 
                 
* GML3 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml3&srsName=EPSG:4326
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="urn:x-ogc:def:crs:EPSG:4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 -88.071564 37.51099 -88.087883 37.476273

  .. warning:: This combination technically inconsistent and may confuse applications expecting a valid GML3 document.
  
     This approach can be used to force x/y order.
      
* GML3 reproject to ``urn:x-ogc:def:crs:EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml3&srsName=urn:x-ogc:def:crs:EPSG:4326
  
  .. code-block:: xml
  
     <gml:MultiSurface srsName="urn:x-ogc:def:crs:EPSG:4326">
       <gml:surfaceMember>
         <gml:Polygon>
           <gml:exterior>
             <gml:LinearRing>
               <gml:posList>
                 37.51099 -88.071564 37.476273 -88.087883
   
WFS 2.0 output format GML32
```````````````````````````

* GML32:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml32

  .. code-block:: xml
  
     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1"><gml:exterior>
           <gml:LinearRing>
             <gml:posList>
               37.51099 -88.071564 37.476273 -88.087883 

* GML32 reproject to ``EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml32&srsName=EPSG:4326

  .. code-block:: xml
  
     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1"><gml:exterior>
           <gml:LinearRing>
             <gml:posList>
               -88.071564 37.51099 -88.087883 37.476273
               
  .. warning:: This combination technically inconsistent and may confuse applications expecting a valid GML3 document.
   
     This approach can be used to force x/y order.
      
* GML32 reproject to ``urn:x-ogc:def:crs:EPSG:4326``:
  
  http://localhost:8080/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Astates&featureId=states.1&outputFormat=gml32&srsName=urn:x-ogc:def:crs:EPSG:4326

  .. code-block:: xml
  
     <gml:MultiSurface srsName="urn:ogc:def:crs:EPSG::4326" gml:id="states.1.the_geom">
       <gml:surfaceMember>
         <gml:Polygon gml:id="states.1.the_geom.1"><gml:exterior>
           <gml:LinearRing>
             <gml:posList>
               37.51099 -88.071564 37.476273 -88.087883