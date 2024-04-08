.. _community_wfsfreemarker:

WFS FreeMarker Extension
========================

The WFS FreeMarker plugin enables the application of a FreeMarker template to a WFS GetFeature response, allowing for the generation of HTML output, akin to the functionality already present for WMS GetFeatureInfo requests.

It leverages the same logic and machinery utilized in templating WMS responses, utilizing the same Freemarker templates already in use for the GetFeatureInfo HTML output.

This feature is also accessible on the :guilabel:`Layer Preview` page in GeoServer, where a new `HTML` output format is provided for the WFS preview.

.. toctree::
   :maxdepth: 2

   installing
   configuration