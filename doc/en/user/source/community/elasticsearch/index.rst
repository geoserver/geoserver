Elasticsearch Data Store
========================

Elasticsearch is a popular distributed search and analytics engine that enables complex search features in near real-time. Default field type mappings support string, numeric, boolean and date types and allow complex, hierarchical documents. Custom field type mappings can be defined for geospatial document fields. The ``geo_point`` type supports point geometries that can be specified through a coordinate string, geohash or coordinate array. The ``geo_shape`` type supports Point, LineString,  Polygon, MultiPoint, MultiLineString, MultiPolygon and GeometryCollection GeoJSON types as well as envelope and circle types. Custom options allow configuration of the type and precision of the spatial index.

This data store allows features from an Elasticsearch index to be published through GeoServer. Both ``geo_point`` and ``geo_shape`` type mappings are supported. OGC filters are converted to Elasticsearch filters and can be combined with native Elasticsearch queries and filters in WMS and WFS requests. 

Compatibility
-------------

* Java JDK (>=1.7)
* GeoServer: 2.9.x
* Elasticsearch: 1.4.x, 1.5.x, 1.7.x, 2.1.x, 2.2.x

Downloads
---------

Pre-compiled binaries for supported GeoServer and Elasticsearch versions can be found on the GitHub releases page. 

https://github.com/ngageoint/elasticgeo/releases

Installation
------------

*Warning: Ensure GeoTools/GeoServer and Elasticsearch versions in the plugin configuration are consistent with your environment* 


Pre-compiled Binaries::

Unpack zipfile and copy plugin file(s) to the ``WEB_INF/lib`` directory of your GeoServer installation and then restart Geoserver. If installing the plugin for Elasticsearch 2.x, remove the old Guava jar (e.g. ``guava-17.0.jar``).


Building from Source::

Build and install a local copy. By default the plugin will be compatible with Elasticsearch 2.x. For compatibility with Elasticsearch 1.x, include the ``elasticsearch1`` Maven profile when building::

    $ git clone git@github.com:ngageoint/elasticgeo.git
    $ cd elasticgeo
    $ mvn clean install [-Pelasticsearch1]
    $ cd gs-web-elasticsearch
    $ mvn package -P deploy[,elasticsearch1]

Copy the ElasticGeo GeoServer plugin to the ``WEB_INF/lib`` directory of your GeoServer installation and then restart Geoserver.::

    $ cp target/elasticgeo--gs--es--geoserver.jar GEOSERVER_HOME/WEB_INF/lib

If installing the plugin for Elasticsearch 2.x, replace the Guava library in the GeoServer installation with Guava 18.0 or later.::

    $ rm GEOSERVER_HOME/WEB_INF/lib/guava*.jar
    $ cp target/lib/guava*.jar GEOSERVER_HOME/WEB_INF/lib

Creating an Elasticsearch data store
------------------------------------

Once the Elasticsearch GeoServer extension is installed, ``Elasticsearch index`` will be an available vector data source format when creating a new data store.

.. figure:: images/elasticsearch_store.png
   :align: center

.. _config_elasticsearch:

Configuring an Elasticsearch data store
---------------------------------------

The Elasticsearch data store configuration panel includes standard connection parameters and search settings.

.. figure:: images/elasticsearch_configuration.png
   :align: center

.. list-table::
   :widths: 20 80

   * - ``elasticsearch_host``
     - Host (IP) for connecting to Elasticsearch
   * - ``elasticsearch_port``
     - Port for connecting to Elasticsearch
   * - ``index_name``
     - Index name
   * - ``search_indices``
     - Indices to use when searching. Enables multi/cross index searches.
   * - ``cluster_name``
     - Cluster name
   * - ``use_local_node``
     - Whether to use the node client or transport client to connect to Elasticsearch
   * - ``store_data``
     - Whether to store data in the local node, if relevant
   * - ``scroll_enabled``
     - Enable the Elasticsearch scan and scroll API
   * - ``scroll_size``
     - Number of documents per shard when using the scroll API
   * - ``scroll_time``
     - Search context timeout when using the scroll API


Configuring an Elasticsearch layer
----------------------------------------

The initial layer configuration panel for an Elasticsearch layer will include an additional pop-up showing a table of available fields.

.. figure:: images/elasticsearch_fieldlist.png
   :align: center

