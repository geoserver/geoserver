.. _filter_syntax:

Supported filter languages
====================================

Data filtering in GeoServer is based on the concepts found in the `OGC Filter Encoding Specification <http://www.opengeospatial.org/standards/filter>`_.

GeoServer accepts filters encoded in two different languages: *Filter Encoding* and *Common Query Language*.  

Filter Encoding
---------------

The **Filter Encoding** language is an XML-based method for defining filters.
XML Filters can be used in the following places in GeoServer:

- in WMS ``GetMap`` requests, using the ``filter`` parameter
- in WFS ``GetFeature`` requests, using the ``filter`` parameter
- in SLD :ref:`Rule <sld_intro>` elements

The Filter Encoding language is defined in the following OGC specifications:

- `OGC Filter encoding specification v 1.0 <http://portal.opengeospatial.org/files/?artifact_id=1171>`_, used in WFS 1.0 and SLD 1.0
- `OGC Filter encoding specification v 1.1 <http://portal.opengeospatial.org/files/?artifact_id=8340>`_, used in WFS 1.1


CQL/ECQL
--------

**CQL** a plain-text language created for the *OGC Catalog* specification and adapted to be a general and easy-to-use filtering mechanism.
GeoServer implements a more powerful extension called **ECQL (Extended CQL)**.
ECQL allows expressing the same filters OGC Filter 1.1 can encode. 
ECQL is a superset of CQL, and is accepted anywhere in GeoServer where CQL is allowed:

- in WMS ``GetMap`` requests, using the :ref:`cql_filter <wms_vendor_parameters>` parameter 
- in WFS ``GetFeature`` requests, using the :ref:`cql_filter <wfs_vendor_parameters>` parameter
- in SLD :ref:`dynamic symbolizers <pointsymbols>`

The :ref:`filter_ecql_reference` describes the features of the ECQL language.
The  :ref:`cql_tutorial` tutorial shows examples of defining filters.

The CQL and ECQL languages are defined in:

- `OpenGIS Catalog Services Specification <http://portal.opengeospatial.org/files/?artifact_id=3843>`_ contains the standard definition of CQL 
- `ECQL Grammar <http://docs.codehaus.org/display/GEOTOOLS/ECQL+Parser+Design>`_ is the grammar defining the GeoTools ECQL implementation







