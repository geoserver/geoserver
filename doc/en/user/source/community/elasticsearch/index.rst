Elasticsearch GeoServer Data Store
==================================

Elasticsearch is a popular distributed search and analytics engine that enables complex search features in near real-time. Default field type mappings support string, numeric, boolean and date types and allow complex, hierarchical documents. Custom field type mappings can be defined for geospatial document fields. The ``geo_point`` type supports point geometries that can be specified through a coordinate string, geohash or coordinate array. The ``geo_shape`` type supports Point, LineString,  Polygon, MultiPoint, MultiLineString, MultiPolygon and GeometryCollection GeoJSON types as well as envelope and circle types. Custom options allow configuration of the type and precision of the spatial index.

This data store allows features from an Elasticsearch index to be published through GeoServer. Both ``geo_point`` and ``geo_shape`` type mappings are supported. OGC filters are converted to Elasticsearch queries and can be combined with native Elasticsearch queries in WMS and WFS requests. 

.. contents:: Contents:

Compatibility
-------------

* Java JDK (>=1.8)
* GeoServer: 2.9.x, 2.10.x
* Elasticsearch: >=2.1.x (supports transport client), >=5.1.x (supports transport and REST clients)

Downloads
---------

Pre-compiled binaries for supported GeoServer and Elasticsearch versions can be found on the GitHub releases page. 

https://github.com/ngageoint/elasticgeo/releases

Installation
------------

**Warning: Ensure GeoTools and GeoServer versions in the plugin configuration are consistent with your environment. If using the transport client then also ensure Elasticsearch version is consistent with the server.**

Plugins built for Elasticsearch 5.x should be compatible with Elasticsearch 2.x servers when the default REST client is used. If any compatibility issues are observed using the REST client please create an issue.

Pre-compiled binaries
^^^^^^^^^^^^^^^^^^^^^

Unpack zipfile and copy plugin file(s) to the ``WEB_INF/lib`` directory of your GeoServer installation and then restart GeoServer. If installing the plugin for Elasticsearch 2.x, remove the old Guava jar (e.g. ``guava-17.0.jar``).

Building from source
^^^^^^^^^^^^^^^^^^^^

Build and install a local copy. By default the plugin will be compatible with Elasticsearch 5.x (and 2.x via the REST client). For compatibility with Elasticsearch 2.x (via the transport client), include the ``elasticsearch2`` Maven profile when building::

    $ git clone git@github.com:ngageoint/elasticgeo.git
    $ cd elasticgeo
    $ mvn clean install [-Pelasticsearch2]

Copy the ElasticGeo GeoServer plugin to the ``WEB_INF/lib`` directory of your GeoServer installation and then restart GeoServer::

    $ cp gs-web-elasticsearch/target/elasticgeo*.jar GEOSERVER_HOME/WEB_INF/lib

If installing the plugin for Elasticsearch 2.x, replace the Guava library in the GeoServer installation with Guava 18.0 or later::

    $ rm GEOSERVER_HOME/WEB_INF/lib/guava*.jar
    $ cp gs-web-elasticsearch/target/lib/guava-18.0.jar GEOSERVER_HOME/WEB_INF/lib

Configuration
-------------

Configuring data store
^^^^^^^^^^^^^^^^^^^^^^

Once the Elasticsearch GeoServer extension is installed, ``Elasticsearch index`` will be an available vector data source format when creating a new data store.

.. figure:: images/elasticsearch_store.png
   :align: center

.. _config_elasticsearch:

The Elasticsearch data store configuration panel includes standard connection parameters and search settings.

.. figure:: images/elasticsearch_configuration.png
   :align: center

