.. _geopkgoutput.usage:

Using the GeoPackage Output Extension
-------------------------------------

The GeoPackage Output Extension adds support to WFS and WMS to request ``GetFeature`` and ``GetMap`` results in GeoPackage Format.

WFS
^^^


Add ``&outputFormat=geopkg`` to your request. The result will be a GeoPackage (MIME type `application/geopackage+sqlite3`) containing the requested features.

.. code-block::  

    curl "http://localhost:8080/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeNames=ws:layername&outputFormat=geopkg" \
    -o wfs.gpkg

.. note::

    You can use `geopkg`, `geopackage`, or `gpkg` as the output format in the request.  Use `1.0.0`, `1.1.0`, or `2.0.0` as ``version=`` to specify which WFS version to use.


WMS
^^^

Add ``&format=geopkg`` to your request. The result will be a GeoPackage (MIME type `application/geopackage+sqlite3`) containing the requested tiles.


.. code-block::  

    curl "http://localhost:8080/geoserver/dave/wms?service=WMS&version=1.1.0&request=GetMap&layers=ws:layername&bbox=-123.43670607166865%2C48.3956835%2C-123.2539813%2C48.5128362547052&width=1536&height=984&srs=EPSG%3A4326&styles=&format=geopkg" \
    -o wms.gpkg


.. note::

    You can use `geopkg`, `geopackage`, or `gpkg` as the output format in the request.  Use `1.1.0`, `1.1.1`, or `1.3` as ``version=`` to specify which WFS version to use.
