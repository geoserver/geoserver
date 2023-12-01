.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1
   :widths: 20 10 50 20

   * - Property
     - Required?
     - Description
     - Default value
   * - ``fill-color``
     - No
     - Color of inside of features.
     - ``'#808080'`` (gray)
   * - ``fill-opacity``
     - No
     - Opacity of the fill. Valid values are a decimal value between ``0`` (completely transparent) and ``1`` (completely opaque).
     - ``1``
   * - ``fill-graphic``
     - No
     - A design or pattern to be used for the fill of the feature. Can either be a mark consisting of a common shape or a URL that points to a graphic. The ``<graphic_options>`` should consist of a mapping containing ``symbols:`` followed by an ``external:`` or ``mark:``, with appropriate parameters as detailed in the :ref:`ysld_reference_symbolizers_point` section.
     - None

The use of ``fill-graphic`` allows for the following extra options:

.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1
   :widths: 20 10 50 20

   * - Property
     - Required?
     - Description
     - Default value
   * - ``x-graphic-margin``
     - No
     - Used to specify margins (in pixels) around the graphic used in the fill. Possible values are a list of four (``top, right, bottom, left``), a list of three (``top, right and left, bottom``), a list of two (``top and bottom, right and left``), or a single value. 
     - N/A
   * - ``x-random``
     - No
     - Activates random distribution of symbols. Possible values are ``free`` or ``grid``. ``free`` generates a completely random distribution, and ``grid`` will generate a regular grid of positions, and only randomize the position of the symbol around the cell centers, providing a more even distribution.
     - N/A
   * - ``x-random-tile-size``
     - No
     - When used with ``x-random``, determines the size of the grid (in pixels) that will contain the randomly distributed symbols.
     - ``256``
   * - ``x-random-rotation``
     - No
     - When used with ``x-random``, activates random symbol rotation. Possible values are ``none`` or ``free``.
     - ``none``
   * - ``x-random-symbol-count``
     - No
     - When used tih ``x-random``, determines the number of symbols drawn. Increasing this number will generate a more dense distribution of symbols
     - ``16``
   * - ``x-random-seed``
     - No
     - Determines the "seed" used to generate the random distribution. Changing this value will result in a different symbol distribution.
     - ``0``

.. todo:: Add examples using random