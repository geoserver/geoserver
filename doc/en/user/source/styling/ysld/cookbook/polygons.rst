.. _ysld_cookbook.polygons:

Polygons
========

Polygons are two dimensional shapes that contain both an outer edge (or "stroke") and an inside (or "fill"). A polygon can be thought of as an irregularly-shaped point and is styled in similar ways to points.

.. _ysld_cookbook_polygons_attributes:

Example polygons layer
----------------------

The :download:`polygons layer <artifacts/ysld_cookbook_polygon.zip>` used below contains county information for a fictional country. For reference, the attribute table for the polygons is included below.

.. list-table::
   :widths: 30 40 30
   :header-rows: 1

   * - ``fid`` (Feature ID)
     - ``name`` (County name)
     - ``pop`` (Population)
   * - polygon.1
     - Irony County
     - 412234
   * - polygon.2
     - Tracker County
     - 235421
   * - polygon.3
     - Dracula County
     - 135022
   * - polygon.4
     - Poly County
     - 1567879
   * - polygon.5
     - Bearing County
     - 201989
   * - polygon.6
     - Monte Cristo County
     - 152734
   * - polygon.7
     - Massive County
     - 67123
   * - polygon.8
     - Rhombus County
     - 198029

:download:`Download the polygons shapefile <artifacts/ysld_cookbook_polygon.zip>`


.. _ysld_cookbook_polygons_simplepolygon:

Simple polygon
--------------

This example shows a polygon filled in blue.

.. figure:: ../../sld/cookbook/images/polygon_simplepolygon.png

   Simple polygon

Code
~~~~

:download:`Download the "Simple polygon" YSLD <artifacts/polygon_simplepolygon.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Simple polygon'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          fill-color: '#000080'

Details
~~~~~~~

There is one rule in one feature style for this style, which is the simplest possible situation. (All subsequent examples will share this characteristic unless otherwise specified.)  Styling polygons is accomplished via the polygon symbolizer (**lines 6-7**). **Line 7** specifies dark blue (``'#000080'``) as the polygon's fill color.

.. note::  The light-colored borders around the polygons in the figure are artifacts of the renderer caused by the polygons being adjacent. There is no border in this style.

.. _ysld_cookbook_polygons_simplepolygonwithstroke:

Simple polygon with stroke
--------------------------

This example adds a 2 pixel white stroke to the :ref:`ysld_cookbook_polygons_simplepolygon` example.

.. figure:: ../../sld/cookbook/images/polygon_simplepolygonwithstroke.png

   Simple polygon with stroke

Code
~~~~

:download:`Download the "Simple polygon with stroke" YSLD <artifacts/polygon_simplepolygonwithstroke.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Simple polygon with stroke'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          stroke-color: '#FFFFFF'
          stroke-width: 2
          fill-color: '#000080'

Details
~~~~~~~

This example is similar to the :ref:`ysld_cookbook_polygons_simplepolygon` example above, with the addition of ``stroke`` parameters (**lines 7-8**). **Line 7** sets the color of stroke to white (``'#FFFFFF'``) and **line 8** sets the width of the stroke to 2 pixels.


Transparent polygon
-------------------

This example builds on the :ref:`ysld_cookbook_polygons_simplepolygonwithstroke` example and makes the fill partially transparent by setting the opacity to 50%.

.. figure:: ../../sld/cookbook/images/polygon_transparentpolygon.png

   Transparent polygon

Code
~~~~

:download:`Download the "Transparent polygon" YSLD <artifacts/polygon_transparentpolygon.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Transparent polygon'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          stroke-color: '#FFFFFF'
          stroke-width: 2
          fill-color: '#000080'
          fill-opacity: 0.5

Details
~~~~~~~

This example is similar to the :ref:`ysld_cookbook_polygons_simplepolygonwithstroke` example, save for defining the fill's opacity in **line 10**. The value of 0.5 results in partially transparent fill that is 50% opaque. An opacity value of 1 would draw the fill as 100% opaque, while an opacity value of 0 would result in a completely transparent (0% opaque) fill. In this example, since the background is white, the dark blue looks lighter. Were the points imposed on a dark background, the resulting color would be darker.


.. _ysld_cookbook_polygons_graphicfill:

Graphic fill
------------

This example fills the polygons with a tiled graphic.

.. figure:: ../../sld/cookbook/images/polygon_graphicfill.png

   Graphic fill

Code
~~~~

:download:`Download the "Graphic fill" YSLD <artifacts/polygon_graphicfill.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Graphic fill'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          fill-color: '#808080'
          fill-graphic:
            size: 93
            symbols:
            - external:
                url: colorblocks.png
                format: image/png

