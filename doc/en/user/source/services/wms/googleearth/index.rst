.. _google_earth:

Google Earth
============

This section contains information on Google Earth support in GeoServer.

Google Earth is a 3-D virtual globe program. A `free download <http://earth.google.com/>`_ from Google, it allows the user to virtually view, pan, and fly around Earth imagery. The imagery on Google Earth is obtained from a variety of sources, mainly from commercial satellite and aerial photography providers.

Google Earth recognizes a markup language called `KML <http://earth.google.com/kml/kml_intro.html>`_ (Keyhole Markup Language) for data exchange. GeoServer integrates with Google Earth by supporting KML as a native output format. Any data configured to be served by GeoServer is thus able to take advantage of the full visualization capabilities of Google Earth.

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
