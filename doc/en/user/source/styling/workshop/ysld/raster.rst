.. _styling_workshop_raster:

Rasters
=======

Finally we will look at using YSLD styling for the portrayal of raster data.

.. figure:: ../style/img/RasterSymbology.svg

   Raster Symbology


Review of raster symbology:

* Raster data is **Grid Coverage** where values have been recorded in a regular array. In OGC terms a **Coverage** can be used to look up a value or measurement for each location.

* When queried with a "sample" location:

  * A grid coverage can determine the appropriate array location and retrieve a value. Different techniques may be used interpolate an appropriate value from several measurements (higher quality) or directly return the "nearest neighbor" (faster).

  * A vector coverages would use a point-in-polygon check and return an appropriate attribute value.

  * A scientific model can calculate a value for each sample location

* Many raster formats organize information into bands of content. Values recorded in these bands and may be mapped into colors for display (a process similar to theming an attribute for vector data).

  For imagery the raster data is already formed into red, green and blue bands for display.

* As raster data has no inherent shape, the format is responsible for describing the orientation and location of the grid used to record measurements.

These raster examples use a digital elevation model consisting of a single band of height measurements. The imagery examples use an RGB image that has been hand coloured for use as a base map.

Reference:

* :ref:`YSLD Reference <ysld_reference>`
* :ref:`Raster <ysld_reference_symbolizers_raster>` (YSLD Reference | Raster symbolizer)
* :ref:`Raster <sld_reference_rastersymbolizer>` (User Manual | SLD Reference )

The exercise makes use of the ``usgs:dem`` and ``ne:ne1`` layers.

Image
^^^^^

The **raster** symbolizer controls the display of raster data. By default, the raster symbolizer automatically selects the appropriate red, green and blue channels for display.

#. Navigate to the **Styles** page.

#. Click :guilabel:`Add a new style` and choose the following:

   .. list-table::
      :widths: 30 70
      :header-rows: 0

      * - Name:
        - :kbd:`image_example`
      * - Workspace:
        - :kbd:`No workspace`
      * - Format:
        - :kbd:`YSLD`

#. Choose :guilabel:`raster` from the ``Generate a default style`` dropdown and click :guilabel:`generate`.

#. Replace the initial YSLD definition with:

   .. code-block:: yaml

      symbolizers:
      - raster:
          opacity: 1.0

#. And use the :guilabel:`Layer Preview` tab to preview the result.

   .. image:: ../style/img/raster_image_1.png

#. The **channels** property can be used to provide a list three band numbers (for images recording in several wave lengths) or a single band number can be used to view a grayscale image.

   .. code-block:: yaml
      :emphasize-lines: 4,5,6

      symbolizers:
      - raster:
          opacity: 1.0
          channels:
            gray:
              name: '2'

#. Isolating just the green band (it wil be drawn as a grayscale image):

   .. image:: ../style/img/raster_image_2.png

DEM
^^^

A digital elevation model is an example of raster data made up of measurements, rather than color information.

The ``usgs:dem`` layer used used for this exercise:

#. Return to the the **Styles** page.

#. Click :guilabel:`Add a new style` and choose the following:

   .. list-table::
      :widths: 30 70
      :header-rows: 0

      * - Name:
        - :kbd:`raster_example`
      * - Workspace:
        - :kbd:`No workspace`
      * - Format:
        - :kbd:`YSLD`

#. Choose :guilabel:`raster` from the ``Generate a default style`` dropdown and click :guilabel:`generate`.

#. The rendering engine will select our single band of raster content, and do its best to map these values into a grayscale image. Replace the content of the style with:

   .. code-block:: yaml

      symbolizers:
      - raster:
          opacity: 1.0

#. Use the :guilabel:`Layer Preview` tab to preview the result. The range produced in this case from the highest and lowest values.

   .. image:: ../style/img/raster_dem_1.png

#. We can use a bit of image processing to emphasis the generated color mapping by making use of **contrast-enhancement**.

   .. code-block:: yaml
      :emphasize-lines: 4,5,6,7,8

      symbolizers:
      - raster:
          opacity: 1.0
          channels:
            gray:
              name: '1'
              contrast-enhancement:
                mode: histogram

