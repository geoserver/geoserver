.. _cog_plugin:

COG (Cloud Optimized GeoTIFF) Support
=====================================

`COG <https://github.com/cogeotiff/cog-spec/blob/master/spec.md>`_ (Cloud Optimized GeoTIFF) is a regular GeoTIFF file, aimed at being hosted on a HTTP file server, whose internal organization is friendly for consumption by clients issuing `HTTP GET range <https://en.wikipedia.org/wiki/Byte_serving>`_ requests.
The COG module allows to set configuration params to connect to a Cloud GeoTIFF, as well as adding JARs to the classpath needed to support that connection.

Installation
------------

As a community module, the package needs to be downloaded from the `nightly builds <https://build.geoserver.org/geoserver/>`_,
picking the community folder of the corresponding GeoServer series (e.g. if working on the GeoServer main development branch nightly
builds, pick the zip file form ``main/community-latest``).

To install the module, unpack the zip file contents into the GeoServer ``WEB-INF/lib`` directory and restart GeoServer.

COG GeoTIFF Configuration Panel
-------------------------------
The COG plugin does not add new stores, instead, it adds COG support to existing ones.

When configuring a GeoTIFF store, a new checkbox is available: ``Cloud Optimized GeoTIFF (COG)``. Setting that will open a new section presenting the COG configuration parameters for this COG Store.

.. figure:: images/cogparams.png

   COG Connection params

Checking the ``Cloud Optimized GeoTIFF (COG)`` checkbox will provide new options:

.. list-table::
   :widths: 20 80
   :header-rows: 1
   :stub-columns: 1

   * - Option
     - Description
   * - :guilabel:`URL`
     - (prefixed by ``cog://``) representing the connection URL to the COG Dataset.
   * - :guilabel:`Range Reader Settings`
     - Which type of Range Reader implementation. Values currently supported are HTTP, GoogleCloud, Azure, S3 the latter using an S3 Client
   * - :guilabel:`User Name / Access Key ID / Account Name`
     - Optional user name (HTTP) or Access Key ID (S3) or Account Name (Azure) in case the COG dataset requires authentication
   * - :guilabel:`Password / Secret Access Key / Account Key`
     - Password (HTTP) or Secret Access Key (S3) or Account Key (Azure) for the previous credential

COG ImageMosaic Configuration
-----------------------------
Additional configuration parameters can be specified in the ImageMosaic indexer configuration, in order to properly configure a COG based ImageMosaic.

:file:`indexer.properties`
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. list-table::
   :widths: 15 5 80
   :header-rows: 1
   :stub-columns: 1

   * - Parameter
     - Mandatory?
     - Description
   * - Cog
     - Y
     - A boolean flag (true/false) to be set (Cog=true) in order to signal that the ImageMosaic is a COG data mosaic.
   * - CogRangeReader
     - N
     - Specifies the desired RangeReader implementation performing the Range Reads requests. 
   * - CogUser
     - N
     - Credential to be set whenever basic HTTP authentication is needed to access the COG Datasets or an S3 Access KeyID is required or an Azure AccountName is required
   * - CogPassword
     - N
     - Password for the above user OR Secret Access Key for the above S3 KeyId or AccountKey for the above Azure AccountName.

.. _cog_plugin_rangereader:

COG RangeReader
```````````````
The following table provides the values for the ``CogRangeReader`` based on the type of target storage:

.. list-table::
   :widths: 20 80
   :header-rows: 1
   :stub-columns: 1
   
   * - Storage type
     - Class name
   * - HTTP
     - Can be omitted, or set to ``it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader``
   * - AWS S3
     - ``it.geosolutions.imageioimpl.plugins.cog.S3RangeReader``
   * - Google Cloud
     - ``it.geosolutions.imageioimpl.plugins.cog.GSRangeReader``
   * - Azure
     - ``it.geosolutions.imageioimpl.plugins.cog.AzureRangeReader``

COG Global Settings
-------------------
The GeoServer Global Settings page contains the default COG settings presented when setting up a new COG GeoTIFF Store.


.. figure:: images/globalcogsettings.png

   Default Global COG Settings

Image locations
---------------

For images served by a HTTP server, a HTTP URL must be used.
For images served by S3 or Google Cloud, it's possible to use both the public HTTP URL,
or the idiomatic URIS, for example:

* ``s3://landsat-pds/c1/L8/153/075/LC08_L1TP_153075_20190515_20190515_01_RT/LC08_L1TP_153075_20190515_20190515_01_RT_B2.TIF``
* ``gs://gcp-public-data-landsat/LC08/01/044/034/LC08_L1GT_044034_20130330_20170310_01_T2/LC08_L1GT_044034_20130330_20170310_01_T2_B11.TIF`` 

HTTP Client (OkHttp) configuration
----------------------------------
HTTP client configuration (based on `OkHttp client <https://square.github.io/okhttp/>`_) can be specified through Environment variables. 

.. list-table::
   :widths: 15 80
   :header-rows: 1
   :stub-columns: 1

   * - Environment Variable
     - Description
   * - IIO_HTTP_MAX_REQUESTS
     - The maximum number of requests to execute concurrently. Above this requests queue in memory, waiting for the running calls to complete. (Default 128)
   * - IIO_HTTP_MAX_REQUESTS_PER_HOST
     - The maximum number of requests for each host to execute concurrently. (Default 5)
   * - IIO_HTTP_MAX_IDLE_CONNECTIONS
     - The maximum number of idle connections. (Default 5)
   * - IIO_HTTP_KEEP_ALIVE_TIME
     - The Keep alive time (in seconds), representing maximum time that excess idle threads will wait for new tasks before terminating. (Default 60)

AWS S3 Client configuration
---------------------------
A single S3 Asynchronous Client will be used for the same region and alias (url schema, i.e. http, https). 
The following Environment Variables can be set to customize the pool for the asynchronous client for that particular alias. 
On the table below, replace the "$ALIAS$" template with HTTP or HTTPS or S3 if you are configuring properties for these schema. 

.. list-table::
   :widths: 15 80
   :header-rows: 1
   :stub-columns: 1

   * - Environment Variable
     - Description
   * - IIO_$ALIAS$_AWS_CORE_POOL_SIZE
     - The core pool size for the S3 Client (Default 50)
   * - IIO_$ALIAS$_AWS_MAX_POOL_SIZE
     - The maximum number of thread to allow in the pool for the S3 Client (Default 128)
   * - IIO_$ALIAS$_AWS_KEEP_ALIVE_TIME
     - The Keep alive time (in seconds), representing maximum time that excess idle threads will wait for new tasks before terminating. (Default 10)
   * - IIO_$ALIAS$_AWS_USER
     - Default user (access key ID) for AWS basic authentication credentials
   * - IIO_$ALIAS$_AWS_PASSWORD
     - Default password (secret access key) for AWS basic authentication credentials
   * - IIO_$ALIAS$_AWS_REGION
     - Default AWS region
   * - IIO_$ALIAS$_AWS_ENDPOINT
     - Endpoint to Amazon service or any other S3-compatible service run by a third-party 

Google Cloud storage configuration
----------------------------------

The credentials to access Google Cloud cannot be provided as username and password (an authentication
method that Google Cloud does not support), but need to be provided via a system variable pointing
to the key file::

    set GOOGLE_APPLICATION_CREDENTIALS=/path/to/the/key-file.json
    export GOOGLE_APPLICATION_CREDENTIALS

Azure configuration
-------------------
A single Azure Client will be used for the same container. 
Account and container will be retrieved from the provided Azure URL.
The following System Properties can be set to customize client properties where missing.

.. list-table::
   :widths: 15 80
   :header-rows: 1
   :stub-columns: 1

   * - System property
     - Description
   * - azure.reader.accountName
     - The Azure Account Name
   * - azure.reader.accountKey
     - The Azure Account Key for the above Account
   * - azure.reader.container
     - The default container for the above Account
   * - azure.reader.prefix
     - The optional prefix containing blobs
   * - azure.reader.maxConnections
     - The max number of connections supported by the underlying Azure client

Client configuration (System Properties)
----------------------------------------
Note that all the IIO  settings reported in the previous tables can also be specified using System Properties instead of Environment variables.
You just need to replace UPPER CASE words with lower case words and underscores with dots.
So, the value for Maximum HTTP requests can be specified by setting either a ``IIO_HTTP_MAX_REQUESTS`` Environment variable or a ``iio.http.max.requests`` Java System Property alternatively (Environment variables are checked first).

By default, when accessing a COG, an initial chunk of 16 KB is read in attempt to parse the header so that the reader will have the offset and length of the available tiles. When dealing with files hosting many tiles, it is possible that the whole header won't fit in the initial chunk. In this case additional reads (chunks of the same size) will be progressively made to complete loading the header.
A ``it.geosolutions.cog.default.header.length`` system property can be configured to set the length (in bytes) of the reading chunk. Tuning this so that the header is read with few extra requests can help improve performance. A value too large can cause memory consumption issues and will reduce efficiency, as un-necessary data will be read.
