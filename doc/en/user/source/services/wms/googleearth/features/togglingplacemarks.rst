.. _ge_feature_toggling_placemarks:

Toggling Placemarks
===================

Vector Placemarks
-----------------

When GeoServer generates KML for a vector dataset, it attaches information from the data to each feature that is created. When clicking on a vector feature, a pop-up window is displayed. This is called a **placemark**. By default this is a simple list which displays attribute data, although this information can be customized using Freemarker templates.

If you would like this information not to be shown when a feature is clicked (either for security reasons, or simply to have a cleaner user interface), it is possible to disable this functionality. To do so, use the ``kmattr`` parameter in a KML request to turn off attribution.

The syntax for ``kmattr`` is as follows::

   format_options=kmattr:[true|false]

Note that ``kmattr`` is a "format option", so the syntax is slightly different from the usual key-value pair. For example::

   http://localhost:8080/geoserver/wms/kml?layers=topp:states&format_options=kmattr:false

Raster Placemarks
-----------------

Unlike vector features, where the placemark is enabled by default, placemarks are disabled by default with raster images of features. To enable this feature, you can use the ``kmplacemark`` format option in your KML request. The syntax is similar to the ``kmattr`` format option specified above::

   format_options=kmplacemark:[true|false]

For example, using the KML reflector, the syntax would be::

   http://localhost:8080/geoserver/wms/kml?layers=topp:states&format_options=kmplacemark:true