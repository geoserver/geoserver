.. _sld_reference_pointsymbolizer:

PointSymbolizer
===============

The PointSymbolizer styles **points**,  Points are elements that contain only position information.

Syntax
------

The outermost element is the ``<Graphic>`` tag.  This determines the type of visualization.  There are five possible tags to include inside the ``<Graphic>`` tag:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<ExternalGraphic>``
     - No (when using ``<Mark>``)
     - Specifies an image file to use as the symbolizer.  
   * - ``<Mark>``
     - No (when using ``<ExternalGraphic>``)
     - Specifies a common shape to use as the symbolizer.
   * - ``<Opacity>``
     - No
     - Determines the opacity (transparency) of symbolizers.  Values range from ``0`` (completely transparent) to ``1`` (completely opaque).  Default is ``1``.
   * - ``<Size>``
     - Yes 
     - Determines the size of the symbolizer in pixels.  When used with an image file, this will specify the height of the image, with the width scaled accordingly.
   * - ``<Rotation>``
     - No
     - Determines the rotation of the graphic in degrees.  The rotation increases in the clockwise direction.  Negative values indicate counter-clockwise rotation.  Default is ``0``.

Within the ``<ExternalGraphic>`` tag, there are also additional tags:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<OnlineResource>``
     - Yes
     - The location of the image file.  Can be either a URL or a local path relative to the SLD.
   * - ``<Format>``
     - Yes
     - The MIME type of the image format.  Most standard web image formats are supported.  

Within the ``<Mark>`` tag, there are also additional tags:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<WellKnownName>``
     - No
     - The name of the common shape.  Options are ``circle``, ``square``, ``triangle``, ``star``, ``cross``, or ``x``.  Default is ``square``.
   * - ``<Fill>``
     - No (when using ``<Stroke>``)
     - Specifies how the symbolizer should be filled.  Options are a ``<CssParameter name="fill">`` specifying a color in the form ``#RRGGBB``, or ``<GraphicFill>`` for a repeated graphic.
   * - ``<Stroke>``
     - No (when using ``<Fill>``)
     - Specifies how the symbolizer should be drawn on its border.  Options are a ``<CssParameter name="fill">`` specifying a color in the form ``#RRGGBB`` or ``<GraphicStroke>`` for a repeated graphic.

Example
-------

Consider the following symbolizer taken from the Simple Point example in the :ref:`sld_cookbook_points` section in the :ref:`sld_cookbook`.

.. code-block:: xml 
   :linenos: 

          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#FF0000</CssParameter>
                </Fill>
              </Mark>
              <Size>6</Size>
            </Graphic>
          </PointSymbolizer>

The symbolizer contains a ``<Graphic>`` tag, which is required.  Inside this tag is the ``<Mark>`` tag and ``<Size>`` tag, which are the minimum required tags inside ``<Graphic>`` (when not using the ``<ExternalGraphic>`` tag).  The ``<Mark>`` tag contains the ``<WellKnownName>`` tag and a ``<Fill>`` tag.  No other tags are required.  In summary, this example specifies the following:
   
#. Data will be rendered as points
#. Points will be rendered as circles
#. Circles will be rendered with a diameter of 6 pixels and filled with the color red

Further examples can be found in the :ref:`sld_cookbook_points` section of the :ref:`sld_cookbook`.
