.. _community_vector_mosaic_config:

Vector Mosaic Datastore configuration
=====================================

When the extension has been installed, `Vector Mosaic Data Store` will be an option in the `Vector Data Sources` list when creating a new data store.

.. figure:: images/vector-mosaic-vector-create.png
   :align: center

   *Vector Mosaic Data Store in the list of vector data sources*

.. figure:: images/vector-mosaic-vector-configure.png
   :align: center

   *Configuring an Vector Mosaic data source*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``Workspace``
     - Name of the workspace to contain the Vector Mosaic store.
   * - ``Data Source Name``
     - Name of the Vector Mosaic Store as it will be known to GeoServer. 
   * - ``Description``
     - A full free-form description of the Vector Mosaic store.
   * - ``Enabled``
     -  If checked, it enables the store. If unchecked (disabled), no data in the Vector Mosaic Store will be served from GeoServer.
   * - ``delegateStoreName``
     - The data source name of the data store previously created that holds the index information about the constituent vector granules.  See `here <delegate.html>`_ for more details about delegate store requirements.
   * - ``connectionParameterKey``
     - The delegate store has a mandatory field called "params". Params can either be a URI pointing at the granule resource location or it can be a configuration string in .properties format. (See `Java Properties file <https://en.wikipedia.org/wiki/.properties>`_ for more details about the format) In the latter case this optional parameter specifies which key points at the location of the granule.  Accepted values are "file" and "url".
   * - ``preferredDataStoreSPI``
     - This optional parameter can serve as an optimization to speed up the lookup of granule data stores.  Instead of attempting to use the mandatory delegate params field (See `delegate requirements <delegate.html>`_ for more details about delegate store requirements.) to look up supported data store types, the Vector Mosaic data store will use the data store SPI specified in this field to identify the correct type.
   * - ``commonParameters``
     - This optional parameter can serve to specify common parameters required by the Vector Mosaic store to handle the underlying vector granules, like property collector configuration and shared delegate parameters store configurations, see also the vectorMosaic REST API.

Common Parameters
~~~~~~~~~~~~~~~~~
The commonParameters field provides a mechanism for passing shared configuration values that apply to the granule ingestion process regardless of the specific source dataset. 
These parameters are especially important when ingesting DGGS-based data or formats that require additional context (e.g., GeoParquet).

commonParameters are encoded as a flat property-file string, following the same conventions as Java Properties:
Key-value pairs separated by newline (\\n)
Keys and values are simple strings: key=value

This parameters block acts as:
- A shared configuration set provided to the ingestion pipeline.
- A bridge between VectorMosaic ingestion and external modules (e.g., OGC API-DGGS).
- A way to inject datastore-level or specific metadata without changing the request body structure.

Property Collectors
~~~~~~~~~~~~~~~~~~~
VectorMosaic REST API supports an extensible mechanism of PropertyCollectors, providing functionality analogous to the ImageMosaic property collectors used for raster granules.
Property collectors extract and compute metadata values during ingestion, making these values available in the VectorMosaic index. 

A PropertyCollector inspects an incoming vector granule (Shapefile, GeoParquet, etc.) and extracts one or more metadata properties.
These metadata values are stored as attributes in the VectorMosaic index record for the granule.

Typical uses include:
 - Extracting timestamps from the file path
 - Computing custom attributes from file metadata

In the same way as ImageMosaic, the commonParameters PropertyCollectors property accepts multiple collector definitions, separated by commas.

Compared to ImageMosaic, which stores specific collector definitions in dedicated sidecar configuration files, 
VectorMosaic ProeprtyCollectors configuration embeds the specific collector definition inline within square brackets.

ImageMosaic example:
the ``indexer.properties`` contains a line::

  PropertyCollectors=TimestampFileNameExtractorSPI[timeregex](time)

Referring a ``timeregex.properties`` sidecar file containing::
 
  regex=(\\d{4})/(\\d{2})/(\\d{2}),format=yyyyMMdd,fullPath=true

VectorMosaic:
the commonParameters contains the specific collector configuration with inline definition within square brackets::

  PropertyCollectors=TimestampFileNameExtractorSPI[regex=(\\d{4})/(\\d{2})/(\\d{2}),format=yyyyMMdd,fullPath=true](time)


See ImageMosaic's `Propertycollectors <../../data/raster/imagemosaic/configuration.html#property-collectors>`_ for further details on the available property collectors.


Example usage: DGGS GeoParquet Ingestion
""""""""""""""""""""""""""""""""""""""""

When ingesting GeoParquet files that contain DGGS cell geometries or DGGS zone identifiers, 
the ingestor requires additional context to interpret:

- Which DGGS instance is being used (e.g., H3)
- Which attribute contains the DGGS zone ID
- Optional DGGS resolution if not specified in the dataset
- Delegate datastore parameters (a DGGS Datastore is built on top of an underlying delegate alphanumeric datastore, in this case a GeoParquetDatastore. 
  Any parameter whose name starts with ``delegate.`` is automatically forwarded by the DGGSDatastore to the underlying delegate datastore. 
  During this process, the ``delegate.`` prefix is stripped before the parameter is supplied.)

An example of commonParameters section for a VectorMosaic of DGGS GeoParquets looks like:

.. figure:: images/vector-mosaic-common-params.png
   :align: center

With the following properties::

  dggs_id=H3
  zoneIdColumnName=h3indexstr
  resolution=10
  PropertyCollectors=TimestampFileNameExtractorSPI[regex=.*?(\\d{4})/(\\d{2})/(\\d{2}).*,format=yyyyMMdd,fullPath=true](time)
  delegate.dbtype=geoparquet

See :ref:`community_vector_mosaic_rest` for an example of ingesting DGGS Parquet into a VectorMosaic.



