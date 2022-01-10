.. _community_solr:

SOLR data store
===============

`SOLR <http://lucene.apache.org/solr/>`_ is a popular search platform based on Apache Lucene project. 
Its major features include powerful full-text search, hit highlighting, faceted search, near real-time indexing, 
dynamic clustering, database integration, rich document (e.g., Word, PDF) handling, and most
importantly for the GeoServer integration, geospatial search.

The latest versions of SOLR can host most basic types of geometries (points, lines and polygons)
as WKT and index them with a spatial index.

.. note:: GeoServer does not come built-in with support for SOLR; it must be installed through this community module. 

The GeoServer SOLR extension has been tested with SOLR version 4.8, 4.9, and 4.10.

The extension supports all WKT geometry types (all linear types, point, lines and polygons, SQL/MMcurves are not supported), 
plus "bounding box" (available starting SOLR 4.10).
It does not support the ``solr.LatLonType`` type yet.

The following pages shows how to use the SOLR data store.

.. toctree::
   :maxdepth: 2

   configure
   load
   optimize
