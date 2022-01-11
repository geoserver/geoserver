.. _qgis:

Generating SLD styles with QGIS
===============================

QGIS includes a sophisticated style editor with many map rendering possibilities. Styles generated
with QGIS can then be exported (with limitations) to SLD for usage with GeoServer.

QGIS style exporting abilities have been evolving over time, as a reference:

* For vector data QGIS exports SLD 1.1 styles that can be read by GeoServer. In order to get
  the suitable results it's important to use QGIS 3.0 or newer, and GeoServer 2.13.x or newer.
* Raster data styling export is new in QGIS 3.4.5 (yet to be released at the time of writing).
  This new version exports SLD 1.0 styles with vendor extensions to support constrast streching that most recent GeoServer versions support properly. For older QGIS versions limited export functionality is available using the SLD4Raster plugin.

For the export it is advised to use the :guilabel:`Save As` functionality available in the style dialog,
as indicated below in this guide. Other plugins exist that streamline the export process, but they
may ruin the style trying to adapt it to older GeoServer versions (e.g., translating it
down to SLD 1.0 by simple text processing means), or rewrite it entirely.

.. warning:: Despite the progress in the last years, it is known that not all QGIS rendering
   options are supported by SLD and/or by GeoServer (e.g. shapeburst symbology),
   and that support for exporting some parts  is simply missing (e.g.. expression based symbology is
   supported in SLD, but QGIS won't export it). If you are interested, both projects would welcome
   sponsoring to improve the situation.


Exporting vector symbology
--------------------------

This is a step by step guide to style a GeoServer demo layer, ``sfdem``.

#. Open :command:`QGIS` (minimum version 3.0)
#. Load the :file:`states.shp` dataset from the GeoServer data directory, :file:`<GEOSERVER_DATA_DIR>/data/shapefiles/states.shp`
#. Double click the layer to open the :guilabel:`Properties` dialog and switch to the :guilabel:`Symbology` page.
#. Choose a `Graduated` rendering, on the ``PERSONS`` column, and click on :guilabel:`Classify` button to generate `1.5` standard deviations, select the `spectral` color ramp, switch mode to `Quantile` and finally and click on the ":guilabel:`Classify` button to generate a 5 classes map, as shown in figure.

   .. figure:: images/qgis-vector-style.png
      :align: center

      QGIS vector styling

#. Switch to the :guilabel:`Labels` page, choose `Single labels``, label with the ``STATE NAME`` attribute and choose your preferred text rendering options, as shown in figure

   .. figure:: images/qgis-label-style.png
      :align: center

      QGIS labelling

#. The layer renders as follows:

   .. figure:: images/qgis-vector-render.png
      :align: center

      QGIS raster styling

#. Go back At the :guilabel:`Properties` dialog, from the bottom of the :guilabel:`Styles` page, choose :menuselection:`Style --> Save Style`.

   .. figure:: images/qgis-vector-saveas.png
      :align: center

      *Export using Save As...*

#. Choose export in the SLD format, placing the file in the desired location.

   .. figure:: images/qgis-choose-format.png
      :align: center

      Choosing export format...

#. Go in GeoServer, create a new style, use the :guilabel:`Upload a new style` dialog to choose the exported file, and click on `upload` link.

   .. figure:: images/gs-vector-upload.png
      :align: center

      Uploading style in GeoServer...

#. Click on guilabel:`Apply`.

#. Change to the :guilabel:`Layer preview` tab, click on the :guilabel:`Preview on Layer` link to choose ``topp:states`` to verify  proper rendering.

   .. figure:: images/gs-vector-preview.png
      :align: center

      Previewing style in GeoServer...

#. Eventually switch to the :guilabel:`Publishing` tab, search for ``states``, and select :guilabel:`Default` or :guilabel:`Associated` checkbox to publish the layer to use the new style permanently.

   .. figure:: images/gs-vector-associate.png
      :align: center

      Associating style in GeoServer...

Exporting raster symbology
--------------------------
The following are a couple of examples on how to export raster layers' symbology in QGIS and how to use the resulting SLD to style layers in GeoServer.

.. warning:: As mentioned above, this functionality has some limitations:

  * :guilabel:`Hillshading` vendor options are not fully supported by GeoServer so you can't choose the `Band` and the position of the sun (`Altitude` and `Azimuth`), the `Multidirectional` option is not supported too
  * GeoServer is not able to interpret the :guilabel:`Color Rendering` options yet

This is a step by step guide to style a GeoServer demo layer, ``sfdem``.

#. Open QGIS (minimum version 3.4.5)
#. Load the :file:`sfdem.tif` raster from the GeoServer data directory, :file:`<GEOSERVER_DATA_DIR>/data/sf/sfdem.tif`
#. Double click the layer to open the :guilabel:`Properties` dialog and switch to the :guilabel:`Symbology` page.
#. Choose a `Singleband pseudocolor` rendering, Generate :guilabel:`Min / Max Value Settings` using :guilabel:`Mean +/- standard deviation` with using ``1.5`` standard deviations. Generate a 5 classes :guilabel:`Linear` interpolated map, as shown in figure.

   .. figure:: images/qgis-raster-style.png
      :align: center

      QGIS raster styling

#. The layer renders as follows:

   .. figure:: images/qgis-raster-render.png
      :align: center

      QGIS raster styling

#. Return to the layer's :guilabel:`Properties` dialog :guilabel:`Symbology` page, at the bottom of the page  choose :menuselection:`Style --> Save Style`.

   .. figure:: images/qgis-raster-saveas.png
      :align: center

      Export using Save As...

#. Choose export in the SLD format, placing the file in the desired location

   .. figure:: images/qgis-choose-format.png
      :align: center

      Choosing export format...

#. Go in GeoServer, create a new style, use the :guilabel:`Upload a new style` dialog to choose the exported file, and click on `upload` link.

   .. figure:: images/gs-raster-upload.png
      :align: center

      Uploading style in GeoServer...

#. Click on guilabel:`Apply` then change to the :guilabel:`Layer preview` tab. Click on the :guilabel:`Preview on Layer` link to choose ``sfdem`` to verify  proper rendering.

   .. figure:: images/gs-raster-preview.png
      :align: center

      Previewing style in GeoServer...

#. Finally switch to the :guilabel:`Publishing` tab, search for ``sfdem`` layer, and select :guilabel:`Default` or :guilabel:`Associated` checkbox to publish ``sfdem`` with the new style.

   .. figure:: images/gs-raster-associate.png
      :align: center

      Associating style in GeoServer...

The next example shows how to style an aerial image instead.

#. Download an aerial image (for example from `USGS Landsat image archives <https://landsatlook.usgs.gov/sentinel2/viewer.html>`_) if you do not already have one. Give it a name (``aerial`` in this example) and :guilabel:`save it as GeoTIFF`

   .. figure:: images/landsat_usgs_sentinel2.png
      :align: center

      aerial.tiff

#. Open GeoServer, :guilabel:`create a new Store` (see :ref:`Add a Store <data_webadmin_stores_add_a_store>`), :guilabel:`add a GeoTIFF Raster Data Source` to the Store and :guilabel:`connect` it to your ``aerial.tif`` file
#. In GeoServer, :guilabel:`create a new Layer` (see :ref:`Add a Layer <data_webadmin_layers_add_a_layer>`) choosing the Store you have created in the previous step
#. Open QGIS (minimum version 3.4.5)
#. Load the ``aerial.tif`` raster
#. Double click the layer to open the :guilabel:`Properties` dialog and switch to the :guilabel:`Symbology` page
#. Choose a `Multiband color` rendering, set the :guilabel:`bands` (Red band == Band 1 (red), Green band == Band 2 (Green), Blue band == Band 3 (Blue)), generate :guilabel:`Min / Max Value Settings` using ``5,0 - 95,0 % range`` of :guilabel:`Cumulative count cut` and select ``Stretch to MinMax`` as :guilabel:`Contrast enhancement` option, as shown in the picture below

   .. figure:: images/qgis-sentinel2-raster-style.png
      :align: center

      QGIS layer properties - Symbology

#. The layer renders as follows:

   .. figure:: images/qgis-sentinel2-raster-rendering.png
      :align: center

      QGIS layer rendering

#. :guilabel:`Save the Style` as SLD

#. Go in GeoServer, use the generated SLD to :guilabel:`create a new style`, choose the ``aerial`` layer through the :guilabel:`Preview on Layer` link and verify if the layer is properly rendered (see the previous example for further details)

   .. figure:: images/gs-sentinel2-raster-rendering.png
      :align: center

      GeoServer layer rendering

#. Finally :guilabel:`Publish` the ``aerial`` layer with the new style as described in the previous example.
