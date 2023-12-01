.. _sld_reference_polygonsymbolizer:

PolygonSymbolizer
=================

A **PolygonSymbolizer** styles features as **polygons**.  
Polygons are two-dimensional geometries.  
They can be depicted with styling for their interior (fill) and their border (stroke).
Polygons may contain one or more holes, which are stroked but not filled.
When rendering a polygon, the fill is rendered before the border is stroked.     

Syntax
------

A ``<PolygonSymbolizer>`` contains an optional ``<Geometry>`` element, and two elements
``<Fill>`` and ``<Stroke>`` for specifying styling:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Geometry>``
     - No
     - Specifies the geometry to be rendered.
   * - ``<Fill>``
     - No
     - Specifies the styling for the polygon interior.
   * - ``<Stroke>``
     - No
     - Specifies the styling for the polygon border.


Geometry
^^^^^^^^

The ``<Geometry>`` element is optional.  
If present, it specifies the featuretype property from which to obtain the geometry to style
using the ``PropertyName`` element.
See also :ref:`geometry_transformations` for GeoServer extensions for specifying geometry.

Any kind of geometry may be styled with a ``<PolygonSymbolizer>``.  
Point geometries are treated as small orthonormal square polygons.
Linear geometries are closed by joining their ends.


Stroke
^^^^^^

The ``<Stroke>`` element specifies the styling for the **border** of a polygon.
The syntax is described in the ``<LineSymbolizer>`` :ref:`sld_reference_stroke` section.

.. _sld_reference_fill:

Fill
^^^^

The ``<Fill>`` element specifies the styling for the **interior** of a polygon.
It can contain the sub-elements:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<GraphicFill>``
     - No
     - Renders the fill of the polygon with a repeated pattern.
   * - ``<CssParameter>``
     - 0..N
     - Specifies parameters for filling with a solid color.

GraphicFill
^^^^^^^^^^^

The ``<GraphicFill>`` element contains a ``<Graphic>`` element,
which specifies a graphic image or symbol to use for a repeated fill pattern.  
The syntax is described in the ``PointSymbolizer`` :ref:`sld_reference_graphic` section.

CssParameter
^^^^^^^^^^^^

The ``<CssParameter>`` elements describe the styling of a solid polygon fill.
Any number of ``<CssParameter>`` elements can be specified. 

The ``name`` **attribute** indicates what aspect of styling an element specifies,
using the standard CSS/SVG styling model.
The **content** of the element supplies the
value of the styling parameter.
The value may contain :ref:`expressions <sld_reference_parameter_expressions>`.

The following parameters are supported:

.. list-table::
   :widths: 30 15 55
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``name="fill"``
     - No
     - Specifies the fill color, in the form ``#RRGGBB``.  Default is grey (``#808080``).
   * - ``name="fill-opacity"``
     - No
     - Specifies the opacity (transparency) of the fill.  The value is a decimal number between ``0`` (completely transparent) and ``1`` (completely opaque).  Default is ``1``.



Example
-------

The following symbolizer is taken from the :ref:`sld_cookbook_polygons` section in the :ref:`sld_cookbook`.

.. code-block:: xml 
   :linenos: 

          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#000080</CssParameter>
            </Fill>
          </PolygonSymbolizer>
          
This symbolizer contains only a ``<Fill>`` element.  
Inside this element is a ``<CssParameter>`` that specifies the fill color for the polygon to be ``#000080`` (a muted blue).
 
Further examples can be found in the :ref:`sld_cookbook_polygons` section of the :ref:`sld_cookbook`.