.. list-table::
   :widths: 20 80

   * - Parameter
     - Description
   * - elasticsearch_host
     - Host (IP) for connecting to Elasticsearch
   * - elasticsearch_port
     - Port for connecting to Elasticsearch. When plugin is built for Elasticsearch 5.x use the HTTP port (e.g. 9200) for the REST client. Otherwise use the transport port (e.g. 9300).
   * - index_name
     - Index name
   * - search_indices
     - Indices to use when searching. Enables multi/cross index searches.
   * - cluster_name
     - Cluster name
   * - default_max_features
     - Default used when maxFeatures is unlimited
   * - source_filtering_enabled
     - Whether to enable filtering of the _source field
   * - scroll_enabled
     - Enable the Elasticsearch scan and scroll API
   * - scroll_size
     - Number of documents per shard when using the scroll API
   * - scroll_time
     - Search context timeout when using the scroll API
   * - grid_size 
     - Hint for Geohash grid size (numRows*numCols)
   * - grid_threshold
     - Geohash grid aggregation precision will be the minimum necessary so that actual_grid_size/grid_size > grid_threshold


Configuring layer
^^^^^^^^^^^^^^^^^

The initial layer configuration panel for an Elasticsearch layer will include an additional pop-up showing a table of available fields.

.. figure:: images/elasticsearch_fieldlist.png
   :align: center

.. list-table::
   :widths: 20 80

   * - Column
     - Description
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

Configuring logging
^^^^^^^^^^^^^^^^^^^

Logging is configurable through Log4j. The data store includes logging such as the query object being sent to Elasticsearch, which is logged at a lower level than may be enabled by default. To enable these logs, add the following lines to the GeoServer logging configuration file (see GeoServer Global Settings)::

    log4j.category.mil.nga.giat.data.elasticsearch=DEBUG 
    log4j.category.mil.nga.giat.process.elasticsearch=DEBUG 

Filtering
---------

Filtering capabilities include OpenGIS simple comparisons, temporal comparisons, as well as other common filter comparisons. Elasticsearch natively supports numerous spatial filter operators, depending on the type:

- ``geo_shape`` types natively support BBOX/Intersects, Within and Disjoint binary spatial operators
- ``geo_point`` types natively support BBOX and Within binary spatial operators, as well as the DWithin and Beyond distance buffer operators

Requests involving spatial filter operators not natively supported by Elasticsearch will include an additional filtering operation on the results returned from the query, which may impact performance.


Native queries
^^^^^^^^^^^^^^

Native Elasticsearch queries can be applied in WFS/WMS feature requests by including the ``q:{query_body}`` or ``f:{query_body}`` key:value pairs in the ``viewparams`` parameter (see GeoServer SQL Views documentation for more information). If supplied, the query is combined with the query derived from the request bbox, CQL or OGC filter using the AND logical binary operator.

Examples
^^^^^^^^

BBOX and CQL filter::

    http://localhost:8080/geoserver/test/wms?service=WMS&version=1.1.0&request=GetMap
         &layers=test:active&styles=&bbox=-1,-1,10,10&width=279&height=512
         &srs=EPSG:4326&format=application/openlayers&maxFeatures=1000
         &cql_filter=standard_ss='IEEE 802.11b'

BBOX and native query::

    http://localhost:8080/geoserver/test/wms?service=WMS&version=1.1.0&request=GetMap
         &layers=test:active&styles=&bbox=-1,-1,10,10&width=279&height=512
         &srs=EPSG:4326&format=application/openlayers&maxFeatures=1000
         &viewparams=f:{"term":{"standard_ss":"IEEE 802.11b"}}

Native query with BBOX filter::

    http://localhost:8080/geoserver/test/wms?service=WMS&version=1.1.0&request=GetMap
         &layers=test:active&styles=&bbox=-1,-1,10,10&width=279&height=512
         &srs=EPSG:4326&format=application/openlayers&maxFeatures=1000
         &viewparams=q:{"term":{"standard_ss":"IEEE 802.11b"}}

Note that commas in native queries must be escaped with a backslash.

Aggregations
------------

**Currently supported only when using the REST client with Elasticsearch 5.x**

Elasticsearch aggregations are supported through WFS/WMS requests by including the ``a:{aggregation_body}`` key:value pair in the ``viewparams`` parameter (see GeoServer SQL Views documentation for more information)::

    http://localhost:8080/geoserver/test/ows?service=WFS&version=1.0.0&request=GetFeature
         &typeName=test:active&bbox=0.0,0.0,24.0,44.0
         &viewparams=a:{"agg": {"geohash_grid": {"field": "geo"\, "precision": 3}}}