#. Image processing of this sort should be used with caution as it does distort the presentation (in this case making the landscape look more varied then it is in reality.

   .. image:: ../style/img/raster_dem_2.png

Color Map
---------

The approach of mapping a data channel directly to a color channel is only suitable to quickly look at quantitative data.

For qualitative data (such as land use) or simply to use color, we need a different approach:

.. note:: We can use a color map to artificially color a single band raster introducing smooth graduations for elevation or tempurature models or clear differentiation for qualitative data.

#. Apply the following YAML to our `usgs:DEM` layer:

   .. code-block:: yaml
      :emphasize-lines: 4,5,6,7,8,9,10

      symbolizers:
      - raster:
          opacity: 1.0
          color-map:
            type: ramp
            entries:
            - ['#9080DB', 1.0, 0, null]
            - ['#008000', 1.0, 1, null]
            - ['#105020', 1.0, 255, null]
            - ['#FFFFFF', 1.0, 4000, null]

#. Resulting in this artificial color image:

   .. image:: ../style/img/raster_dem_3.png

#. An opacity value can also be used with each **color-map** entry.

   .. code-block:: yaml
      :emphasize-lines: 7

      symbolizers:
      - raster:
          opacity: 1.0
          color-map:
            type: ramp
            entries:
            - ['#9080DB', 0.0, 0, null]
            - ['#008000', 1.0, 1, null]
            - ['#105020', 1.0, 255, null]
            - ['#FFFFFF', 1.0, 4000, null]

#. Allowing the areas of zero height to be transparent:

   .. image:: ../style/img/raster_dem_4.png

.. note:: Raster format for GIS work often supply a "no data" value, or contain a mask, limiting the dataset to only the locations with valid information.

Custom
------

We can use what we have learned about color maps to apply a color brewer palette to our data.

This exploration focuses on accurately communicating differences in value, rather than strictly making a pretty picture. Care should be taken to consider the target audience and medium used during palette selection.

#. Restore the ``raster_example`` YSLD style to the following:

   .. code-block:: yaml

      symbolizers:
      - raster:
          opacity: 1.0

#. Producing the following map preview.

   .. image:: ../style/img/raster_01_auto.png

#. To start with we can provide our own grayscale using two color map entries.

   .. code-block:: yaml
      :emphasize-lines: 4,5,6,7,8

      symbolizers:
      - raster:
          opacity: 1.0
          color-map:
            type: ramp
            entries:
            - ['#000000', 1.0, 0, null]
            - ['#FFFFFF', 1.0, 4000, null]

#. Use the :guilabel:`Layer Preview` tab to zoom in and take a look.

   This is much more direct representation of the source data. We have used our knowledge of elevations to construct a more accurate style.

   .. image:: ../style/img/raster_02_straight.png

#. While our straightforward style is easy to understand, it does leave a bit to be desired with respect to clarity.

   The eye has a hard time telling apart dark shades of black (or bright shades of white) and will struggle to make sense of this image. To address this limitation we are going to switch to the ColorBrewer **9-class PuBuGn** palette. This is a sequential palette that has been hand tuned to communicate a steady change of values.

   .. image:: ../style/img/raster_03_elevation.png

#. Update your style with the following:

   .. code-block:: yaml
      :emphasize-lines: 8,9,10,11,12,13,14,15

      symbolizers:
      - raster:
          opacity: 1.0
          color-map:
            type: ramp
            entries:
            - ['#014636', 1.0, 0, null]
            - ['#016C59', 1.0, 500, null]
            - ['#02818A', 1.0, 1000, null]
            - ['#3690C0', 1.0, 1500, null]
            - ['#67A9CF', 1.0, 2000, null]
            - ['#A6BDDB', 1.0, 2500, null]
            - ['#D0D1E6', 1.0, 3000, null]
            - ['#ECE2F0', 1.0, 3500, null]
            - ['#FFF7FB', 1.0, 4000, null]

   .. image:: ../style/img/raster_04_PuBuGn.png

