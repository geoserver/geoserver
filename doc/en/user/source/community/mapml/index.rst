.. _mapml:

MapML 
====================

Map Markup Language (MapML) is a text-based format which allows map authors to encode map information as hypertext documents exchanged over the Uniform Interface of the Web. The format definition is still a work-in-progress by the Maps for HTML W3C Community Group, but various tools to work with the format already exist, including a Leaflet-based map viewer. For more information on MapML refer to the `Maps for HTML Community Group <https://maps4html.github.io/>`_.

The MapML module for GeoServer provides MapML resources to access WMS and WFS services provided by Geoserver. The MapML modules includes support for styles, tiling, querying, sharding, and dimensions options for WMS layers, and also provides a MapML outputFormat for WMS GetFeatureInfo and WFS GetFeatures requests. See below for information on installing and configuring the MapML module.


Installation
--------------------

#. Download the MapML extension from the `nightly GeoServer community module builds <https://build.geoserver.org/geoserver/master/community-latest/>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance.

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.


Configuration
-------------

Configuration can be done using the administrator GUI.


Usage
-----

