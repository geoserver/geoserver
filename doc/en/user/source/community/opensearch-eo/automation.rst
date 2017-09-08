.. _opensearch_automation:

Automation with the administration REST API
============================================

The OpenSearch module sports full automation REST API that can be used to
create collections, ingest products and eventually their granules.
The full API is available at this URL:


* :api:`/oseo <opensearch-eo.yaml>`

Usage of the API for simple search publishing
----------------------------------------------

In case the OpenSearch module is used only for search purposes the collections
and products endpoints can be used directly.

For convenience and performance it is recommended to create collections and products
by using POST request with zip payloads.

Usage of the API for search and integrated OGC service publishing
-----------------------------------------------------------------

In this case the user intend to both use the OpenSearch module for search
purposes, but also to publish actual mosaics for each collection.

In this case the following approach should is recommended:

* Create a collection via the REST API, using the ZIP file POST upload
* Create at least one product in the collection in the REST API, using the
  ZIP file POST upload and providing a full ``granules.json`` content with all
  the granules of said product
* Post a layer publishing description file to ``/oseo/collection/<COLLECTION>/layer``
  to have the module setup a set of mosaic configuration files, store, layer with
  eventual coverage view and style

The layer configuration specification will have different contents depending on
the collection structure:

* Single CRS, non band split, RGB or RGBA files:

  .. code-block:: json

    {
    	"workspace": "gs",
    	"layer": "test123",
    	"separateBands": false,
    	"heterogeneousCRS": false
    }

* Single CRS, multiband in single file, with a gray browse style:

  .. code-block:: json

    {
    	"workspace": "gs",
    	"layer": "test123",
    	"separateBands": false,
    	"browseBands": [1],
    	"heterogeneousCRS": false
    }

* Heterogeneous CRS, multi-band split across files, with a RGB browse style:

  .. code-block:: json

    {
    	"workspace": "gs",
    	"layer": "test123",
    	"separateBands": true,
    	"bands": [
    		1,
    		2,
    		3,
    		4,
        5,
        6,
        7,
        8
    	],
    	"browseBands": [
    		4,
    		3,
    		2
    	],
    	"heterogeneousCRS": true,
    	"mosaicCRS": "EPSG:4326"
    }
