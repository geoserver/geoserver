.. _kml_extension:
.. _google_earth:

KML
===

This section documents the KML extension, which provides Google Earth (KML/KMZ) output support for GeoServer.

KML (Keyhole Markup Language) is a markup language for geographic visualization used by Earth browsers such as Google Earth and Google Maps. GeoServer integrates with these clients by exposing KML as a native output format. Any published data can take advantage of the visualization capabilities in KML-aware clients once the KML extension is installed.

To use this functionality, install the KML extension package (:download_extension:`kml`) into the GeoServer ``WEB-INF/lib`` directory.

KMZ output
----------

Alongside raw KML, GeoServer can generate KMZ (compressed KML) responses by requesting the ``application/vnd.google-earth.kmz`` output format. The resulting KMZ package is a zip archive that contains the KML document and any bundled assets, such as icon PNG files stored inside an ``images/`` directory. See :ref:`google_earth_kmz` for practical usage notes.

.. toctree::
   :maxdepth: 2

   overview.rst
   quickstart.rst
   kmlstyling.rst
   tutorials/index.rst
   features/index.rst
