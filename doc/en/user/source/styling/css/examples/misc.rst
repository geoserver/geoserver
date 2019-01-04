.. _css_example_misc:

Miscellaneous
=============

Markers sized by an attribute value
-----------------------------------

The following produces square markers at each point, but these are sized such that the area of each marker is proportional to the ``REPORTS`` attribute.
When zoomed in (when there are less points in view) the size of the markers is doubled to make the smaller points more noticeable.

.. code-block:: css

  * {
    mark: symbol(square);
  }
  
  [@sd > 1M] :mark {
    size: [sqrt(REPORTS)];
  }
  
  /* So that single-report points can be more easily seen */
  [@sd < 1M] :mark {
    size: [sqrt(REPORTS)*2];
  }


This example uses the ``sqrt`` function.
There are many functions available for use in CSS and SLD.
For more details read - :doc:`/filter/function_reference`

Specifying a geometry attribute
-------------------------------

In some cases, typically when using a database table with multiple geometry columns, it's necessary to specify which geometry to use.
For example, let's suppose you have a table containing routes ``start`` and ``end`` both containing point geometries.
The following CSS will style the start with a triangle mark, and the end with a square.

.. code-block:: css

   * {
       geometry: [start],          [end];
       mark:     symbol(triangle), symbol(square);
   }

Generating a geometry (Geometry Transformations)
------------------------------------------------

Taking the previous example a bit further, we can also perform computations on-the-fly to generate the geometries that will be drawn.
Any operation that is available for GeoServer :ref:`geometry_transformations` is also available in CSS styles.
To use them, we simply provide a more complex expression in the ``geometry`` property.
For example, we could mark the start and end points of all the paths in a line layer (you can test this example out with any line layer, such as the ``sf:streams`` layer that is included in GeoServer's default data directory.)

.. code-block:: css

   * {
       geometry: [startPoint(the_geom)], [endPoint(the_geom)];
       mark:     symbol(triangle),       symbol(square);
   }

Rendering different geometry types (lines/points) with a single style
---------------------------------------------------------------------

As one more riff on the geometry examples, we'll show how to render both the original line and the start/endpoints in a single style.
This is accomplished by using ``stroke-geometry`` and ``mark-geometry`` to specify that different geometry expressions should be used for symbols compared with strokes.

.. code-block:: css

   * {
       stroke-geometry: [the_geom];
       stroke:          blue;
       mark-geometry: [startPoint(the_geom)], [endPoint(the_geom)];
       mark:          symbol(triangle),       symbol(square);
   }
