.. _mbstyle_source:

Publishing a GeoServer Layer for use with Mapbox Styles
=======================================================

GeoServer can be configured to serve layers as vector tiles which can be used as sources for Mapbox styles rendered by client-side applications such as OpenLayers.

1. :ref:`production_container.enable_cors` in GeoServer.

2. Install the :ref:`Vector Tiles <vectortiles.install>` extension.

3. Follow the :ref:`vectortiles.tutorial` to publish your layers in ``application/vnd.mapbox-vector-tile`` format (You only need to do the "Publish vector tiles in GeoWebCache" step).

Once these steps are complete, you will be able to use your GeoServer layers in any Mapbox-compatible client application that can access your GeoServer.

The source syntax to use these GeoServer layers in your MapBox Style is::

    "<source-name>": {
      "type": "vector",
      "tiles": [
        "http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetTile&SERVICE=WMTS
            &VERSION=1.0.0&LAYER=<workspace>:<layer>&STYLE=&TILEMATRIX=EPSG:900913:{z}
            &TILEMATRIXSET=EPSG:900913&FORMAT=application/vnd.mapbox-vector-tile
            &TILECOL={x}&TILEROW={y}"
      ],
      "minZoom": 0,
      "maxZoom": 14
    }

.. note:: 

   ``<workspace>`` and ``<layer>`` should be replaced by the workspace and name of the layer in question. ``{x}``, ``{y}``, and ``{z}`` are placeholder values for the tile indices and should be preserved as written.

.. note:: 

   ``<source-name>`` should be replaced by a source name of your choice. It will be used to refer to the source elsewhere in the Mapbox Style.

.. note:: 

   If geoserver is not being served from ``localhost:8080``, update the domain accordingly.