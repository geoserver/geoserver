.. _community_schemaless_features:

Schemaless Features Plugin
==========================
This plugin includes support for serving complex features directly from Schemaless Feature sources.

.. warning::

   At the time of writing only MongoDB support is provided.
   The plug-in supports only the following services/operation with the indicated output formats:

   * WMS:
      * GetMap (all the formats)
      * GetFeatureInfo (GeoJSON and HTML outputs only)
   * WFS:
      * GetFeature (GeoJSON output only)

.. toctree::
   :caption: documentation
   :maxdepth: 1
   
   install
   schemaless-mongo/index