Details
~~~~~~~

This style fills the polygon with a tiled graphic. This is known as an ``external`` in YSLD, to distinguish it from commonly-used shapes such as squares and circles that are "internal" to the renderer. **Lines 11-13** specify details for the graphic, with **line 12** setting the path and file name of the graphic and **line 13** indicating the file format (MIME type) of the graphic (``image/png``). Although a full URL could be specified if desired, no path information is necessary in **line 12** because this graphic is contained in the same directory as the YSLD. **Line 9** determines the height of the displayed graphic in pixels; if the value differs from the height of the graphic then it will be scaled accordingly while preserving the aspect ratio.

.. figure:: ../../sld/cookbook/images/colorblocks.png

   Graphic used for fill


Hatching fill
-------------

This example fills the polygons with a hatching pattern.

.. figure:: ../../sld/cookbook/images/polygon_hatchingfill.png

   Hatching fill

Code
~~~~

:download:`Download the "Hatching fill" YSLD <artifacts/polygon_hatchingfill.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Hatching fill'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          fill-color: '#808080'
          fill-graphic:
            size: 16
            symbols:
            - mark:
                shape: shape://times
                stroke-color: '#990099'
                stroke-width: 1

Details
~~~~~~~

In this example, there is a ``fill-graphic`` parameter as in the :ref:`ysld_cookbook_polygons_graphicfill` example, but a ``mark`` (**lines 11-14**) is used instead of an ``external``. **Line 12** specifies a "times" symbol (an "x") be tiled throughout the polygon. **Line 13** sets the color to purple (``'#990099'``), **line 14** sets the width of the hatches to 1 pixel, and **line 9** sets the size of the tile to 16 pixels. Because hatch tiles are always square, the ``size`` sets both the width and the height.


.. _ysld_cookbook_polygons_polygonwithdefaultlabel:

Polygon with default label
--------------------------

This example shows a text label on the polygon. In the absence of any other customization, this is how a label will be displayed.

.. figure:: ../../sld/cookbook/images/polygon_polygonwithdefaultlabel.png

   Polygon with default label

Code
~~~~

:download:`Download the "Polygon with default label" YSLD <artifacts/polygon_polygonwithdefaultlabel.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Polygon with default label'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          stroke-color: '#FFFFFF'
          stroke-width: 2
          fill-color: '#40FF40'
      - text:
          label: ${name}
          placement: point

Details
~~~~~~~

In this example there is a polygon symbolizer and a text symbolizer. **Lines 6-9** comprise the polygon symbolizer. The fill of the polygon is set on **line 7** to a light green (``'#40FF40'``) while the stroke of the polygon is set on **lines 8-9** to white (``'#FFFFFF'``) with a thickness of 2 pixels. The label is set in the text symbolizer on **lines 10-12**, with **line 11** determining what text to display, in this case the value of the "name" attribute. (Refer to the attribute table in the :ref:`ysld_cookbook_polygons_attributes` section if necessary.)  All other details about the label are set to the renderer default, which here is Times New Roman font, font color black, and font size of 10 pixels.


Label halo
----------

This example alters the look of the :ref:`ysld_cookbook_polygons_polygonwithdefaultlabel` by adding a white halo to the label.

.. figure:: ../../sld/cookbook/images/polygon_labelhalo.png

   Label halo

Code
~~~~

:download:`Download the "Label halo" YSLD <artifacts/polygon_labelhalo.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Label halo'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          stroke-color: '#FFFFFF'
          stroke-width: 2
          fill-color: '#40FF40'
      - text:
          label: ${name}
          halo:
            fill-color: '#FFFFFF'
            radius: 3
          placement:
            type: point

Details
~~~~~~~

This example is similar to the :ref:`ysld_cookbook_polygons_polygonwithdefaultlabel`, with the addition of a halo around the labels on **lines 12-14**. A halo creates a color buffer around the label to improve label legibility. **Line 14** sets the radius of the halo, extending the halo 3 pixels around the edge of the label, and **line 13** sets the color of the halo to white (``'#FFFFFF'``). Since halos are most useful when set to a sharp contrast relative to the text color, this example uses a white halo around black text to ensure optimum readability.


.. _ysld_cookbook_polygons_polygonwithstyledlabel:

Polygon with styled label
-------------------------

This example improves the label style from the :ref:`ysld_cookbook_polygons_polygonwithdefaultlabel` example by centering the label on the polygon, specifying a different font name and size, and setting additional label placement optimizations.

.. figure:: ../../sld/cookbook/images/polygon_polygonwithstyledlabel.png

   Polygon with styled label

Code
~~~~

:download:`Download the "Polygon with styled label" YSLD <artifacts/polygon_polygonwithstyledlabel.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Polygon with styled label'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - polygon:
          stroke-color: '#FFFFFF'
          stroke-width: 2
          fill-color: '#40FF40'
      - text:
          label: ${name}
          fill-color: '#000000'
          font-family: Arial
          font-size: 11
          font-style: normal
          font-weight: bold
          placement: point
          anchor: [0.5,0.5]
          x-autoWrap: 60
          x-maxDisplacement: 150

