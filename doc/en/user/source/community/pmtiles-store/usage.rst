.. _pmtiles_store_usage:

Using PMTiles DataStores
=========================

This section describes how to configure and use PMTiles data stores in GeoServer through the web administration interface.

Creating a PMTiles DataStore
-----------------------------

To create a new PMTiles data store:

#. Navigate to :guilabel:`Stores` > :guilabel:`Add new Store`
#. Select :guilabel:`Protomaps PMTiles` from the Vector Data Sources list
#. Fill in the connection parameters in the configuration form
#. Click :guilabel:`Save`

After saving, GeoServer will connect to the PMTiles archive and discover the available tile layers.

Basic Configuration
-------------------

The PMTiles data store configuration form presents the following basic parameters:

**URI to a Protomaps PMTiles file** (required)
   A text field for the URI to your PMTiles archive. Supports multiple protocols:

   * Local files: ``file:///path/to/file.pmtiles``
   * HTTP/HTTPS servers: ``https://example.com/tiles.pmtiles``
   * Amazon S3: ``s3://bucket-name/path/to/file.pmtiles``
   * Azure Blob Storage: ``https://account.blob.core.windows.net/container/blob.pmtiles``
   * Google Cloud Storage: ``https://storage.googleapis.com/bucket/path/file.pmtiles``
   * S3-compatible services (MinIO): ``http://localhost:9000/bucket/file.pmtiles``

**Workspace/Namespace** (optional)
   The workspace in which to publish layers from this data store.

Data Source Examples
--------------------

Local File
^^^^^^^^^^

For a local PMTiles file, you can specify the path using either a ``file://`` URI or an absolute file path.

**Unix/Linux/Mac examples:**

With URI scheme:

.. code-block:: none

   file:///var/geoserver/data/tiles/countries.pmtiles

Without URI scheme:

.. code-block:: none

   /var/geoserver/data/tiles/countries.pmtiles

**Windows examples:**

With URI scheme (forward slashes):

.. code-block:: none

   file:///C:/GeoServer/data/tiles/countries.pmtiles

Without URI scheme (forward slashes):

.. code-block:: none

   C:/GeoServer/data/tiles/countries.pmtiles

Without URI scheme (backslashes):

.. code-block:: none

   C:\GeoServer\data\tiles\countries.pmtiles

HTTP/HTTPS Server
^^^^^^^^^^^^^^^^^

For PMTiles files hosted on web servers that support HTTP range requests:

.. code-block:: none

   https://tiles.example.com/osm/world.pmtiles

If the server requires authentication, see the HTTP Authentication Parameters section below.

Amazon S3
^^^^^^^^^

For files stored in Amazon S3 buckets:

.. code-block:: none

   s3://my-tiles-bucket/regions/europe.pmtiles

Or using the HTTPS URL format:

.. code-block:: none

   https://my-tiles-bucket.s3.us-west-2.amazonaws.com/regions/europe.pmtiles

The form will show additional S3-specific parameters in the advanced section (see below).

Azure Blob Storage
^^^^^^^^^^^^^^^^^^

For Azure Blob Storage, use the HTTPS URL format:

.. code-block:: none

   https://myaccount.blob.core.windows.net/tiles-container/cities/global.pmtiles

Additional Azure authentication parameters will appear in the advanced section.

Google Cloud Storage
^^^^^^^^^^^^^^^^^^^^

For Google Cloud Storage, use either format:

.. code-block:: none

   gs://my-tiles-bucket/world/basemap.pmtiles

Or the HTTPS URL:

.. code-block:: none

   https://storage.googleapis.com/my-tiles-bucket/world/basemap.pmtiles

Advanced Configuration Parameters
----------------------------------

Click the :guilabel:`Advanced` section in the data store configuration form to access additional parameters organized by category.

Memory Caching Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^

These parameters control in-memory caching of byte ranges to improve performance:

**Enable memory cache for raw byte data**
   Checkbox to enable in-memory caching. When enabled, frequently accessed byte ranges are cached in memory, reducing repeated reads from the underlying storage.

**Enable block-aligned memory caching**
   Checkbox to enable block-aligned caching. When enabled, read requests are aligned to block boundaries, which can improve performance for cloud storage by fetching larger, contiguous chunks of data.

