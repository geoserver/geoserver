.. _ysld_reference_symbolizers:

Symbolizers
===========

The basic unit of visualization is the symbolizer. There are five types of symbolizers: **Point**, **Line**, **Polygon**, **Raster**, and **Text**.

Symbolizers are contained inside :ref:`rules <ysld_reference_rules>`. A rule can contain one or many symbolizers. 

.. note:: The most common use case for multiple symbolizers is a geometry (point/line/polygon) symbolizer to draw the features plus a text symbolizer for labeling these features.

   .. figure:: img/symbolizers.*
      
      Use of multiple symbolizers

Drawing order
-------------

The order of symbolizers significant, and also the order of your data.

For each feature the rules are evaluated resulting in a list of symbolizers that will be used to draw that feature. The symbolizers are drawn in the order provided.

Consider the following two symbolizers::

   symbolizers:
   - point:
       symbols:
       - mark:
           shape: square
           fill-color: '#FFCC00'
   - point:
       symbols:
       - mark:
           shape: triangle
           fill-color: '#FF3300'
     
When drawing three points these symbolizers will be applied in order on each feature:

#. Feature 1 is drawn as a square, followed by a triangle:
   
   .. figure:: img/symbolizer-order1.*
      
      Feature 1 buffer rendering
      
#. Feature 2 is drawn as a square, followed by a triangle. Notice the slight overlap with Feature 1:

   .. figure:: img/symbolizer-order2.*
      
      Feature 2 buffer rendering

#. Feature 3 is drawn as a square, followed by a triangle:

   .. figure:: img/symbolizer-order3.*
      
      Feature 3 buffer rendering

.. note:: In the final image, Feature 1 and Feature 2 have a slight overlap. This overlap is determined by data order which we have no control over. If you need to control the overlap review the :ref:`ysld_reference_featurestyles` section on managing "z-order".
   
   .. figure:: img/symbolizer-order4.*
      
      Feature style controlling z-order
   
Matching symbolizers and geometries
-----------------------------------

It is common to match the symbolizer with the type of geometries contained in the layer, but this is not required. The following table illustrates what will happen when a geometry symbolizer is matched up with another type of geometry.

.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1

   * - 
     - Points
     - Lines
     - Polygon
     - Raster
   * - Point Symbolizer
     - **Points**
     - Midpoint of the lines
     - Centroid of the polygons
     - Centroid of the raster
   * - Line Symbolizer
     - n/a
     - **Lines**
     - Outline (stroke) of the polygons
     - Outline (stroke) of the raster
   * - Polygon Symbolizer
     - n/a
     - Will "close" the line and style as a polygon 
     - **Polygons**
     - Will "outline" the raster and style as a polygon
   * - Raster Symbolizer
     - n/a
     - n/a
     - n/a
     - Transform raster values to color channels for display
   * - Text Symbolizer
     - Label at point location
     - Label at midpoint of lines
     - Label at centroid of polygons
     - Label at centroid of raster outline

Syntax
------

The following is the basic syntax common to all symbolizers. Note that the contents of the block are not all expanded here and that each kind of symbolizer provides additional syntax.

::

   geometry: <cql>
   uom: <text>
   ..
   x-composite: <text>
   x-composite-base: <boolean>

where:

.. include:: include/symbol.txt

The following properties are equivalent to SLD "vendor options".

.. include:: include/composite.txt

See the following pages for details:

.. toctree::
   :maxdepth: 1

   line
   polygon
   point
   raster
   text