Details
~~~~~~~

This example is similar to the :ref:`ysld_cookbook_polygons_polygonwithdefaultlabel` example, with additional styling options within the text symbolizer on lines **13-21**. **Lines 13-16** set the font styling. **Line 13** sets the font family to be Arial, **line 14** sets the font size to 11 pixels, **line 15** sets the font style to "normal" (as opposed to "italic" or "oblique"), and **line 16** sets the font weight to "bold" (as opposed to "normal").

The ``anchor`` parameter on **line 18** centers the label by positioning it 50% (or 0.5) of the way horizontally and vertically along the centroid of the polygon.

Finally, there are two added touches for label placement optimization: **line 20** ensures that long labels are split across multiple lines by setting line wrapping on the labels to 60 pixels, and **line 21** allows the label to be displaced by up to 150 pixels. This ensures that labels are compacted and less likely to spill over polygon boundaries. Notice little Massive County in the corner, whose label is now displayed." 


Attribute-based polygon
-----------------------


This example styles the polygons differently based on the "pop" (Population) attribute.

.. figure:: ../../sld/cookbook/images/polygon_attributebasedpolygon.png

   Attribute-based polygon

Code
~~~~

:download:`Download the "Attribute-based polygon" YSLD <artifacts/polygon_attributebasedpolygon.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Attribute-based polygon'
  feature-styles:
  - name: name
    rules:
    - name: SmallPop
      title: Less Than 200,000
      filter: ${pop < '200000'}
      symbolizers:
      - polygon:
          fill-color: '#66FF66'
    - name: MediumPop
      title: 200,000 to 500,000
      filter: ${pop >= '200000' AND pop < '500000'}
      symbolizers:
      - polygon:
          fill-color: '#33CC33'
    - name: LargePop
      title: ${Greater Than 500,000}
      filter: pop > '500000'
      symbolizers:
      - polygon:
          fill-color: '#009900'

Details
~~~~~~~

.. note:: Refer to the :ref:`ysld_cookbook_polygons_attributes` to see the attributes for the layer. This example has eschewed labels in order to simplify the style, but you can refer to the example :ref:`ysld_cookbook_polygons_polygonwithstyledlabel` to see which attributes correspond to which polygons.

Each polygon in our fictional country has a population that is represented by the population ("pop") attribute. This style contains three rules that alter the fill based on the value of "pop" attribute, with smaller values yielding a lighter color and larger values yielding a darker color.

The three rules are designed as follows:

.. list-table::
   :widths: 20 20 30 30
   :header-rows: 1

   * - Rule order
     - Rule name
     - Population (``pop``)
     - Color
   * - 1
     - SmallPop
     - Less than 200,000
     - ``#66FF66``
   * - 2
     - MediumPop
     - 200,000 to 500,000
     - ``#33CC33``
   * - 3
     - LargePop
     - Greater than 500,000
     - ``#009900``

The order of the rules does not matter in this case, since each shape is only rendered by a single rule.

The first rule, on **lines 5-10**, specifies the styling of polygons whose population attribute is less than 200,000. **Line 7** sets this filter, denoting the attribute ("pop"), to be "less than" the value of 200,000. The color of the polygon fill is set to a light green (``'#66FF66'``) on **line 10**.

The second rule, on **lines 11-16**, is similar, specifying a style for polygons whose population attribute is greater than or equal to 200,000 but less than 500,000. The filter is set on **line 13**. This filter specifies two criteria instead of one: a "greater than or equal to" and a "less than" filter. These criteria are joined by ``AND``, which mandates that both filters need to be true for the rule to be applicable. The color of the polygon fill is set to a medium green on (``'#33CC33'``) on **line 16**.

The third rule, on **lines 17-22**, specifies a style for polygons whose population attribute is greater than or equal to 500,000. The filter is set on **line 19**. The color of the polygon fill is the only other difference in this rule, which is set to a dark green (``'#009900'``) on **line 22**.



Zoom-based polygon
------------------

This example alters the style of the polygon at different zoom levels.


.. figure:: ../../sld/cookbook/images/polygon_zoombasedpolygonlarge.png

   Zoom-based polygon: Zoomed in

.. figure:: ../../sld/cookbook/images/polygon_zoombasedpolygonmedium.png

   Zoom-based polygon: Partially zoomed

.. figure:: ../../sld/cookbook/images/polygon_zoombasedpolygonsmall.png

   Zoom-based polygon: Zoomed out

Code
~~~~

:download:`Download the "Zoom-based polygon" YSLD <artifacts/polygon_zoombasedpolygon.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Zoom-based polygon'
  feature-styles:
  - name: name
    rules:
    - name: Large
      scale: [min,1.0e8]
      symbolizers:
      - polygon:
          stroke-color: '#000000'
          stroke-width: 7
          fill-color: '#0000CC'
      - text:
          label: ${name}
          fill-color: '#FFFFFF'
          font-family: Arial
          font-size: 14
          font-style: normal
          font-weight: bold
          placement: point
          anchor: [0.5,0.5]
    - name: Medium
      scale: [1.0e8,2.0e8]
      symbolizers:
      - polygon:
          stroke-color: '#000000'
          stroke-width: 4
          fill-color: '#0000CC'
    - name: Small
      scale: [2.0e8,max]
      symbolizers:
      - polygon:
          stroke-color: '#000000'
          stroke-width: 1
          fill-color: '#0000CC'