Aggregation WFS features will include a single attribute, ``_aggregation``, containing the raw aggregation content. Note that size is set to zero when an aggregation is supplied so only aggregation features are returned (e.g. maxFeatures is ignored and there will be no search hit results).

Geohash grid aggregations
^^^^^^^^^^^^^^^^^^^^^^^^^

Geohash grid aggregation support includes dynamic precision updating and a custom rendering transformation for visualization. Geohash grid aggregation precision is updated dynamically to approximate the specified ``grid_size`` based on current bbox extent and the additional ``grid_threshold`` parameter as described above.

Geohash grid aggregation visualization is supported in WMS requests through a custom rendering transformation, ``vec:GeoHashGrid``, which translates aggregation response data into a raster for display. By default raster values correspond to the aggregation bucket ``doc_count``. The following shows an example GeoServer style that uses the GeoHashGrid rendering transformation::

   <StyledLayerDescriptor version="1.0.0"
       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
       xmlns="http://www.opengis.net/sld"
       xmlns:ogc="http://www.opengis.net/ogc"
       xmlns:xlink="http://www.w3.org/1999/xlink"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     <NamedLayer>
       <Name>GeoHashGrid</Name>
       <UserStyle>
         <Title>GeoHashGrid</Title>
         <Abstract>GeoHashGrid aggregation</Abstract>
         <FeatureTypeStyle>
           <Transformation>
             <ogc:Function name="vec:GeoHashGrid">
               <ogc:Function name="parameter">
                 <ogc:Literal>data</ogc:Literal>
               </ogc:Function>
               <ogc:Function name="parameter">
                 <ogc:Literal>gridStrategy</ogc:Literal>
                 <ogc:Literal>Basic</ogc:Literal>
               </ogc:Function>
               <ogc:Function name="parameter">
                 <ogc:Literal>pixelsPerCell</ogc:Literal>
                 <ogc:Literal>1</ogc:Literal>
               </ogc:Function>
               <ogc:Function name="parameter">
                 <ogc:Literal>outputBBOX</ogc:Literal>
                 <ogc:Function name="env">
                   <ogc:Literal>wms_bbox</ogc:Literal>
                 </ogc:Function>
               </ogc:Function>
               <ogc:Function name="parameter">
                 <ogc:Literal>outputWidth</ogc:Literal>
                 <ogc:Function name="env">
                   <ogc:Literal>wms_width</ogc:Literal>
                 </ogc:Function>
               </ogc:Function>
               <ogc:Function name="parameter">
                 <ogc:Literal>outputHeight</ogc:Literal>
                 <ogc:Function name="env">
                   <ogc:Literal>wms_height</ogc:Literal>
                 </ogc:Function>
               </ogc:Function>
             </ogc:Function>
           </Transformation>
           <Rule>
            <RasterSymbolizer>
              <Geometry>
                <!-- Actual geometry property name in feature source -->
                <ogc:PropertyName>geo</ogc:PropertyName></Geometry>
              <Opacity>0.6</Opacity>
              <ColorMap type="ramp" >
                <ColorMapEntry color="#FFFFFF" quantity="0" label="nodata" opacity="0"/>
                <ColorMapEntry color="#2851CC" quantity="1" label="values"/>
                <ColorMapEntry color="#211F1F" quantity="2" label="label"/>
                <ColorMapEntry color="#EE0F0F" quantity="3" label="label"/>
                <ColorMapEntry color="#AAAAAA" quantity="4" label="label"/>
                <ColorMapEntry color="#6FEE4F" quantity="5" label="label"/>
                <ColorMapEntry color="#DDB02C" quantity="10" label="label"/>
              </ColorMap>
            </RasterSymbolizer>
           </Rule>
         </FeatureTypeStyle>
       </UserStyle>
     </NamedLayer>
    </StyledLayerDescriptor>

