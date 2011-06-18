.. _wms_basics:

WMS basics
==========

GeoServer provides support for Open Geospatial Consortium (OGC) Web Map Service (WMS) versions 1.1.1 and 1.3.0.  This is a standard for generating maps on the web - it is how all the visual mapping that GeoServer does is produced.  Using a compliant WMS makes it possible for clients to overlay maps from several different sources in a seamless way.

GeoServer's implementation fully supports most every part of the standard, and is certified compliant against the OGC's test suite.  It includes a wide variety of rendering and labeling options, and is one of the fastest WMS Servers for both raster and vector data.  

The WMS implementation of GeoServer also supports reprojection in to any reference system in the EPSG database, and it is also possible to add additional projections if the Well Known Text is known.  It also fully supports the Styled Layer Descriptor (SLD) standard, and indeed uses SLD files as its native rendering rules.  For more information on how to style GeoServer data in the WMS see the section :ref:`styling`

Differences between WMS versions
--------------------------------

The major differences between versions 1.1.1 and 1.3.0 are:

* In 1.1.1 geographic coordinate systems specified with the ``EPSG`` namespace 
  are defined to have an axis ordering of longitude/latitude. In 1.3.0 the 
  ordering is latitude/longitude. See :ref:`axis_ordering` below for more 
  details.
* In the GetMap operation the ``srs`` parameter from 1.1.1 is now ``crs`` in 
  1.3.0. Although GeoServer supports both regardless of version.
* In the GetFeatureInfo operation the ``x``, ``y`` parameters from 1.1.1 are
  now ``i``, ``j`` in 1.3.0. Although GeoServer will support ``x``, ``y`` in 
  1.3.0 when running on non cite compliance mode.

.. _axis_ordering:

Axis Ordering
-------------

The WMS 1.3 specification has mandated that the axis ordering for geographic 
coordinate systems defined in the EPSG database be latitude/longitude, or y/x. 
This is contrary to the fact that most spatial data is usually in 
longitude/latitude, or x/y. For example, consider the WMS 1.1 request:: 

   geoserver/wms?VERSION=1.1.1&REQUEST=GetMap&SRS=epsg:4326&BBOX=-180,-90.180,90&...

The equivalent WMS 1.3 request would be::

   geoserver/wms?VERSION=1.1.1&REQUEST=GetMap&CRS=epsg:4326&BBOX=-90,-180,90,180&...

The coordinates specified by the BBOX parameter are essentially flipped.
