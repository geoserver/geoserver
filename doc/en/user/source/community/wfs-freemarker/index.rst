.. _community_wfsfreemarker:

WFS FreeMarker Extension
===============================

The WFS FreeMarker plugin allows to apply a FreeMarker template to a WFS `GetFeature` response to generate HTML output, similarly to what already happens for WMS `GetFeatureInfo` requests.

It reuses the same logic and machinery used in templating WMS responses, leveraging the mechanism of `org.geoserver.wms.featureinfo.HTMLTemplateManager`

This feature is also utilizable in the "Layer Preview" page in GeoServer, where a new “HTML” output format is made available for the WFS preview.