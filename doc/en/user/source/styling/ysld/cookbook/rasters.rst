.. _ysld_cookbook.rasters:

Rasters
=======

Rasters are geographic data displayed in a grid. They are similar to image files such as PNG files, except that instead of each point containing visual information, each point contains geographic information in numerical form. Rasters can be thought of as a georeferenced table of numerical values.

One example of a raster is a Digital Elevation Model (DEM) layer, which has elevation data encoded numerically at each georeferenced data point.

Example raster
--------------

The :download:`raster layer <artifacts/ysld_cookbook_raster.zip>` that is used in the examples below contains elevation data for a fictional world. The data is stored in EPSG:4326 (longitude/latitude) and has a data range from 70 to 256. If rendered in grayscale, where minimum values are colored black and maximum values are colored white, the raster would look like this:

.. figure:: ../../sld/cookbook/images/raster.png

   Raster file as rendered in grayscale

:download:`Download the raster shapefile <artifacts/ysld_cookbook_raster.zip>`

.. _ysld_cookbook_raster_twocolorgradient:


Two-color gradient
------------------

This example shows a two-color style with green at lower elevations and brown at higher elevations.

.. figure:: ../../sld/cookbook/images/raster_twocolorgradient.png

   Two-color gradient

Code
~~~~

:download:`Download the "Two-color gradient" YSLD <artifacts/raster_twocolorgradient.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Two color gradient'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - raster:
          opacity: 1.0
          color-map:
            type: ramp
            entries:
            - ['#008000',1,70,'']
            - ['#663333',1,256,'']

Details
~~~~~~~

There is one rule in one feature style for this example, which is the simplest possible situation. All subsequent examples will share this characteristic. Styling of rasters is done via the raster symbolizer (**lines 2-7**).

This example creates a smooth gradient between two colors corresponding to two elevation values. The gradient is created via the ``color-map`` on **lines 8-12**. Each entry in the ``color-map`` represents one entry or anchor in the gradient. **Line 11** sets the lower value of 70 and color to a dark green (``'#008000'``). **Line 12** sets the upper value of 256 and color to a dark brown (``'#663333'``). **Line 9** sets the type to ``ramp``, which means that all data values in between these two quantities will be linearly interpolated:  a value of 163 (the midpoint between 70 and 256) will be colored as the midpoint between the two colors (in this case approximately ``'#335717'``, a muddy green).

Transparent gradient
--------------------

This example creates the same two-color gradient as in the :ref:`ysld_cookbook_raster_twocolorgradient` as in the example above but makes the entire layer mostly transparent by setting a 30% opacity.

.. figure:: ../../sld/cookbook/images/raster_transparentgradient.png

   Transparent gradient

Code
~~~~

:download:`Download the "Transparent gradient" YSLD <artifacts/raster_transparentgradient.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Transparent gradient'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - raster:
          opacity: 0.3
          color-map:
            type: ramp
            entries:
            - ['#008000',1,70,'']
            - ['#663333',1,256,'']

Details
~~~~~~~


This example is similar to the :ref:`ysld_cookbook_raster_twocolorgradient` example save for the addition of **line 7**, which sets the opacity of the layer to 0.3 (or 30% opaque). An opacity value of 1 means that the shape is drawn 100% opaque, while an opacity value of 0 means that the shape is rendered as completely transparent. The value of 0.3 means that the the raster partially takes on the color and style of whatever is drawn beneath it. Since the background is white in this example, the colors generated from the ``color-map`` look lighter, but were the raster imposed on a dark background the resulting colors would be darker.


Brightness and contrast
-----------------------

This example normalizes the color output and then increases the brightness by a factor of 2.

.. figure:: ../../sld/cookbook/images/raster_brightnessandcontrast.png

   Brightness and contrast
 
Code
~~~~

:download:`Download the "Brightness and contrast" YSLD <artifacts/raster_brightnessandcontrast.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Brightness and contrast'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - raster:
          opacity: 1
          color-map:
            type: ramp
            entries:
            - ['#008000',1,70,'']
            - ['#663333',1,256,'']
          contrast-enhancement:
            mode: normalize
            gamma: 0.5

Details
~~~~~~~

This example is similar to the :ref:`ysld_cookbook_raster_twocolorgradient`, save for the addition of the ``contrast-enhancement`` parameter on **lines 13-15**. **Line 14** normalizes the output by increasing the contrast to its maximum extent. **Line 15** then adjusts the brightness by a factor of 0.5. Since values less than 1 make the output brighter, a value of 0.5 makes the output twice as bright.

As with previous examples, **lines 8-12** determine the ``color-map``, with **line 11** setting the lower bound (70) to be colored dark green (``'#008000'``) and **line 12** setting the upper bound (256) to be colored dark brown (``'#663333'``). 



Three-color gradient
--------------------

This example creates a three-color gradient in primary colors.

.. figure:: ../../sld/cookbook/images/raster_threecolorgradient.png

   Three-color gradient

Code
~~~~

:download:`Download the "Three-color gradient" YSLD <artifacts/raster_threecolorgradient.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Three color gradient'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - raster:
          opacity: 1
          color-map:
            type: ramp
            entries:
            - ['#0000FF',1,150,'']
            - ['#FFFF00',1,200,'']
            - ['#FF0000',1,250,'']

