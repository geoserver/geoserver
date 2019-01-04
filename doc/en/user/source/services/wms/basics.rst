.. _wms_basics:

WMS basics
==========

GeoServer provides support for Open Geospatial Consortium (OGC) **Web Map Service (WMS)** versions 1.1.1 and 1.3.0.  
This is the most widely used standard for generating maps on the web, and it is the primary interface to request map products from GeoServer.  
Using WMS makes it possible for clients to overlay maps from several different sources in a seamless way.

GeoServer's WMS implementation fully supports the standard, and is certified compliant against the OGC's test suite.  
It includes a wide variety of rendering and labeling options, and is one of the fastest WMS Servers for both raster and vector data.  

GeoServer WMS supports reprojection to any **coordinate reference system** in the EPSG database.
It is possible to add additional coordinate systems if the Well Known Text definition is known.
See :ref:`crs_handling` for details.

GeoServer fully supports the **Styled Layer Descriptor (SLD)** standard, and uses SLD files as its native styling language.  
For more information on how to style data in GeoServer see the section :ref:`styling`

Differences between WMS versions
--------------------------------

The major differences between versions 1.1.1 and 1.3.0 are:

* In 1.1.1 geographic coordinate systems specified with the ``EPSG`` namespace 
  are defined to have an axis ordering of longitude/latitude. In 1.3.0 the 
  ordering is latitude/longitude. See :ref:`axis_ordering` below for more 
  details.
* In the GetMap operation the ``srs`` parameter is called ``crs`` in 1.3.0. 
  GeoServer supports both keys regardless of version.
* In the GetFeatureInfo operation the ``x`` and ``y`` parameters are
  called ``i`` and ``j`` in 1.3.0. 
  GeoServer supports both keys regardless of version, 
  except when in CITE-compliance mode.

.. _axis_ordering:

Axis Ordering
-------------

The WMS 1.3.0 specification mandates that the axis ordering for geographic 
coordinate systems defined in the EPSG database be *latitude/longitude*, or *y/x*. 
This is contrary to the fact that most spatial data is usually in 
*longitude/latitude*, or *x/y*. 
This requires that the coordinate order in the ``BBOX`` parameter
be reversed for ``SRS`` values which are geographic coordinate systems.

For example, consider the WMS 1.1 request using the WGS84 SRS (EPSG:4326):: 

   geoserver/wms?VERSION=1.1.1&REQUEST=GetMap&SRS=epsg:4326&BBOX=-180,-90,180,90&...

The equivalent WMS 1.3.0 request is::

   geoserver/wms?VERSION=1.3.0&REQUEST=GetMap&CRS=epsg:4326&BBOX=-90,-180,90,180&...

Note that the coordinates specified in the ``BBOX`` parameter are reversed.
