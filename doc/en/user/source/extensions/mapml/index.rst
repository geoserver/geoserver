.. _mapml:

MapML 
=========

Map Markup Language (MapML) is a text-based format which allows map authors to encode map information as hypertext documents exchanged over the Uniform Interface of the Web. The format definition is a work-in-progress by the Maps for HTML W3C Community Group. Various tools to work with the format exist, including a Leaflet-based map viewer included in the GeoServer MapML extension. For more information on MapML refer to the `Maps for HTML Community Group <https://maps4html.org/>`.

The MapML module for GeoServer adds new MapML resources to access WMS, WMTS and WFS services configured in Geoserver. The MapML modules includes support for styles, tiling, querying, and dimensions options for WMS layers, and also provides a MapML outputFormat for WMS GetFeatureInfo and WFS GetFeatures requests. See below for information on installing and configuring the MapML module.

    .. note:: The Maps for HTML community kindly requests that if you use MapML and the software provided here or elsewhere, that you give us feedback about your experience: open an issue or start a discussion on `GitHub <https://github.com/Maps4HTML>`_.

    .. warning:: MapML is an experimental proposed extension of HTML for the Web. The objective of the project is to standardize map, layer and feature semantics in HTML.  As the format progresses towards a Web standard, it may change slightly.  Always use the latest version of this extension, and follow or join in the project's progress at https://maps4html.org.


.. toctree::
   :maxdepth: 2
   
   installation
   template