Example WMS request including Geohash grid aggregation with the above custom style::

    http://localhost:8080/geoserver/test/wms?service=WMS&version=1.1.0&request=GetMap
         &layers=test:active&styles=geohashgrid&bbox=0.0,0.0,24.0,44.0&srs=EPSG:4326
         &width=418&height=768&format=application/openlayers
         &viewparams=a:{"agg": {"geohash_grid": {"field": "geo"\, "precision": 3}}}

Troubleshooting
^^^^^^^^^^^^^^^

* Commas in the aggregation body must be escaped with a backslash. Additionally body may need to be URL encoded.
* Geometry property name in the SLD RasterSymbolizer must be a valid geometry property in the layer
* Layers created with earlier (pre-aggregation support) versions of the plugin may need to be reloaded. In this case the layer must be removed and re-added to GeoServer (e.g. a feature type reload will not be sufficient).
* Aggregations are only supported when using the REST client with Elasticsearch 5.x

Grid Strategy
^^^^^^^^^^^^^
``gridStrategy``: Parameter to identify the ``mil.nga.giat.process.elasticsearch.GeoHashGrid`` implemenation that will be used to convert each geohashgrid bucket into a raster value (number).

.. list-table::
   :widths: 20 20 20 40

   * - Name
     - gridStrategy
     - gridStrategyArgs
     - Description
   * - Basic
     - ``basic``
     - no
     - Raster value is geohashgrid bucket ``doc_count``.
   * - Metric
     - ``metric``
     - yes
     - Raster value is geohashgrid bucket metric value.
   * - Nested
     - ``nested_agg``
     - yes
     - Extract raster value from nested aggregation results.

``gridStrategyArgs``: (Optional) Parameter used to specify an optional argument list for the grid strategy.

``gridStrategyEmptyCellValue``: (Optional) Parameter used to specify the value for empty grid cells. By default, empty grid cells are set to ``0``.

``gridStrategyScale``: (Optional) Parameter used to specify a scale applied to all raster values. Each tile request is scaled according to the min and max values for that tile. It is best to use a non-tited layer with this parameter to avoid confusing results.


Basic
~~~~~
Raster value is geohashgrid bucket ``doc_count``.

Example Aggregation::

  {
    "agg": {
      "geohash_grid": {
        "field": "geo"
      }
    }
  }
    
Example bucket::

 {
   "key" : "xv",
   "doc_count" : 1
 }

Extracted raster value: ``1``

Metric
~~~~~~
Raster value is geohashgrid bucket metric value.

.. list-table::
   :widths: 20 20 60

   * - Argument Index
     - Default Value
     - Description
   * - 0
     - ``metric``
     - Key used to pluck metric object from top level bucket. Empty string results in plucking doc_count.
   * - 1
     - ``value``
     - Key used to pluck the value from the metric object.

Example Aggregation::

  {
    "agg": {
      "geohash_grid": {
        "field": "geo"
      },
      "aggs": {
        "metric": {
          "max": {
            "field": "magnitude"
          }
        }
      }
    }
  }

Example bucket::

  {
    "key" : "xv",
    "doc_count" : 1,
    "metric" : {
      "value" : 4.9
    }
  }
    
Extracted raster value: ``4.9``

Nested
~~~~~~~~~~
Extract raster value from nested aggregation results.

.. list-table::
   :widths: 20 20 60

   * - Argument Index
     - Default Value
     - Description
   * - 0
     - ``nested``
     - Key used to pluck nested aggregation results from the geogrid bucket.
   * - 1
     - empty string
     - Key used to pluck metric object from each nested aggregation bucket. Empty string results in plucking doc_count.
   * - 2
     - ``value``
     - Key used to pluck the value from the metric object.
   * - 3
     - ``largest``
     - ``largest`` | ``smallest``. Strategy used to select a bucket from the nested aggregation buckets. The grid cell raster value is extracted from the selected bucket.
   * - 4
     - ``value``
     - ``key`` | ``value``. Strategy used to extract the raster value from the selected bucket. ``value``: Raster value is the selected bucket's metric value. ``key``: Raster value is the selected bucket's key.
   * - 5
     - null
     - (Optional) Map used to convert String keys into numeric values. Use the format ``key1:1;key2:2``. Only utilized when raster strategy is ``key``.


