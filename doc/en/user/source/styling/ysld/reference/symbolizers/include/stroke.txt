.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1
   :widths: 20 10 50 20

   * - Property
     - Required?
     - Description
     - Default value
   * - ``stroke-color``
     - No
     - Color of line features.
     - ``'#000000'`` (black)
   * - ``stroke-width``
     - No
     - Width of line features, measured in pixels.
     - ``1``
   * - ``stroke-opacity``
     - No
     - Opacity of line features. Valid values are a decimal value between ``0`` (completely transparent) and ``1`` (completely opaque).
     - ``1``
   * - ``stroke-linejoin``
     - No
     - How line segments are joined together. Options are ``mitre`` (sharp corner), ``round`` (round corner), and ``bevel`` (diagonal corner).
     - ``mitre``
   * - ``stroke-linecap``
     - No
     - How line features are rendered at their ends. Options are ``butt`` (sharp square edge), ``round`` (rounded edge), and ``square`` (slightly elongated square edge).
     - ``butt``
   * - ``stroke-dasharray``
     - No
     - A numeric list signifying length of lines and gaps, creating a dashed effect. Units are pixels, so ``"2 3"`` would be a repeating pattern of 2 pixels of drawn line followed by 3 pixels of blank space. If only one number is supplied, this will mean equal amounts of line and gap.
     - No dash
   * - ``stroke-dashoffset``
     - No
     - Number of pixels into the dasharray to offset the drawing of the dash, used to shift the location of the lines and gaps in a dash.
     - ``0``
   * - ``stroke-graphic``
     - No
     - A design or pattern to be used along the stroke. Output will always be a linear repeating pattern, and as such is not tied to the value of ``stroke-width``. Can either be a mark consisting of a common shape or a URL that points to a graphic. The ``<graphic_options>`` should consist of a mapping containing ``symbols:`` followed by an ``external:`` or ``mark:``, with appropriate parameters as detailed in the :ref:`ysld_reference_symbolizers_point` section. Cannot be used with ``stroke-graphic-fill``.
     - N/A
   * - ``stroke-graphic-fill``
     - No
     - A design or pattern to be used for the fill of the stroke. The area that is to be filled is tied directly to the value of ``stroke-width``. Can either be a mark consisting of a common shape or a URL that points to a graphic. The ``<graphic_options>`` should consist of a mapping containing ``symbols:`` followed by an ``external:`` or ``mark:``, with appropriate parameters as detailed in the :ref:`ysld_reference_symbolizers_point` section. Cannot be used with ``stroke-graphic``.
     - N/A