#. A little bit of work with alpha (to mark the ocean as a no-data section):

   .. code-block:: yaml
      :emphasize-lines: 7,8

      symbolizers:
      - raster:
          opacity: 1.0
          color-map:
            type: ramp
            entries:
            - ['#014636', 0, 0, null]
            - ['#014636', 1.0, 1, null]
            - ['#016C59', 1.0, 500, null]
            - ['#02818A', 1.0, 1000, null]
            - ['#3690C0', 1.0, 1500, null]
            - ['#67A9CF', 1.0, 2000, null]
            - ['#A6BDDB', 1.0, 2500, null]
            - ['#D0D1E6', 1.0, 3000, null]
            - ['#ECE2F0', 1.0, 3500, null]
            - ['#FFF7FB', 1.0, 4000, null]

#. And we are done:

   .. image:: ../style/img/raster_05_alpha.png

Bonus
-----

.. _ysld.raster.q1:

Explore Contrast Enhancement
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. A special effect that is effective with grayscale information is automatic contrast adjustment.

#. Make use of a simple contrast enhancement with ``usgs:dem``:

  .. code-block:: yaml

     symbolizers:
     - raster:
         opacity: 1.0
         contrast-enhancement:
           mode: normalize

#. Can you explain what happens when zoom in to only show a land area (as indicated with the bounding box below)?

   .. image:: ../style/img/raster_contrast_1.png

   .. note:: Discussion :ref:`provided <ysld.raster.a1>` at the end of the workbook.

.. _ysld.raster.q2:

Challenge Intervals
^^^^^^^^^^^^^^^^^^^

#.  The color-map **type** property dictates how the values are used to generate a resulting color.

    * :kbd:`ramp` is used for quantitative data, providing a smooth interpolation between the provided color values.
    * :kbd:`intervals` provides categorization for quantitative data, assigning each range of values a solid color.
    * :kbd:`values` is used for qualitative data, each value is required to have a **color-map** entry or it will not be displayed.

#. **Chalenge:** Update your DEM example to use **intervals** for presentation. What are the advantages of using this approach for elevation data?

   .. note:: Answer :ref:`provided <ysld.raster.a2>` at the end of the workbook.

Explore Image Processing
^^^^^^^^^^^^^^^^^^^^^^^^

Additional properties are available to provide slight image processing during visualization.

.. note:: In this section are we going to be working around a preview issue where only the top left corner of the raster remains visible during image processing. This issue has been reported as  :geos:`6213`.

Image processing can be used to enhance the output to highlight small details or to balance images from different sensors allowing them to be compared.

#. The **contrast-enhancement** property is used to turn on a range of post processing effects. Settings are provided for :kbd:`normalize` or :kbd:`histogram` or :kbd:`none`;

  .. code-block:: yaml

     symbolizers:
     - raster:
         opacity: 1.0
         contrast-enhancement:
           mode: normalize

#. Producing the following image:

   .. image:: ../style/img/raster_image_3.png

#. The **raster-gamma** property is used adjust the brightness of **contrast-enhancement** output. Values less than 1 are used to brighten the image while values greater than 1 darken the image.

  .. code-block:: yaml

     symbolizers:
     - raster:
         opacity: 1.0
         contrast-enhancement:
           gamma: 1.5

#. Providing the following effect:

   .. image:: ../style/img/raster_image_4.png

.. _ysld.raster.q3:

Challenge Clear Digital Elevation Model Presentation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Now that you have seen the data on screen and have a better understanding how would you modify our initial gray-scale example?

#. **Challenge:** Use what you have learned to present the ``usgs:dem`` clearly.

   .. note:: Answer :ref:`provided <ysld.raster.a3>` at the end of the workbook.

.. _ysld.raster.q4:

Challenge Raster Opacity
^^^^^^^^^^^^^^^^^^^^^^^^

#. There is a quick way to make raster data transparent, raster **opacity** property works in the same fashion as with vector data. The raster as a whole will be drawn partially transparent allow content from other layers to provide context.

#. **Challenge:** Can you think of an example where this would be useful?

   .. note:: Discussion :ref:`provided <ysld.raster.a4>` at the end of the workbook.