Details
~~~~~~~

It is often desirable to make shapes larger at higher zoom levels when creating a natural-looking map. This example varies the thickness of the lines according to the zoom level. Polygons already do this by nature of being two dimensional, but another way to adjust styling of polygons based on zoom level is to adjust the thickness of the stroke (to be larger as the map is zoomed in) or to limit labels to only certain zoom levels. This is ensures that the size and quantity of strokes and labels remains legible and doesn't overshadow the polygons themselves.

Zoom levels (or more accurately, scale denominators) refer to the scale of the map. A scale denominator of 10,000 means the map has a scale of 1:10,000 in the units of the map projection.

.. note:: Determining the appropriate scale denominators (zoom levels) to use is beyond the scope of this example.

This style contains three rules, defined as follows:

.. list-table::
   :widths: 15 15 40 15 15
   :header-rows: 1

   * - Rule order
     - Rule name
     - Scale denominator
     - Stroke width
     - Label display?
   * - 1
     - Large
     - 1:100,000,000 or less
     - 7
     - Yes
   * - 2
     - Medium
     - 1:100,000,000 to 1:200,000,000
     - 4
     - No
   * - 3
     - Small
     - Greater than 1:200,000,000
     - 2
     - No

The first rule, on **lines 5-20**, is for the smallest scale denominator, corresponding to when the view is "zoomed in". The scale rule is set on **line 6** such that the rule will apply only where the scale denominator is 100,000,000 or less. **Line 11** defines the fill as blue (``'#0000CC'``). Note that the fill is kept constant across all rules regardless of the scale denominator. As in the :ref:`ysld_cookbook_polygons_polygonwithdefaultlabel` or :ref:`ysld_cookbook_polygons_polygonwithstyledlabel` examples, the rule also contains a text symbolizer at **lines 12-20** for drawing a text label on top of the polygon. **Lines 15-18** set the font information to be Arial, 14 pixels, and bold with no italics. The label is centered both horizontally and vertically along the centroid of the polygon on by setting ``anchor`` to be ``[0.5, 0.5]`` (or 50%) on **line 20**. Finally, the color of the font is set to white (``'#FFFFFF'``) in **line 14**.

The second rule, on **lines 21-27**, is for the intermediate scale denominators, corresponding to when the view is "partially zoomed". The scale rules on **lines 22** set the rule such that it will apply to any map with a scale denominator between 100,000,000 and 200,000,000. (The lower bound is inclusive and the upper bound is exclusive, so a zoom level of exactly 200,000,000 would *not* apply here.)  Aside from the scale, there are two differences between this rule and the first: the width of the stroke is set to 4 pixels on **line 26** and a text symbolizer is not present so that no labels will be displayed.

The third rule, on **lines 28-34**, is for the largest scale denominator, corresponding to when the map is "zoomed out". The scale rule is set on **line 29** such that the rule will apply to any map with a scale denominator of 200,000,000 or greater. Again, the only differences between this rule and the others are the width of the lines, which is set to 1 pixel on **line 33**, and the absence of a text symbolizer so that no labels will be displayed.

The resulting style produces a polygon stroke that gets larger as one zooms in and labels that only display when zoomed in to a sufficient level.