Example Aggregation::

  {
    "agg": {
      "geohash_grid": {
        "field": "geo"
      },
      "aggs": {
        "nested": {
          "histogram": {
            "field": "magnitude",
            "interval": 1,
            "min_doc_count": 1
          }
        }
      }
    }
  }

Example Parameters::

  <ogc:Function name="parameter">
    <ogc:Literal>gridStrategyArgs</ogc:Literal>
    <ogc:Literal>nested</ogc:Literal>
    <ogc:Literal></ogc:Literal>
    <ogc:Literal></ogc:Literal>
    <ogc:Literal>largest</ogc:Literal>
    <ogc:Literal>key</ogc:Literal>
  </ogc:Function>

Example bucket::

  {
    "key" : "xv",
    "doc_count" : 1729,
    "nested" : {
      "buckets" : [
        {
          "key" : 2.0,
          "doc_count" : 5
        },
        {
          "key" : 3.0,
          "doc_count" : 107
        },
        {
          "key" : 4.0,
          "doc_count" : 1506
        },
        {
          "key" : 5.0,
          "doc_count" : 100
        },
        {
          "key" : 6.0,
          "doc_count" : 11
        }
      ]
    }
  }

Extracted raster value: ``4.0``

Implementing a custom Grid Strategy
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default the raster values computed in the geohash grid aggregation rendering transformation correspond to the top level ``doc_count``. Adding an additional strategy for computing the raster values from bucket data currently requires source code updates to the ``gt-elasticsearch-process`` module as described below.

First create a custom implementation of ``mil.nga.giat.process.elasticsearch.GeoHashGrid`` and provide an implementation of the ``computeCellValue`` method, which takes the raw bucket data and returns the raster value. For example the default basic implementation simply returns the doc_count::

    public class BasicGeoHashGrid extends GeoHashGrid {
        @Override
        public Number computeCellValue(Map<String,Object> bucket) {
            return (Number) bucket.get("doc_count");
        }
    }

Then update ``mil.nga.giat.process.elasticsearch.GeoHashGridProcess`` and add a new entry to the Strategy enum to point to the custom implementation. 

After deploying the customized plugin the new geohash grid computer can be used by updating the ``gridStrategy`` parameter in the GeoServer style::

   <StyledLayerDescriptor version="1.0.0"
       ...
           <Transformation>
             <ogc:Function name="vec:GeoHashGrid">
               ...
               <ogc:Function name="parameter">
                 <ogc:Literal>gridStrategy</ogc:Literal>
                 <ogc:Literal>NewName</ogc:Literal>
               </ogc:Function>

Notes and Known Issues
----------------------

- ``PropertyIsEqualTo`` maps to an Elasticsearch term query, which will return documents that contain the supplied term. When searching on an analyzed string field, ensure that the search values are consistent with the analyzer used in the index. For example, values may need to be lowercase when querying fields analyzed with the default analyzer. See the Elasticsearch term query documentation for more information.
- ``PropertyIsLike`` maps to either a query string query or a regexp query, depending on whether the field is analyzed or not. Reserved characters should be escaped as applicable. Note case sensitive and insensitive searches may not be supported for analyzed and not analyzed fields, respectively. See Elasticsearch query string and regexp query documentation for more information.
- Date conversions are handled using the date format from the associated type mapping, or ``date_optional_time`` if not found. Note that UTC timezone is used for both parsing and printing of dates.
- Filtering on Elasticsearch ``object`` types is supported. By default, field names will include the full path to the field (e.g. "parent.child.field_name"), but this can be changed in the GeoServer layer configuration.

  - When referencing fields with path elements using ``cql_filter``, it may be necessary to quote the name (e.g. ``cql_filter="parent.child.field_name"='value'``)

- Filtering on Elasticsearch ``nested`` types is supported only for non-geospatial fields.
- Circle geometries are not currently supported
