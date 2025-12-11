.. _wfs_basics:

WFS basics
==========

GeoServer provides support for the `Open Geospatial Consortium (OGC) <https://www.ogc.org/>`_ `Web Feature Service (WFS) <https://www.ogc.org/standards/wfs/>`_ specification, versions **1.0.0**, **1.1.0**, and **2.0.0**. WFS defines a standard for exchanging vector data over the Internet. With a compliant WFS, clients can query both the data structure and the source data. Advanced WFS operations also support feature locking and edit operations.  

GeoServer is the reference implementation of all three versions of the standard, completely implementing every part of the protocol. This includes the basic operations of :ref:`wfs_getcap`, :ref:`wfs_dft`, and :ref:`wfs_getfeature`, as well as more advanced options such as :ref:`wfs_wfst`. GeoServer WFS is also integrated with its :ref:`security` system to limit access to data and transactions, and supports a variety of :ref:`wfs_output_formats`, making the raw data more widely available.

Layer and attribute naming considerations
-----------------------------------------

When fetching WFS data as GML, the XML encoder follows the `W3C Recommendation for XML <https://www.w3.org/TR/REC-xml/#charsets>`_ which implements specific requirements for the characters which may be passed. When data is to be made available via WFS, it is important that the Layer and Attribute names contain only permitted characters.

GeoServer does not prohibit or warn when creating layer or attribute names containing these characters, as for other services such as WMS, WCS, or OGC Features API these may be present.

Differences between WFS versions
--------------------------------

The major differences between the WFS versions are: 

* WFS 1.1.0 and 2.0.0 return GML3 as the default GML, whereas in WFS 1.0.0, the default is GML2. GML3 adopts marginally different ways of specifying a geometry. GeoServer supports requests in both GML3 and GML2 formats.

* In WFS 1.1.0 and 2.0.0, the SRS (Spatial Reference System, or projection) is specified with ``urn:x-ogc:def:crs:EPSG:XXXX``, whereas in WFS 1.0.0 the specification was ``http://www.opengis.net/gml/srs/epsg.xml#XXXX``. This change has implications for the :ref:`axis order <wfs_basics_axis>` of the returned data. 

* WFS 1.1.0 and 2.0.0 support on-the-fly reprojection of data, which supports returning the data in a SRS other than the native SRS. 

* WFS 2.0.0 introduces a new version of the filter encoding specification, adding support for temporal filters.

* WFS 2.0.0 supports joins via a GetFeature request.

* WFS 2.0.0 adds the ability to page results of a GetFeature request via the ``startIndex`` and ``count`` parameters. GeoServer now supports this functionality in WFS 1.0.0 and 1.1.0. 

* WFS 2.0.0 supports stored queries, which are regular WFS queries stored on the server such that they may be invoked by passing the appropriate identifier with a WFS request.

* WFS 2.0.0 supports SOAP (Simple Object Access Protocol) as an alternative to the OGC interface.

.. note:: There are also two changes to parameter names which can cause confusion. WFS 2.0.0 uses the ``count`` parameter to limit the number of features returned rather than the ``maxFeatures`` parameter used in previous versions. It also changed ``typeName`` to ``typeNames`` although GeoServer will accept either.

