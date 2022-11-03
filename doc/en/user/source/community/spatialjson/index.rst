.. _spatialjson:

SpatialJSON WFS Output Format Extension
=======================================

This module adds the SpatialJSON WFS output format. The SpatialJSON format is a more compact and
memory-friendly variant of GeoServer's GeoJSON format. It aims to save space by applying several
optimizations to traditional GeoJSON format for simple feature results. Most of these optimizations
work by removing redundand information from the JSON-encoded features.

A service exception is thrown if the result contains complex features as the SpatialJSON format
does not handle those.

.. note:: The SpatialJSON format is **not compatible** with GeoJSON. A SpatialJSON enabled reader is required to decode features transferred in SpatialJSON format.

This module adds two additional WFS output formats for requesting simple features in SpatialJSON
format:

-  ``application/json; subtype=json/spatial`` for requesting SpatialJSON
-  ``text/javascript; subtype=json/spatial`` for requesting SpatialJSON as a JSONP request

.. warning:: At the time of writing, this format is still *work in progress* and changes may be applied in the future.

.. toctree::
    :maxdepth: 1

    installation
    development
    schema
    attributes
