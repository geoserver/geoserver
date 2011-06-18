.. _filter_syntax:

GeoServer supported filter languages
====================================

Data filtering in GeoServer is based on the concepts found in the `OGC Filter Encoding Specification <http://www.opengeospatial.org/standards/filter>`_, which we suggest the reader to get familiar with.

In particular GeoServer accepts filters encoded in three different languages:
  
- `OGC Filter encoding specification v 1.0 <http://portal.opengeospatial.org/files/?artifact_id=1171>`_, used in WFS 1.0 and SLD 1.0
- `OGC Filter encoding specification v 1.1 <http://portal.opengeospatial.org/files/?artifact_id=8340>`_, used in WFS 1.1
- `CQL, Catalog Query Language <http://portal.opengeospatial.org/files/?artifact_id=3843>`_, a plain text language created for the OGC Catalog specification and adapted to be a general and easy to use filtering mechanism. 
- `ECQL, Extended CQL <http://docs.codehaus.org/display/GEOTOOLS/ECQL+Parser+Design>`_, an extension to CQL that allows to express the same filters OGC Filter 1.1 can encode. A quick :ref:`cql_tutorial` is also available in this guide that shows examples of both CQL and ECQL.

We suggest to look into the respective specifications for details.

