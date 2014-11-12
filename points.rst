.. _cartography.ysld.cookbook.points:

.. highlight:: python
   :linenothreshold: 1

Points
======

While points are seemingly the simplest type of shape, possessing only position and no other dimensions, there are many different ways that a point can be styled in YSLD.

.. _ysld_cookbook_points_attributes:

Example points layer
--------------------

The :download:`points layer <artifacts/ysld_cookbook_point.zip>` used for the examples below contains name and population information for the major cities of a fictional country. For reference, the attribute table for the points in this layer is included below.

.. list-table::
   :widths: 30 40 30

   * - **fid** (Feature ID)
     - **name** (City name)
     - **pop** (Population)
   * - point.1
     - Borfin
     - 157860
   * - point.2
     - Supox City
     - 578231
   * - point.3
     - Ruckis
     - 98159
   * - point.4
     - Thisland
     - 34879
   * - point.5
     - Synopolis
     - 24567
   * - point.6
     - San Glissando
     - 76024
   * - point.7
     - Detrania
     - 205609

:download:`Download the points shapefile <artifacts/ysld_cookbook_point.zip>`

.. _ysld_cookbook_points_simplepoint:

Simple point
------------

This example specifies points be styled as red circles with a diameter of 6 pixels.

.. figure:: images/point_simplepoint.png
   :align: center

   *Simple point*
   
Code
~~~~

:download:`Download the "Simple point" YSLD <artifacts/point_simplepoint.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Simple Point With Stroke'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 6
          symbols:
          - mark:
              shape: circle
              fill-color: FF0000

Details
~~~~~~~

There is one rule in one feature style for this YSLD, which is the simplest possible situation.  (All subsequent examples will contain one rule and one feature style unless otherwise specified.)  Styling points is accomplished via the point symbolizer (**lines 7-12**).  **Line 11** specifies the shape of the symbol to be a circle, with **line 12** determining the fill color to be red (``#FF0000``).  **Line 8** sets the size (diameter) of the graphic to be 6 pixels.


.. _ysld_cookbook_points_simplepointwithstroke:

Simple point with stroke
------------------------

This example adds a stroke (or border) around the :ref:`ysld_cookbook_points_simplepoint`, with the stroke colored black and given a thickness of 2 pixels.

.. figure:: images/point_simplepointwithstroke.png
   :align: center

   *Simple point with stroke*

Code
~~~~

:download:`Download the "Simple point with stroke" YSLD <artifacts/point_simplepointwithstroke.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Simple point with stroke'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 6
          symbols:
          - mark:
              shape: circle
              stroke-color: '000000'
              stroke-width: 2
              fill-color: FF0000

Details
~~~~~~~

