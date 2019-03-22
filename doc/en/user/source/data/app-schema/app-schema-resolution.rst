.. _app-schema.app-schema-resolution:

Application Schema Resolution
=============================

To be able to encode XML responses conforming to a GML application schema, the app-schema plugin must be able to locate the application schema files (XSDs) that define the schema. This page describes the schema resolution process.


Schema downloading is now automatic for most users
--------------------------------------------------

GeoServer will automatically download and cache (see `Cache`_ below) all the schemas it needs the first time it starts if:

#. All the application schemas you use are accessed via http/https URLs, and
#. Your GeoServer instance is deployed on a network that permits it to download them.

.. note:: This is the recommended way of using GeoServer app-schema for most users.

If cached downloading is used, no manual handling of schemas will be required. The rest of this page is for those with more complicated arrangements, or who wish to clear the cache.


Resolution order
----------------

The order of sources used to resolve application schemas is:

#. `OASIS Catalog`_
#. `Classpath`_
#. `Cache`_

Every attempt to load a schema works down this list, so imports can be resolved from sources other than that used for the originating document. For example, an application schema in the cache that references a schema found in the catalog will use the version in the catalog, rather than caching it. This allows users to supply unpublished or modified schemas sourced from, for example, the catalog, at the cost of interoperability (how do WFS clients get them?).


OASIS Catalog
-------------

An `OASIS XML Catalog <http://www.oasis-open.org/committees/entity/spec-2001-08-06.html>`_ is a standard configuration file format that instructs an XML processing system how to process entity references. The GeoServer app-schema resolver uses catalog URI semantics to locate application schemas, so ``uri`` or ``rewriteURI`` entries should be present in your catalog. The optional mapping file  ``catalog`` element provides the location of the OASIS XML Catalog configuration file, given as a path relative to the mapping file, for example::

    <catalog>../../../schemas/catalog.xml</catalog>

Earlier versions of the app-schema plugin required all schemas to be present in the catalog. This is no longer the case. Because the catalog is searched first, existing catalog-based deployments will continue to work as before.

To migrate an existing GeoServer app-schema deployment that uses an OASIS Catalog to instead use cached downloads (see `Cache`_ below), remove all ``catalog`` elements from your mapping files and restart GeoServer.


Classpath
---------

Java applications such as GeoServer can load resources from the Java classpath. GeoServer app-schema uses a simple mapping from an http or https URL to a classpath resource location. For example, an application schema published at ``http://schemas.example.org/exampleml/exml.xsd`` would be found on the classpath if it was stored either:

* at ``/org/example/schemas/exampleml/exml.xsd`` in a JAR file on the classpath (for example, a JAR file in ``WEB-INF/lib``) or,
* on the local filesystem at ``WEB-INF/classes/org/example/schemas/exampleml/exml.xsd`` .

The ability to load schemas from the classpath is intended to support testing, but may be useful to users whose communities supply JAR files containing their application schemas.

.. _app-schema-cache:

Cache
-----

If an application schema cannot be found in the catalog or on the classpath, it is downloaded from the network and stored in a subdirectory ``app-schema-cache`` of the GeoServer data directory.

* Once schemas are downloaded into the cache, they persist indefinitely, including over GeoServer restarts.
* No attempt will be made to retrieve new versions of cached schemas.
* To clear the cache, remove the  subdirectory ``app-schema-cache`` of the GeoServer data directory and restart GeoServer.

GeoServer app-schema uses a simple mapping from an http or https URL to local filesystem path. For example, an application schema published at ``http://schemas.example.org/exampleml/exml.xsd`` would be downloaded and stored as ``app-schema-cache/org/example/schemas/exampleml/exml.xsd`` . Note that:

* Only ``http`` and ``https`` URLs are supported.
* Port numbers, queries, and fragments are ignored.

If your GeoServer instance is deployed on a network whose firewall rules prevent outgoing TCP connections on port 80 (http) or 443 (https), schema downloading will not work. (For security reasons, some service networks ["demilitarised zones"] prohibit such outgoing connections.) If schema downloading is not permitted on your network, there are three solutions:

#. Either: Install and configure GeoServer on another network that can make outgoing TCP connections, start GeoServer to trigger schema download, and then manually copy the ``app-schema-cache`` directory to the production server. This is the easiest option because GeoServer automatically downloads all the schemas it needs, including dependencies.
#. Or: Deploy JAR files containing all required schema files on the classpath (see `Classpath`_ above).
#. Or: Use a catalog (see `OASIS Catalog`_ above).

.. warning:: System property "schema.cache.dir" with a cache directory location is required for using a mapping file from a remote URL with 'http://' or 'https://' protocol.