.. list-table::
   :widths: 20 80

   * - ``Use All``
     - Use all fields in the layer feature type
   * - ``Short Names``
     - For hierarchical documents with inner fields (e.g. ``parent.child.field_name``), only use the base name 
       (``field_name``) in the schema. Note, full path will always be included when the base name is duplicated across fields.
   * - ``Use``
     - Used to select the fields that will make up the layer feature type
   * - ``Name``
     - Name of the field
   * - ``Type``
     - Type of the field, as derived from the Elasticsearch schema. For geometry types, you have the option to provide a more specific data type.
   * - ``Default Geometry``
     - Indicates if the geometry field is the default one. Useful if the documents contain more than one geometry field, as SLDs and spatial filters will hit the default geometry field unless otherwise specified
   * - ``Stored``
     - Indicates whether the field is stored in the index
   * - ``Analyzed``
     - Indicates whether the field is analyzed
   * - ``SRID``
     - Native spatial reference ID of the geometries. Currently only EPSG:4326 is supported.
   * - ``Date Format``
     - Date format used for parsing field values and printing filter elements

To return to the field table after it has been closed, click the "Configure Elasticsearch fields" button below the "Feature Type Details" panel on the layer configuration page.

.. figure:: images/elasticsearch_fieldlist_edit.png
   :align: center

Usage
---------

Filtering
^^^^^^^^^

Filtering capabilities include OpenGIS simple comparisons, temporal comparisons, as well as other common filter comparisons. Elasticsearch natively supports numerous spatial filter operators, depending on the type:

- ``geo_shape`` types natively support BBOX/Intersects, Within and Disjoint binary spatial operators
- ``geo_point`` types natively support BBOX and Within binary spatial operators, as well as the DWithin and Beyond distance buffer operators

Requests involving spatial filter operators not natively supported by Elasticsearch will include an additional filtering operation on the results returned from the query, which may impact performance.


Custom ``q`` and ``f`` parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Native Elasticsearch queries and filters can be included in WFS/WMS feature requests using the custom ``q`` (query) and ``f`` (filter) parameters through the ``viewparams`` parameter (see GeoServer SQL Views documentation for more information). If supplied, the filter is combined with the filter derived from the request bbox, CQL or OGC filter using the AND logical binary operator.

Examples
^^^^^^^^

BBOX and CQL filter::

    http://localhost:8080/geoserver/test/wms?service=WMS&version=1.1.0&request=GetMap
         &layers=test:active&styles=&bbox=-1,-1,10,10&width=279&height=512
         &srs=EPSG:4326&format=application/openlayers&maxFeatures=1000
         &cql_filter=standard_ss='IEEE 802.11b'

BBOX and native filter::

    http://localhost:8080/geoserver/test/wms?service=WMS&version=1.1.0&request=GetMap
         &layers=test:active&styles=&bbox=-1,-1,10,10&width=279&height=512
         &srs=EPSG:4326&format=application/openlayers&maxFeatures=1000
         &viewparams=f:{"term":{"standard_ss":"IEEE 802.11b"}}

Native query with BBOX filter::

    http://localhost:8080/geoserver/test/wms?service=WMS&version=1.1.0&request=GetMap
         &layers=test:active&styles=&bbox=-1,-1,10,10&width=279&height=512
         &srs=EPSG:4326&format=application/openlayers&maxFeatures=1000
         &viewparams=q:{"term":{"standard_ss":"IEEE 802.11b"}}

Note that commas in native query and filter must be escaped with a backslash.

Notes and Known Issues
----------------------

- ``PropertyIsEqualTo`` maps to an Elasticsearch term filter, which will return documents that contain the supplied term. When searching on an analyzed string field, ensure that the search values are consistent with the analyzer used in the index. For example, values may need to be lowercase when querying fields analyzed with the default analyzer. See the Elasticsearch term filter documentation for more information.
- ``PropertyIsLike`` maps to either a query string query filter or a regexp filter, depending on whether the field is analyzed or not. Reserved characters should be escaped as applicable. Note case sensitive and insensitive searches may not be supported for analyzed and not analyzed fields, respectively. See Elasticsearch query string and regexp filter documentation for more information.
- Date conversions are handled using the date format from the associated type mapping, or ``date_optional_time`` if not found. Note that UTC timezone is used for both parsing and printing of dates.
- Filtering on Elasticsearch ``object`` types is supported. By default, field names will include the full path to the field (e.g. "parent.child.field_name"), but this can be changed in the GeoServer layer configuration.

  - When referencing fields with path elements using ``cql_filter``, it may be necessary to quote the name (e.g. ``cql_filter="parent.child.field_name"='value'``)

- Filtering on Elasticsearch ``nested`` types is supported only for non-geospatial fields.
- Circle geometries are not currently supported
