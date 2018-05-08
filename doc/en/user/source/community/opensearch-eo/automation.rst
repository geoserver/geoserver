.. _opensearch_automation:

Automation with the administration REST API
============================================

The OpenSearch module supports full automation REST API that can be used to
create collections, ingest products and eventually their granules.
The full API is available at this URL:


* :api:`/oseo <opensearch-eo.yaml>`

In general terms, one would:

* Create a collection, along with description, thumbnail, metadata, OGC links
* Then create a product, along with description, thumbnail, metadata, OGC links
* Finally, and optionally, specify the granules composing the product (actually needed only
  if the OpenSearch subsystem is meant to be used for publishing OGC services layers too, 
  instead of being a simple search engine.

Understanding the zip file uploads
----------------------------------

The description of a collection and product is normally made of various components, in order to expedite
data creation and reduce protocol chattines, it is possible to bulk-upload all files composing
the description of collections and products as a single zip file.

Collection components
`````````````````````

A collection.zip, sent as a PUT request to ``rest/collections`` would contain the following files:

.. list-table::
   :widths: 10 10 80
   :header-rows: 1
   
   * - Name
     - Optional
     - Description
   * - collection.json
     - N
     - The collection attributes, matching the database structure (the prefixes are
       separated with a colon in this document)
   * - description.html
     - Y
     - The HTML description for the collection
   * - metadata.xml
     - Y
     - The metadata for the collection, in ISO format
   * - thumbnail.png, thumbnail.jpg or thumbnail.jpeg
     - Y
     - The collection thumbnail
   * - owsLinks.json
     - Y
     - The list of OWS links to OGC services providing access to the collection contents 
       (typically as a time enabled layer)

Product components
``````````````````

A product.zip, sent as a PUT request to ``rest/collections/<theCollection>/products`` would contain the following files:

.. list-table::
   :widths: 10 10 80
   :header-rows: 1
       
   * - Name
     - Optional
     - Description
   * - product.json
     - N
     - The product attributes, matching the database structure (the prefixes are
       separated with a colon in this JSON document)
   * - description.html
     - Y
     - The HTML description for the product
   * - metadata.xml
     - Y
     - The metadata for the collection, in O&M format
   * - thumbnail.png, thumbnail.jpg or thumbnail.jpeg
     - Y
     - The collection thumbnail
   * - owsLinks.json
     - Y
     - The list of OWS links to OGC services providing access to the product contents 
       (typically, a specific time slice in the collection layer, but other organizations are possible too)
   * - granules.json
     - Y
     - The list of actual files making up the product, along with their bounding boxes, file location
       and eventual band name, for products splitting bands in different files.
       Could be a single file, a list of files split by area, or a list of files representing the
       various bands of a multispectral product.

Template variable expansion
---------------------------

Some of the metadata/HTML description can embed simple templating variables that GeoServer will
expand while generating output. Here is a description of the variable, and where they can be used

.. list-table::
   :widths: 20 40 40
   :header-rows: 1
           
   * - Name
     - Description
     - Usage
   * - ${BASE_URL}
     - The server "base url", typically "protocol://host:port/geoserver", which can be 
       used to save links that can easily migrate between different environments (e.g.
       test vs production)
     - OGC links, original package location download links (for products), HTML descriptions for products and collections
   * - ${ISO_METADATA_LINK}
     - The link to a collection ISO metadata (GeoServer will point at a URL returning the
       metadata saved in the database)
     - A collection HTML description
   * - ${OM_METADATA_URL}
     - The link to a product O&M metadata (GeoServer will point at a URL returning the
       metadata saved in the database)
     - A product HTML description
   * - ${ATOM_URL}
     - The link to a collection ATOM representation, as returned by OpenSearch
     - A collection HTML description
   * - ${QUICKLOOK_URL}
     - A link to the product quicklook (GeoServer will point at a URL returning the quicklook 
       saved in the database)
     - A product sample image

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
