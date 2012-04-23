.. _filter_syntax:

Supported filter languages
====================================

Data filtering in GeoServer is based on the concepts found in the `OGC Filter Encoding Specification <http://www.opengeospatial.org/standards/filter>`_.

GeoServer accepts filters encoded in two different languages: *Filter Encoding* and *Common Query Language*.  
Each of these has variations, described in the following specifications:

**Filter Encoding**
  
- `OGC Filter encoding specification v 1.0 <http://portal.opengeospatial.org/files/?artifact_id=1171>`_, used in WFS 1.0 and SLD 1.0
- `OGC Filter encoding specification v 1.1 <http://portal.opengeospatial.org/files/?artifact_id=8340>`_, used in WFS 1.1


**CQL/ECQL**

- `CQL, Common Query Language <http://portal.opengeospatial.org/files/?artifact_id=3843>`_, a plain-text language created for the OGC Catalog specification and adapted to be a general and easy-to-use filtering mechanism. 
- `ECQL, Extended CQL <http://docs.codehaus.org/display/GEOTOOLS/ECQL+Parser+Design>`_, an extension to CQL that allows expressing the same filters OGC Filter 1.1 can encode. The  :ref:`cql_tutorial` tutorial in this guide shows examples of both CQL and ECQL.

Refer to these specifications for precise details of the languages.

