.. _mixed_geometries:

Styling mixed geometry types
============================

On occasion one might need to style a geometry column whose geometry type can be different for each feature 
(some are polygons, some are points, etc), and use different styling for different geometry types.

SLD 1.0 does not provide a clean solution for dealing with this situation. 
Point, Line, and Polygon symbolizers do not select geometry by type, since each can apply to all geometry types:

*  Point symbolizers apply to any kind of geometry. If the geometry is not a point, the centroid of the geometry is used.
*  Line symbolizers apply to both lines and polygons.  For polygons the boundary is styled.
*  Polygon symbolizers apply to lines, by adding a closing segment connecting the first and last points of the line.

There is also no standard filter predicate to identify geometry type which could be used in rules.

This section suggests a number of ways to accomplish styling by geometry type.  
They require either data restructuring or the use of non-standard filter functions.

Restructuring the data
----------------------

There are a few ways to restructure the data so that it can be styled by geometry type using only standard SLD constructs.

Split the table
^^^^^^^^^^^^^^^

The first and obvious one is to split the original table into a set of separate tables, each one containing a single geometry type. For example, if table ``findings`` has a geometry column that can contain point, lines, and polygons, three tables can be created, each one containing a single geometry type.

Separate geometry columns
^^^^^^^^^^^^^^^^^^^^^^^^^

A second way is to use one table and separate geometry columns. So, if the table ``findings`` has a ``geom`` column, the restructured table will have ``point``, ``line`` and ``polygon`` columns, each of them containing just one geometry type. After the restructuring, the symbolizers will refer to a specific geometry, for example:
  
.. code-block:: xml
   
   <PolygonSymbolizer>
       <Geometry><ogc:PropertyName>polygon</ogc:PropertyName></Geometry>
   </PolygonSymbolizer>

This way each symbolizer will match only the geometry types it is supposed to render, and skip over the rows that contain a null value.

Add a geometry type column
^^^^^^^^^^^^^^^^^^^^^^^^^^

A third way is to add a geometry type column allowing standard filtering constructs to be used, and then build a separate rule per geometry type. In the example above a new attribute, ``gtype`` will be added containing the values ``Point``, ``Line`` and ``Polygon``. The following SLD template can be used after the change:
  
.. code-block:: xml

   <Rule>
      <ogc:Filter>
         <ogc:PropertyIsEqualTo>
            <ogc:PropertyName>gtype</ogc:PropertyName>
            <ogc:Literal>Point</ogc:Literal>
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
            <ogc:Literal>Line</ogc:Literal>
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
            <ogc:Literal>Polygon</ogc:Literal>
         </ogc:PropertyIsEqualTo>
      </ogc:Filter>
      <PolygonSymbolizer>
         ...
      </PolygonSymbolizer>
   </Rule>
   
The above suggestions assume that restructuring the data is technically possible.
This is usually true in spatial databases that provide functions that allow determining the geometry type.

Create views
^^^^^^^^^^^^

A less invasive way to get the same results without changing the structure of the table is to create views that have the required structure. This allows the original data to be kept intact, and the views may be used for rendering.


Using SLD rules and filter functions
------------------------------------

SLD 1.0 uses the OGC Filter 1.0 specification for filtering out the data to be styled by each rule.
Filters can contain :ref:`filter_function` to compute properties of geometric values.
In GeoServer, filtering by geometry type can be done using the ``geometryType`` or ``dimension`` filter functions.

.. note:: The Filter Encoding specification provides a standard syntax for filter functions, but does not mandate a specific set of functions.
          SLDs using these functions may not be portable to other styling software.


geometryType function
^^^^^^^^^^^^^^^^^^^^^

The ``geometryType`` function takes a geometry property and returns a string, which (currently) is one of the values ``Point``, ``LineString``, ``LinearRing``, ``Polygon``, ``MultiPoint``, ``MultiLineString``, ``MultiPolygon`` and ``GeometryCollection``.

Using this function, a ``Rule`` matching only single points can be written as:

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
   
The filter is more complex if it has to match all linear geometry types.  
In this case, it looks like:

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

This filter is read as ``geometryType(geom) in ("LineString", "LinearRing", "MultiLineString")``.  
Filter functions in Filter 1.0 have a fixed number of arguments, 
so there is a series of ``in`` functions whose names correspond to the number of arguments they accept: ``in2``, ``in3``, ..., ``in10``.

dimension function
^^^^^^^^^^^^^^^^^^

A slightly simpler alternative is to use the geometry ``dimension`` function
to select geometries of a desired dimension.
Dimension 0 selects Points and MultiPoints, 
dimension 1 selects LineStrings, LinearRings and MultiLineStrings,
and dimension 2 selects Polygons and MultiPolygons.
The following example shows how to select linear geometries:

.. code-block:: xml

   <Rule>
      <ogc:PropertyIsEqualTo>
         <ogc:Function name="dimension">
            <ogc:PropertyName>geom</ogc:PropertyName>
         </ogc:Function>
         <ogc:Literal>1</ogc:Literal>
      </ogc:PropertyIsEqualTo>
      <LineSymbolizer>
        ...
      </LineSymbolizer>
   </Rule>