Details
~~~~~~~

This example creates a three-color gradient based on a ``color-map`` with three entries on **lines 8-13**: **line 11** specifies the lower bound (150) be styled in blue (``'#0000FF'``), **line 12** specifies an intermediate point (200) be styled in yellow (``'#FFFF00'``), and **line 13** specifies the upper bound (250) be styled in red (``'#FF0000'``).

Since our data values run between 70 and 256, some data points are not accounted for in this style. Those values below the lowest entry in the color map (the range from 70 to 149)  are styled the same color as the lower bound, in this case blue. Values above the upper bound in the color map (the range from 251 to 256) are styled the same color as the upper bound, in this case red.


Alpha channel
-------------

This example creates an "alpha channel" effect such that higher values are increasingly transparent.

.. figure:: ../../sld/cookbook/images/raster_alphachannel.png

   Alpha channel

Code
~~~~

:download:`Download the "Alpha channel" YSLD <artifacts/raster_alphachannel.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Alpha channel'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - raster:
          opacity: 1
          color-map:
            type: ramp
            entries:
            - ['#008000',1,70,'']
            - ['#008000',0,256,'']

Details
~~~~~~~

An alpha channel is another way of referring to variable transparency. Much like how a gradient maps values to colors, each entry in a ``color-map`` can have a value for opacity (with the default being 1.0 or completely opaque).

In this example, there is a ``color-map`` with two entries: **line 11** specifies the lower bound of 70 be colored dark green (``'#008000'``), while **line 13** specifies the upper bound of 256 also be colored dark green but with an opacity value of 0. This means that values of 256 will be rendered at 0% opacity (entirely transparent). Just like the gradient color, the opacity is also linearly interpolated such that a value of 163 (the midpoint between 70 and 256) is rendered at 50% opacity.


Discrete colors
---------------

This example shows a gradient that is not linearly interpolated but instead has values mapped precisely to one of three specific colors.

.. figure:: ../../sld/cookbook/images/raster_discretecolors.png

   Discrete colors

Code
~~~~

:download:`Download the "Discrete colors" YSLD <artifacts/raster_discretecolors.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Discrete colors'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - raster:
          opacity: 1
          color-map:
            type: intervals
            entries:
            - ['#008000',1,150,'']
            - ['#663333',1,256,'']

Details
~~~~~~~

Sometimes color bands in discrete steps are more appropriate than a color gradient. The ``type: intervals`` parameter added to the ``color-map`` on **line 9** sets the display to output discrete colors instead of a gradient. The values in each entry correspond to the upper bound for the color band such that colors are mapped to values less than the value of one entry but greater than or equal to the next lower entry. For example, **line 11** colors all values less than 150 to dark green (``'#008000'``) and **line 12** colors all values less than 256 but greater than or equal to 150 to dark brown (``'#663333'``).


Many color gradient
-------------------

This example shows a gradient interpolated across eight different colors.

.. figure:: ../../sld/cookbook/images/raster_manycolorgradient.png

   Many color gradient

Code
~~~~

:download:`Download the "Many color gradient" YSLD <artifacts/raster_manycolorgradient.ysld>`

.. code-block:: yaml
  :linenos:

  title: 'YSLD Cook Book: Many color gradient'
  feature-styles:
  - name: name
    rules:
    - symbolizers:
      - raster:
          opacity: 1
          color-map:
            type: ramp
            entries:
            - ['#000000',1,95,'']
            - ['#0000FF',1,110,'']
            - ['#00FF00',1,135,'']
            - ['#FF0000',1,160,'']
            - ['#FF00FF',1,185,'']
            - ['#FFFF00',1,210,'']
            - ['#00FFFF',1,235,'']
            - ['#FFFFFF',1,256,'']

Details
~~~~~~~

A ``color-map`` can include up to 255 entries. 
This example has eight entries (**lines 11-18**):

.. list-table::
   :widths: 15 25 30 30 
   :header-rows: 1

   * - Entry number
     - Value
     - Color
     - RGB code
   * - 1
     - 95
     - Black
     - ``'#000000'``
   * - 2
     - 110
     - Blue
     - ``'#0000FF'``
   * - 3
     - 135
     - Green
     - ``'#00FF00'``
   * - 4
     - 160
     - Red
     - ``'#FF0000'``
   * - 5
     - 185
     - Purple
     - ``'#FF00FF'``
   * - 6
     - 210
     - Yellow
     - ``'#FFFF00'``
   * - 7
     - 235
     - Cyan
     - ``'#00FFFF'``
   * - 8
     - 256
     - White
     - ``'#FFFFFF'``
