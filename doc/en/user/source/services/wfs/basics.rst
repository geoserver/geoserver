.. _wfs_basics:

WFS basics
==========

GeoServer provides support for Open Geospatial Consortium (OGC) Web Feature Service (WFS) versions 1.0 and 1.1.  This is a standard for getting raw vector data - the 'source code' of the map - over the web.  Using a compliant WFS makes it possible for clients to query the data structure and the actual data.  Advanced WFS operations also enable editing and locking of the data.  

GeoServer is the reference implementation of both the 1.0 and 1.1 versions of the standard, completely implementing every part of the protocol.  This includes the Basic operations of GetCapabilities, DescribeFeatureType and GetFeature, as well as the more advanced Transaction, LockFeature and GetGmlObject operations.  GeoServer's WFS also is integrated with GeoServer's :ref:`security` system, to limit access to data and transactions.  It also supports a wide variety of :ref:`wfs_output_formats`, to make the raw data more widely available.  

GeoServer additionally supports a special 'versioning' protocol in an extension: :ref:`wfsv_extension`.  This is not yet a part of the WFS specification, but is written to be compatible, extending it to provide a history of edits, differences between edits, and a rollback operation to take things to a previous state.  


:ref:`wfs_reference`

Differences between WFS versions
-------------------------------- 

The major differences between the WFS versions are: 

* WFS 1.1.0 returns GML3 as the default GML. In WFS 1.0.0 the default was GML2. (GeoServer still supports requests in both GML3 and GML2 formats.) GML3 has slightly different ways of specifying a geometry. 
* In WFS 1.1.0, the way to specify the SRS (Spatial Reference System, aka projection) is ``urn:x-ogc:def:crs:EPSG:XXXX``, whereas in version 1.0.0 the specification was ``http://www.opengis.net/gml/srs/epsg.xml#XXXX``. This change has implications on the axis order of the returned data. 
* WFS 1.1.0 supports on-the-fly reprojection of data, which allows for data to be returned in a SRS other than the native. 

Axis ordering
------------- 

WFS 1.0.0 servers return geographic coordinates in longitude/latitude 
(x/y) order. This is the most common way to distribute data as well (for 
example, most shapefiles adopt this order by default). 

However, the traditional axis order for geographic and cartographic 
systems is latitude/longitude (y/x), the opposite and WFS 1.1.0 
specification respects this. This can cause difficulties when switching 
between servers with different WFS versions, or when upgading your WFS. 

To sum up, the defaults are as follows: 

* WFS 1.1.0 request = latitude/longitude
* WMS 1.0.0 request = longitude/latitude 

GeoServer, however, in an attempt to minimize confusion and increase 
interoperability, has adopted the following conventions when specifying 
projections in the follow formats: 

* ``EPSG:xxxx`` - longitude/latitude
* ``http://www.opengis.net/gml/srs/epsg.xml#xxxx`` - longitude/latitude
* ``urn:x-ogc:def:crs:EPSG:xxxx`` - latitude/longitude 
