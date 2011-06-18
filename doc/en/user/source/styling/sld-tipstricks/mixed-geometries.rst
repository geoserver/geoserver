.. _mixed_geometries:

Dealing with mixed geometry types
==================================

On occasion one might have the need to render data with a single geometry column whose content type can be different for each feature (some have a polygon, some have a point, etc).

SLD 1.0 does not provide a clean solution for dealing with such a case. This is due to a mix of two issues. The first one is that point, line, and polygon  symbolizers can apply to other geometry types:

*  Point symbolizers can apply to any kind of geometry; if the geometry is not a point, the centroid of the feature will be used in its place.
*  Line symbolizers can apply to both lines and polygons.
*  Polygon symbolizers can apply to lines as well, by adding a segment connecting the last point of the line to the first.

The second issue is that there is no standard way to apply a filter identifying the type of the chosen geometry attribute.

There are a number of workarounds, either requiring data restructuring or the use of non-standard filter functions.

Restructuring the data
----------------------

There are a few ways to restructure the data so that it can be rendered without difficulties using only standard SLD constructs.

Split the table
```````````````

The first and obvious one is to split the table into a set of separate ones, each one containing a single geometry type. For example, if table ``findings`` has a geometry column that can contain point, lines, and polygons, three tables will be generated, each one containing a single geometry type.

Separate geometry columns
`````````````````````````

A second way is to use one table and separate geometry columns. So, if the table ``findings`` has a ``geom`` column, the restructured table will have ``point``, ``line`` and ``polygon`` columns, each of them containing just one geometry type. After the restructuring, the symbolizers will refer to a specific geometry, for example:
  
.. code-block:: xml
   
   <PolygonSymbolizer>
       <Geometry><ogc:PropertyName>polygon</ogc:PropertyName></Geometry>
   </PolygonSymbolizer>

This way each symbolizer will match only the geometry types it is supposed to render, and skip over the rows that contain a null value.

Add a geometry type column
``````````````````````````

A third way is to add a geometry type column allowing standard filtering constructs to be used, and then build a separate rule per geometry type. In the example above a new attribute, ``gtype`` will be added containing the values ``Point``, ``Line`` and ``Polygon``. The following SLD template can be used after the change:
  
.. code-block:: xml

   <Rule>
      <ogc:Filter>
         <ogc:PropertyIsEqualTo>
            <ogc:PropertyName>gtype</ogc:PropertyName>
            <ogc:Literal>Point</ogc:PropertyName>
         </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <PointSymbolizer>
         ...
      </PointSymbolizer>
   </Rule>
   <Rule>
      <ogc:Filter>
         <ogc:PropertyIsEqualTo>
            <ogc:PropertyName>gtype</ogc:PropertyName>
            <ogc:Literal>Line</ogc:PropertyName>
         </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <LineSymbolizer>
         ...
      </LineSymbolizer>
   </Rule>
   <Rule>
      <ogc:Filter>
         <ogc:PropertyIsEqualTo>
            <ogc:PropertyName>gtype</ogc:PropertyName>
            <ogc:Literal>Polygon</ogc:PropertyName>
         </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <PolygonSymbolizer>
         ...
      </PolygonSymbolizer>
   </Rule>
   
All of the above suggestions do work under the assumption that restructuring the data is technically possible, which is usually true in spatial databases that provide functions that allow to recognize the geometry type.

Create views
````````````

A less invasive way to get the same results without changing the structure of the table is to create views that have the required structure. This allows the original data to be kept intact, and the views to be used only for rendering sake.


Using non-standard SLD functions
--------------------------------

SLD 1.0 uses the OGC Filter 1.0 specification for filtering out the data to be renderered by each rule.
A function is a black box taking a number of parameters as inputs, and returning a result. It can implement many functionalities, such as computing a trigonometric function, formatting dates, or determining the type of a geometry.

However, none of the standards define a set of well known functions.  This means that any SLD document that uses functions is valid, although it is not portable to another GIS system. If this is not a problem, filtering by geometry type can be done using the ``geometryType`` filter function, which takes a geometry property and returns a string, which can (currently) be one of ``Point``, ``LineString``, ``LinearRing``, ``Polygon``, ``MultiPoint``, ``MultiLineString``, ``MultiPolygon`` and ``GeometryCollection``.

Using the function, a ``Rule`` matching only single points can be written as:

.. code-block:: xml

   <Rule>
      <ogc:PropertyIsEqualTo>
         <ogc:Function name="geometryType">
            <ogc:PropertyName>geom</ogc:PropertyName>
         </ogc:Function>
         <ogc:Literal>Point</ogc:Literal>
      </ogc:PropertyIsEqualTo>
      <PointSymbolizer>
        ...
      </PointSymbolizer>
   </Rule>
   
The filter becomes more complex if one has to match any kind of linear geometry.  In this case, it would look like:

.. code-block:: xml

   <Rule>
      <ogc:Filter>
        <ogc:PropertyIsEqualTo>
          <ogc:Function name="in3">
             <ogc:Function name="geometryType">
                 <ogc:PropertyName>geom</ogc:PropertyName>
             </ogc:Function>
             <ogc:Literal>LineString</ogc:Literal>
             <ogc:Literal>LinearRing</ogc:Literal>
             <ogc:Literal>MultiLineString</ogc:Literal>
          </ogc:Function>
          <ogc:Literal>true</ogc:Literal>
        </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <LineSymbolizer>
        ...
      </LineSymbolizer>
   </Rule>

This filter would read like ``geometryType(geom) in (LineString, LinearRing, MultiLineString)``.  Filter functions in Filter 1.0 have a known number of arguments, so there are various in functions with different names, like ``in2``, ``in3``, ..., ``in10``.
