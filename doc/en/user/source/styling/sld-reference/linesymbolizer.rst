.. _sld_reference_linesymbolizer:
   
LineSymbolizer
==============

The LineSymbolizer styles **lines**.  Lines are one-dimensional geometry elements that contain position and length.  Lines can be comprised of multiple line segments.

Syntax
------

The outermost tag is the ``<Stroke>`` tag.  This tag is required, and determines the visualization of the line.  There are three possible tags that can be included inside the ``<Stroke>`` tag.

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<GraphicFill>``
     - No
     - Renders the pixels of the line with a repeated pattern.
   * - ``<GraphicStroke>``
     - No
     - Renders the line with a repeated linear graphic.
   * - ``<CssParameter>``
     - No
     - Determines the stroke styling parameters.
     
When using the ``<GraphicStroke>`` and ``<GraphicFill>`` tags, it is required to insert the ``<Graphic>`` tag inside them.  The syntax for this tag is identical to that mentioned in the :ref:`sld_reference_pointsymbolizer` section above.

Within the ``<CssParameter>`` tag, there are also additional parameters that go inside the actual tag:

.. list-table::
   :widths: 30 15 55
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``name="stroke"``
     - No
     - Specifies the solid color given to the line, in the form #RRGGBB.  Default is black (``#000000``).
   * - ``name="stroke-width"``
     - No
     - Specifies the width of the line in pixels.  Default is ``1``.
   * - ``name="stroke-opacity"``
     - No
     - Specifies the opacity (transparency) of the line.  possible values are between ``0`` (completely transparent) and ``1`` (completely opaque).  Default is ``1``.
   * - ``name="stroke-linejoin"``
     - No
     - Determines how lines are rendered at intersections of line segments.  Possible values are ``mitre`` (sharp corner), ``round`` (rounded corner), and ``bevel`` (diagonal corner).  Default is ``mitre``.
   * - ``name="stroke-linecap"``
     - No
     - Determines how lines are rendered at ends of line segments.  Possible values are ``butt`` (sharp square edge), ``round`` (rounded edge), and ``square`` (slightly elongated square edge).  Default is ``butt``.
   * - ``name="stroke-dasharray"``
     - No
     - Encodes a dash pattern as a series of numbers separated by spaces.  Odd-indexed numbers (first, third, etc) determine the length in pxiels to draw the line, and even-indexed numbers (second, fourth, etc) determine the length in pixels to blank out the line.  Default is an unbroken line. `Starting from version 2.1` dash arrays can be combined with graphic strokes to generate complex line styles with alternating symbols or a mix of lines and symbols.
   * - ``name="stroke-dashoffset"``
     - No
     - Specifies the distance in pixels into the ``dasharray`` pattern at which to start drawing.  Default is ``0``.
