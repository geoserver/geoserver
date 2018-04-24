.. _opensearch_eo_intro:

Introduction to OpenSearch for EO
=================================

This plugin adds support for the OpenSearch for Earth Observation protocol to
GeoServer. References:

* `OpenSearch <http://www.opensearch.org>`_
* `OpenSearch parameter extension <http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0>`_
* `OpenSearch Geo and Time extension <http://www.opengeospatial.org/standards/opensearchgeo>`_
* `OpenSearch for Earth Observation <http://docs.opengeospatial.org/is/13-026r8/13-026r8.html>`_

The OpenSearch plugin organizes data in "Collections" and "Products":

* A collection is a set of products with some uniformity, described by some search attributes and a ISO metadata sheet
* A product is a set of images (and ancilliary information), describe by some search attributes and a O&M metadata sheet

The system allows the common EO "two level" searches, that is:

* Firsts lookup for the desired collection of data on the main OSDD document
* Once the collection is located, a second OSDD providing access to the product search is delivered

If the database contains also the OGC cross links, the Atom search outputs will also contain
links allowing a client to jump from the data search to the actual data visualization and exploitation
on OGC services.

Search engine storage
---------------------

The OpenSearch protocol implementation relies on an extension of GeoTools ``DataAccess`` called ``OpenSearchAccess``.
At the time of writing a single implementation of such interface exists, called ``JDBCOpenSearchAccess``,
built and tested to work against a specific PostGIS database schema.

.. note:: The ``JDBCOpenSearchAccess`` is written in general enough terms that other databases should be usable as well, but it's likely some code improvements will be required to deal with certain databases naming restrictions (e.g., Oracle).

In the future we hope to see other implementations as well, based on storage that
might be more suitable for large scale search engine such as SOLR or ElasticSearch.
