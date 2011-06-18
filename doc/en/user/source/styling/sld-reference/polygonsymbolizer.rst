.. _sld_reference_polygonsymbolizer:

PolygonSymbolizer
=================

The LineSymbolizer styles **polygons**.  Lines are two-dimensional geometry elements.  They can contain styling information about their border (stroke) and their fill.

Syntax
------

A ``<PolygonSymbolizer>`` can have two outermost tags:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Fill>``
     - No (when using ``<Stroke>``)
     - Determines the styling for the fill of the polygon.
   * - ``<Stroke>``
     - No (when using ``<Fill>``)
     - Determines the styling for the stroke of the polygon.

The details for the ``<Stroke>`` tag are identical to that mentioned in the :ref:`sld_reference_linesymbolizer` section above.

Within the ``<Fill>`` tag, there are additional tags:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<GraphicFill>``
     - No
     - Renders the fill of the polygon with a repeated pattern.
   * - ``<CssParameter>``
     - No
     - Determines the fill styling parameters.

When using the ``<GraphicFill>`` tag, it is required to insert the ``<Graphic>`` tag inside it.  The syntax for this tag is identical to that mentioned in the :ref:`sld_reference_pointsymbolizer` section above.
 
Within the ``<CssParameter>`` tag, there are also additional parameters that go inside the actual tag:

.. list-table::
   :widths: 30 15 55
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``name="fill"``
     - No
     - Specifies the fill color for the polygon, in the form #RRGGBB.  Default is grey (``#808080``).
   * - ``name="fill-opacity"``
     - No
     - Specifies the opacity (transparency) of the fill of the polygon.  Possible values are between ``0`` (completely transparent) and ``1`` (completely opaque).  Default is ``1``.


Example
-------

Consider the following symbolizer taken from the Simple Point example in the :ref:`sld_cookbook_polygons` section in the :ref:`sld_cookbook`.

.. code-block:: xml 
   :linenos: 

          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#000080</CssParameter>
            </Fill>
          </PolygonSymbolizer>
          
This symbolizer contains only a ``<Fill>`` tag.  Inside this tag is a ``<CssParameter>`` that specifies a fill color for the polygont o be ``#000080``, or a muted blue.
 
 Further examples can be found in the :ref:`sld_cookbook_polygons` section of the :ref:`sld_cookbook`.