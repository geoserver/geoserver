.. _sld-extensions_composite-blend:

Color compositing and color blending
====================================

It is possible to perform color blending and compositing, either between feature type styles or by associating blending operations with each symbolizer.

GeoServer implements most of the color compositing and blending modes suggested by the `SVG compositing and blending level 1 specification <http://www.w3.org/TR/compositing-1/>`_. Either set of operations allows one to control how two overlapping layers/symbols are merged together to form a final map (as opposed to the normal behavior of just stacking images on top of each other).

This section will use the following definitions for the common terms "source" and "destination":

* **Source** : Image currently being painted *on top of* the map
* **Destination**:  *Background* image that the source image is being drawn on

.. toctree::
   :maxdepth: 2

   syntax
   modes
   example