**Memory cache block size in bytes**
   The block size in bytes for block-aligned caching. Recommended values are powers of 2 (e.g., 65536 for 64 KB, 1048576 for 1 MB). Larger blocks may improve performance for cloud storage but use more memory.

HTTP/HTTPS Parameters
^^^^^^^^^^^^^^^^^^^^^

These parameters appear when using HTTP or HTTPS URLs:

**HTTP connection timeout in milliseconds**
   Connection timeout for HTTP requests (default: 30000 ms).

**Trust all SSL/TLS certificates**
   Checkbox to disable SSL certificate validation. Use only for testing with self-signed certificates.

HTTP Authentication Parameters
"""""""""""""""""""""""""""""""

For HTTP/HTTPS URLs requiring authentication, the form provides multiple authentication options:

**HTTP Basic Authentication**

  * **HTTP Basic Auth username**: Username for HTTP Basic Authentication
  * **HTTP Basic Auth password**: Password for HTTP Basic Authentication (hidden)

**Bearer Token Authentication**

  * **HTTP Bearer Token**: Bearer token for authentication (hidden)

**API Key Authentication**

  * **API Key header name**: The HTTP header name for the API key (e.g., ``X-API-Key``)
  * **API Key value**: The API key value (hidden)
  * **API Key value prefix**: Optional prefix for the API key value (e.g., "Bearer " or "ApiKey ")

Amazon S3 Parameters
^^^^^^^^^^^^^^^^^^^^

These parameters appear when using S3 URLs or when connecting to S3-compatible services:

**Configure the AWS region**
   AWS region for the S3 bucket (e.g., ``us-west-2``, ``eu-central-1``).

**Use Default Credentials Provider**
   Checkbox to use the AWS default credentials chain (environment variables, IAM roles, credential file).

**AWS Access Key**
   AWS access key ID for authentication (if not using default credentials).

**AWS Secret Access Key**
   AWS secret access key (hidden, password field).

**Default Credentials Profile**
   AWS credential profile name to use from the credentials file.

**Enable S3 path style access**
   Checkbox to use path-style URLs (``https://s3.region.amazonaws.com/bucket/key``) instead of virtual-hosted-style URLs. Required for S3-compatible services like MinIO.

Azure Blob Storage Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

These parameters appear when using Azure Blob Storage URLs:

**Set the blob name if the endpoint points to the account URL**
   The blob name if your URL points to the storage account rather than a specific blob.

**Azure Account access key**
   Storage account key for authentication (hidden, password field).

**Azure SAS token**
   Shared Access Signature token for authentication (hidden, password field). Recommended for limited-time or limited-scope access.

Google Cloud Storage Parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

These parameters appear when using Google Cloud Storage URLs:

**Google Cloud project ID**
   Your GCP project ID.

**Quota Project ID**
   Optional project ID for quota and billing purposes.

**Use default application credentials**
   Checkbox to use Google's default application credentials chain (service account, application default credentials, etc.).

Performance Tuning
------------------

To optimize performance for cloud-based PMTiles archives:

#. **Enable memory caching**: Check "Enable memory cache for raw byte data"
#. **Enable block alignment**: Check "Enable block-aligned memory caching"
#. **Set appropriate block size**: Use 65536 (64 KB) for general use, or 1048576 (1 MB) for cloud storage with high latency

Example configuration for optimal cloud storage performance:

* Enable memory cache for raw byte data: **checked**
* Enable block-aligned memory caching: **checked**
* Memory cache block size in bytes: **65536** (or **1048576** for cloud storage)

Publishing Layers
-----------------

After creating and saving a PMTiles data store:

#. The store page will list the available tile layers discovered from the PMTiles metadata
#. Each layer corresponds to a vector tile layer in the archive
#. Click :guilabel:`Publish` next to a layer to configure it
#. Set the bounding box, coordinate system, and other layer properties
#. Click :guilabel:`Save` to make the layer available

The published layers can then be accessed through WFS and other OGC web services.

Limitations
-----------

* PMTiles data stores are **read-only** - you cannot edit features through GeoServer
* The data format is pre-tiled Mapbox Vector Tiles (MVT)
* Resolution and tiling structure are determined by the PMTiles archive content
* Changes to the underlying PMTiles file require reloading the store or restarting GeoServer