This example is similar to the :ref:`ysld_cookbook_points_simplepoint` example.  **Lines 12-13** specify the stroke, with **line 12** setting the color to black (``#000000``) and **line 13** setting the width to 2 pixels.


Rotated square
--------------

This example creates a square instead of a circle, colors it green, sizes it to 12 pixels, and rotates it by 45 degrees.

.. figure:: images/point_rotatedsquare.png
   :align: center

   *Rotated square*

Code
~~~~

:download:`Download the "Rotated square" YSLD <artifacts/point_rotatedsquare.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Rotated square'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 12
          rotation: 45
          symbols:
          - mark:
              shape: square
              fill-color: '009900'

Details
~~~~~~~

In this example, **line 12** sets the shape to be a square, with **line 13** setting the color to a dark green (``#009900``).  **Line 8** sets the size of the square to be 12 pixels, and **line 9** sets the rotation to 45 degrees.


Transparent triangle
--------------------

This example draws a triangle, creates a black stroke identical to the :ref:`ysld_cookbook_points_simplepointwithstroke` example, and sets the fill of the triangle to 20% opacity (mostly transparent).

.. figure:: images/point_transparenttriangle.png
   :align: center

   *Transparent triangle*

Code
~~~~   

:download:`Download the "Transparent triangle" YSLD <artifacts/point_transparenttriangle.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Transparent triangle'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 12
          symbols:
          - mark:
              shape: triangle
              stroke-color: '000000'
              stroke-width: 2
              fill-color: '009900'
              fill-opacity: 0.2

Details
~~~~~~~

In this example, **line 11** once again sets the shape, in this case to a triangle.  **Line 14** sets the fill color to a dark green (``#009900``) and **line 15** sets the opacity to 0.2 (20% opaque).  An opacity value of 1 means that the shape is drawn 100% opaque, while an opacity value of 0 means that the shape is drawn 0% opaque, or completely transparent.  The value of 0.2 (20% opaque) means that the fill of the points partially takes on the color and style of whatever is drawn beneath it.  In this example, since the background is white, the dark green looks lighter.  Were the points imposed on a dark background, the resulting color would be darker.  **Lines 12-13** set the stroke color to black (``#000000``) and width to 2 pixels.  Finally, **line 8** sets the size of the point to be 12 pixels in diameter.

Point as graphic
----------------

This example styles each point as a graphic instead of as a simple shape.

.. figure:: images/point_pointasgraphic.png
   :align: center

   *Point as graphic*

Code
~~~~

:download:`Download the "Point as graphic" YSLD <artifacts/point_pointasgraphic.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Point as graphic'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 32
          symbols:
          - external:
              url: smileyface.png
              format: image/png

Details
~~~~~~~

This style uses a graphic instead of a simple shape to render the points.  In YSLD, this is known as an ``external``, to distinguish it from the commonly-used shapes such as squares and circles that are "internal" to the renderer.  **Lines 10-12** specify the details of this graphic.  **Line 11** sets the path and file name of the graphic, while **line 12** indicates the format (MIME type) of the graphic (image/png). In this example, the graphic is contained in the same directory as the YSLD, so no path information is necessary in **line 8**,  although a full URL could be used if desired.  **Line 8** determines the size of the displayed graphic; this can be set independently of the dimensions of the graphic itself, although in this case they are the same (32 pixels).  Should a graphic be rectangular, the ``size`` value will apply to the *height* of the graphic only, with the width scaled proportionally.

.. figure:: images/smileyface.png
   :align: center

   *Graphic used for points*

.. _ysld_cookbook_points_pointwithdefaultlabel:

Point with default label
------------------------

This example shows a text label on the :ref:`ysld_cookbook_points_simplepoint` that displays the "name" attribute of the point. This is how a label will be displayed in the absence of any other customization.

.. figure:: images/point_pointwithdefaultlabel.png
   :align: center

   *Point with default label*

Code
~~~~

:download:`Download the "Point with default label" YSLD <artifacts/point_pointwithdefaultlabel.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Point with default label'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 6
          symbols:
          - mark:
              shape: circle
              fill-color: FF0000
      - text:
          label: name
          fill-color: '000000'
          font-family: Serif
          font-size: 10
          font-style: normal
          font-weight: normal
          placement: point

Details
~~~~~~~

**Lines 3-12**, which contain the point symbolizer, are identical to the :ref:`ysld_cookbook_points_simplepoint` example above.  The label is set in the text symbolizer on **lines 13-20**.  **Line 14** determines what text to display in the label, which in this case is the value of the "name" attribute.  (Refer to the attribute table in the :ref:`ysld_cookbook_points_attributes` section if necessary.)  **Line 15** sets the text color.  All other details about the label are set to the renderer default, which here is Times New Roman font, font color black, and font size of 10 pixels.  The bottom left of the label is aligned with the center of the point.


.. _ysld_cookbook_points_pointwithstyledlabel:

Point with styled label
-----------------------

This example improves the label style from the :ref:`ysld_cookbook_points_pointwithdefaultlabel` example by centering the label above the point and providing a different font name and size.

.. figure:: images/point_pointwithstyledlabel.png
   :align: center

   *Point with styled label*

Code
~~~~   

:download:`Download the "Point with styled label" YSLD <artifacts/point_pointwithstyledlabel.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Point with styled label'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 6
          symbols:
          - mark:
              shape: circle
              fill-color: FF0000
      - text:
          label: name
          fill-color: '000000'
          font-family: Arial
          font-size: 12
          font-style: normal
          font-weight: bold
          placement: point
          anchor: (0.5,0.0)
          displacement: (0,5)

Details
~~~~~~~

In this example, **lines 3-12** are identical to the :ref:`ysld_cookbook_points_simplepoint` example above.  The ``<TextSymbolizer>`` on **lines 13-22** contains many more details about the label styling than the previous example, :ref:`ysld_cookbook_points_pointwithdefaultlabel`.  **Line 14** once again specifies the "name" attribute as text to display.  **Lines 16-19** set the font information:  **line 16** sets the font family to be "Arial", **line 17** sets the font size to 12, **line 18** sets the font style to "normal" (as opposed to "italic" or "oblique"), and **line 19** sets the font weight to "bold" (as opposed to "normal").  **Lines 20-22** determine the placement of the label relative to the point.  The ``anchor`` (**line 21**) sets the point of intersection between the label and point, which here sets the point to be centered (0.5) horizontally axis and bottom aligned (0.0) vertically with the label.  There is also ``displacement`` (**line 22**), which sets the offset of the label relative to the line, which in this case is 0 pixels horizontally and 5 pixels vertically .  Finally, **line 15** sets the font color of the label to black (``#000000``).

The result is a centered bold label placed slightly above each point.



Point with rotated label
------------------------

This example builds on the previous example, :ref:`ysld_cookbook_points_pointwithstyledlabel`, by rotating the label by 45 degrees, positioning the labels farther away from the points, and changing the color of the label to purple.

.. figure:: images/point_pointwithrotatedlabel.png
   :align: center

   *Point with rotated label*

Code
~~~~

:download:`Download the "Point with rotated label" YSLD <artifacts/point_pointwithrotatedlabel.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Point with rotated label'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - point:
          size: 6
          symbols:
          - mark:
              shape: circle
              fill-color: FF0000
      - text:
          label: name
          fill-color: '990099'
          font-family: Arial
          font-size: 12
          font-style: normal
          font-weight: bold
          placement: point
          anchor: (0.5,0.0)
          displacement: (0,25)
          rotation: -45

Details
~~~~~~~

This example is similar to the :ref:`ysld_cookbook_points_pointwithstyledlabel`, but there are three important differences.  **Line 22** specifies 25 pixels of vertical displacement.  **Line 23** specifies a rotation of "-45" or 45 degrees counter-clockwise.  (Rotation values increase clockwise, which is why the value is negative.)  Finally, **line 15** sets the font color to be a shade of purple (``#99099``).

Note that the displacement takes effect before the rotation during rendering, so in this example, the 25 pixel vertical displacement is itself rotated 45 degrees.


Attribute-based point
---------------------

This example alters the size of the symbol based on the value of the population ("pop") attribute.  

.. figure:: images/point_attributebasedpoint.png
   :align: center

   *Attribute-based point*
   
Code
~~~~

:download:`Download the "Attribute-based point" YSLD <artifacts/point_attribute.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Attribute-based point'
  feature-styles:
  - name: name
    rules:
    - name: SmallPop
      title: 1 to 50000
      filter: pop < '50000'
      symbolizers:
      - point:
          size: 8
          symbols:
          - mark:
              shape: circle
              fill-color: 0033CC
    - name: MediumPop
      title: 50000 to 100000
      filter: pop >= '50000' AND pop < '100000'
      symbolizers:
      - point:
          size: 12
          symbols:
          - mark:
              shape: circle
              fill-color: 0033CC
    - name: LargePop
      title: Greater than 100000
      filter: pop >= '100000'
      symbolizers:
      - point:
          size: 16
          symbols:
          - mark:
              shape: circle
              fill-color: 0033CC

Details
~~~~~~~
   
.. note:: Refer to the :ref:`ysld_cookbook_points_attributes` to see the attributes for this data.  This example has eschewed labels in order to simplify the style, but you can refer to the example :ref:`ysld_cookbook_points_pointwithstyledlabel` to see which attributes correspond to which points.

This style contains three rules.  Each rule varies the style based on the value of the population ("pop") attribute for each point, with smaller values yielding a smaller circle, and larger values yielding a larger circle.

The three rules are designed as follows:

.. list-table::
   :widths: 20 30 30 20

   * - **Rule order**
     - **Rule name**
     - **Population** ("pop")
     - **Size**
   * - 1
     - SmallPop
     - Less than 50,000
     - 8
   * - 2
     - MediumPop
     - 50,000 to 100,000
     - 12
   * - 3
     - LargePop
     - Greater than 100,000
     - 16

The order of the rules does not matter in this case, since each shape is only rendered by a single rule.

The first rule, on **lines 6-15**, specifies the styling of those points whose population attribute is less than 50,000.  **Line 8** sets this filter, denoting the attribute ("pop") to be "less than" the value of 50,000.  The symbol is a circle (**line 14**), the color is dark blue (``#0033CC``, on **line 16**), and the size is 8 pixels in diameter (**line 19**).  

The second rule, on **lines 16-25**, specifies a style for points whose population attribute is greater than or equal to 50,000 and less than 100,000.  The population filter is set on **line 18**.  This filter specifies two criteria instead of one: a "greater than or equal to" and a "less than" filter.  These criteria are joined by ``AND``, which mandates that both filters need to be true for the rule to be applicable.  The size of the graphic is set to 12 pixels on **line 21**.  All other styling directives are identical to the first rule.

The third rule, on **lines 26-35**, specifies a style for points whose population attribute is greater than or equal to 100,000.  The population filter is set on **line 28**, and the only other difference is the size of the circle, which in this rule (**line 31**) is 16 pixels.

The result of this style is that cities with larger populations have larger points.


Zoom-based point
----------------

This example alters the style of the points at different zoom levels.

.. figure:: images/point_zoombasedpointlarge.png
   :align: center

   *Zoom-based point: Zoomed in*

.. figure:: images/point_zoombasedpointmedium.png
   :align: center
   
   *Zoom-based point: Partially zoomed*

.. figure:: images/point_zoombasedpointsmall.png
   :align: center
   
   *Zoom-based point: Zoomed out*

   
Code
~~~~

:download:`Download the "Zoom-based point" YSLD <artifacts/point_zoom.ysld>`

::

  name: Default Styler
  title: 'YSLD Cook Book: Zoom-based point'
  feature-styles:
  - name: name
    rules:
    - name: Large
      scale: (,1.6E8)
      symbolizers:
      - point:
          size: 12
          symbols:
          - mark:
              shape: circle
              fill-color: CC3300
    - name: Medium
      scale: (1.6E8,3.2E8)
      symbolizers:
      - point:
          size: 8
          symbols:
          - mark:
              shape: circle
              fill-color: CC3300
    - name: Small
      scale: (3.2E8,)
      symbolizers:
      - point:
          size: 4
          symbols:
          - mark:
              shape: circle
              fill-color: CC3300

Details
~~~~~~~

It is often desirable to make shapes larger at higher zoom levels when creating a natural-looking map.  This example styles the points to vary in size based on the zoom level (or more accurately, scale denominator).  Scale denominators refer to the scale of the map.  A scale denominator of 10,000 means the map has a scale of 1:10,000 in the units of the map projection.

.. note:: Determining the appropriate scale denominators (zoom levels) to use is beyond the scope of this example.

This style contains three rules.  The three rules are designed as follows:

.. list-table::
   :widths: 25 25 25 25 

   * - **Rule order**
     - **Rule name**
     - **Scale denominator**
     - **Point size**
   * - 1
     - Large
     - 1:160,000,000 or less
     - 12
   * - 2
     - Medium
     - 1:160,000,000 to 1:320,000,000
     - 8
   * - 3
     - Small
     - Greater than 1:320,000,000
     - 4

The order of these rules does not matter since the scales denominated in each rule do not overlap.

The first rule (**lines 6-14**) is for the smallest scale denominator, corresponding to when the view is "zoomed in".  The scale rule is set on **line 7**, so that the rule will apply to any map with a scale denominator of 160,000,000 or less.  The rule draws a circle (**line 13**), colored red (``#CC3300`` on **line 14**) with a size of 12 pixels (**line 10**).

The second rule (**lines 15-23**) is the intermediate scale denominator, corresponding to when the view is "partially zoomed".  The scale rules is set on **line 16**, so that the rule will apply to any map with a scale denominator between 160,000,000 and 320,000,000.  (The lower bound is inclusive and the upper bound is exclusive, so a zoom level of exactly 320,000,000 would *not* apply here.)  Aside from the scale, the only difference between this rule and the first is the size of the symbol, which is set to 8 pixels on **line 19**.

The third rule (**lines 24-32**) is the largest scale denominator, corresponding to when the map is "zoomed out".  The scale rule is set on **line 25**, so that the rule will apply to any map with a scale denominator of 320,000,000 or more.  Again, the only other difference between this rule and the others is the size of the symbol, which is set to 4 pixels on **line 28**.

The result of this style is that points are drawn larger as one zooms in and smaller as one zooms out.

