.. _STAC:

The STAC extension  
==================

The OpenSeach for EO subsystem exposes also a `STAC <https://stacspec.org/>`__ service, implemented
as a OGC API Features conformant `STAC API <https://github.com/radiantearth/stac-api-spec>`_.

The landing page of the STAC API is linked from the GeoServer home page, and available at ``$HOST:$PORT/geoserver/ogc/stac``.
The API exposes the OpenSearch for EO contents, restructuring them as needed:

* The collections table is mapped to STAC collections
* The products table is mapped to STAC items

Given the differences in names and structures the STAC resources are created using templates, in
particular:

* The HTML representation is built using :ref:`Freemarker templates <tutorial_freemarkertemplate>`
* The GeoJSON representation is built using GeoJSON :ref:`features templates <community_wfstemplating>`

The default templates work against the `default PostGIS database structure <https://raw.githubusercontent.com/geoserver/geoserver/main/src/community/oseo/oseo-core/src/test/resources/postgis.sql>`_ and
can be customized to include new properties to follow eventual database modifications.

All built-in templates are copied over to the data directory for customization, and placed
in the ``$GEOSERER_DATA_DIR/templates/ogc/stac`` folder:

* collection.ftl
* collection_include.ftl
* collections.ftl
* collections.json
* item.ftl
* item_include.ftl
* items-content.ftl
* items-empty.ftl
* items-footer.ftl
* items-header.ftl
* items.json
* landingPage.ftl
* queryables-collection.ftl
* queryables-common.ftl
* queryables-global.ftl
* search-content.ftl
* search-empty.ftl
* search-footer.ftl
* search-header.ftl

Specifically for the JSON output:

* `$GEOSERVER_DATA_DIR/templates/ogc/stac/v1/collections.json` is the `collections template <https://raw.githubusercontent.com/geoserver/geoserver/main/src/community/oseo/oseo-stac/src/main/resources/org/geoserver/ogcapi/v1/stac/collections.json>`_
* `$GEOSERVER_DATA_DIR/templates/ogc/stac/v1/items.json` is the `items template <https://raw.githubusercontent.com/geoserver/geoserver/main/src/community/oseo/oseo-stac/src/main/resources/org/geoserver/ogcapi/v1/stac/items.json>`_

The JSON templates in the case of STAC also drive database querying, the exposed STAC properties
are back-mapped into database properties by interpreting the template. It is advised to keep 
property mapping as simple as possible to allow usage of native SQL queries and indexes while
accessing the database through the STAC API.

For both items and collections, collection specific templates can also be provided, which would contain
directives and mappings unique to that collection.
A collection specific template can be placed in the same templates directory as above, 
using the naming convention ``items-<COLLECTION_ID>.json`` or ``collections-<COLLECTION_ID>.json``, 
where ``<COLLECTION_ID>`` is the collection identifier. 
For example, if the collection is named ``SENTINEL2``:

* The collections specific template for it is named ``collections-SENTINEL2.json``
* The items template specific for it is named ``items-SENTINEL2.json``

Fields fragments
-----------------
When dealing with JSON output for GET requests in the context of STAC service, the module supports the selection of fields based on the inclusion and exclusion semantic described in the `field fragments specification <https://github.com/radiantearth/stac-api-spec/tree/master/fragments/fields#includeexclude-semantics>`_.
According to the current specification:

- If no ``fields`` query parameter is specified all the item's attribute are returned.
- If a ``fields`` attribute is specified with no values, only the item's default values (the one necessary to have a valid STAC entity) are returned: ``id``,``type``,``geometry``,``bbox``,``links``,``assets``,``properties.datetime``,``properties.created``.
- If ``fields`` value is specified GeoServer will return always the default attributes, if the user doesn't target them as excluded. Eg. ``assets`` will always be present if not exluced explicitly (``fields=-assets,...``).
- If only include is specified, these attributes are added to the default set of attributes (set union operation).
- If only exclude is specified, these attributes are subtracted from the union of the default set of attributes and the include attributes (set difference operation). This will result in an entity that is not a valid Item if any of the excluded attributes are in the default set of attributes, but no error message will be raised by GeoServer.
- If a attribute is included, e.g. ``properties``, but one or more of the nested attributes is excluded, e.g. ``-properties.datetime``, then the excluded nested attributes will not appear in properties.
- If an attribute is excluded, e.g. ``-properties.nestedObj``, but one of more of the nested attributes is included, e.g. ``properties.nestedObject.attribute``, then ``nestedObject`` will appear in the output with the included attributes only.

Datacube Extension Support
--------------------------
Support for the `STAC Datacube Extension <https://github.com/stac-extensions/datacube>`_ "cube_dimensions" elements is available in HTML and JSON templates via the `eoSummaries` function.  `eoSummaries` supports presenting the following collection-wide summary statistics:

* min - The minimum value of the field in the collection
* max - The maximum value of the field in the collection
* distinct - An array of distinct values of the field in the collection
* bounds - Minimum and maximum dimension values of the spatial bounding box of the collection (either x or y, presented as a two value array or xmin, xmax, ymin, ymax presented as individual dimension values)

`eoSummaries` has three arguments:

* aggregate - The type of summary statistic.  One of "min", "max", "distinct", or "bounds".
* collectionIdentifier - The name of the collection that is being summarized.
* property - The name of the property being summarized.  
	
	* Note that for the "bounds" aggregate, this value should either be "x","y","xmin","ymin","xmax", or "ymax".

**JSON Template Example**:

.. code-block:: none

  "extent": {
    "spatial": {
      "bbox": [
        [
          "$${eoSummaries('bounds',eo:parentIdentifier,'xmin')}",
          "$${eoSummaries('bounds',eo:parentIdentifier,'ymin')}",
          "$${eoSummaries('bounds',eo:parentIdentifier,'xmax')}",
          "$${eoSummaries('bounds',eo:parentIdentifier,'ymax')}"
        ]
      ]
    },
	"cube:dimensions"\: {
     "x": {
      	"type": "spatial",
      	"axis": "x",
      	"extent": "$${eoSummaries('bounds',eo:parentIdentifier,'x')}",
      	"reference_system": 4326},
			"y": {
     		"type": "spatial",
     		"axis": "y",
     		"extent": "$${eoSummaries('bounds',eo:parentIdentifier,'y')}",
     		"reference_system": 4326},
     		"time": 
     			{"type": "temporal",
     			"extent": 
     				["$${eoSummaries('min',eo:parentIdentifier,'timeStart')}",
     			"$${eoSummaries('min',eo:parentIdentifier,'timeEnd')}"]
     			}
     	}
    
**HTML/FTL Example**:

.. code-block:: none

	<li><b>Extents</b>:
	     <ul>
        <li data-tid='gbounds'>Geographic (WGS84):
					${model.eoSummaries("bounds",a.name.value,"x")[0]}, 
					${model.eoSummaries("bounds",a.name.value,"y")[0]}, 
					${model.eoSummaries("bounds",a.name.value,"x")[1]}, 
					${model.eoSummaries("bounds",a.name.value,"y")[1]}.
				</li>
				<li data-tid='tbounds'>Temporal: 
					${model.eoSummaries("min",a.name.value,"timeStart")}/
					${model.eoSummaries("max",a.name.value,"timeEnd")}
				</li> 
			</ul>
	</li>


Layer information support
-------------------------

The OpenSearch REST admin API allows to create and manage layers, and the STAC API exposes them as collections.

Layers are kept in the ``layers`` attribute of a collection, each one exposes the following information:

* All ``collection_layers`` attributes (``workspace``, ``layer``, ``bands``, and so on)
* The layer ``title`` and ``description`` from the GeoServer own configuration
* A list of ``styles``, matching the styles associated to the layer. The first one is the default style. Each style contains the follwoing properties:
  - ``name``: the style name
  - ``title``: the style title
* A ``services`` object with attributes matching a lowercase service name, e.g., ``wms``, ``wcs``, ``wmts``, each one being an object with the following two fields:
    - ``enabled``: true or false (might depend on the layer configuration too)
    - ``formats``: the formats supported, for that layer, by the main service output (e.g., maps, coverages, tiles)

This information could be used, for example, to implement the STAC `web-map-links <https://github.com/stac-extensions/web-map-links>`_ extension.

Here is a Freemarker template snipped using the layer information:

   .. code-block::

    <#include "common-header.ftl">
      <h1>This is a collection with layers</h1>
      
      <#list collection.layers as layerWrapper>
        <#assign layer=layerWrapper.rawValue>
        <h2>${layer.workspace.value}:${layer.layer.value}</h2>
        <p class="title">${layer.title.value}</p>
        <p class="description">${layer.description.value}</p>
        
        <#if layer.styles??>
          <h3>Styles</h3>
            <#list layer.styles as styleWrapper>
              <#assign style=styleWrapper.rawValue>
              <p class="style">${(style.name.value)!}: ${(style.title.value)!}</p>
            </#list>
        </#if>
        
        <#assign wms=layer.services.rawValue.wms.rawValue>
        <#if wms??>
            <h3>WMS</h3>
            <p>Enabled: ${wms.enabled.value}</p>
            <p>Formats: 
            <ul>
            <#list wms.formats as format>
              <li>${format.value}</li>
            </#list>
            </ul>
        </#if>
        
        <#assign wcs=layer.services.rawValue.wcs.rawValue>
        <#if wcs??>
            <h3>WCS</h3>
            <p>Enabled: ${wcs.enabled.value}</p>
            <p>Formats: 
            <ul>
            <#list wcs.formats as format>
              <li>${format.value}</li>
            </#list>
            </ul>
        </#if>
        
        <#assign wmts=layer.services.rawValue.wmts.rawValue>
        <#if wmts??>
            <h3>WMTS</h3>
            <p>Enabled: ${wmts.enabled.value}</p>
            <p>Formats: 
            <ul>
            <#list wmts.formats as format>
              <li>${format.value}</li>
            </#list>
            </ul>
        </#if>
      </#list>

And here is a Features Templating JSON template exposing the same information, to use in the collection JSON output:

   .. code-block:: json

    {
      "collections": [
        {
          "id": "${name}",
          "title": "${title}",
          "layers": [
            {
              "$source": "layers"
            },
            {
              "workspace": "${workspace}",
              "layer": "${layer}",
              "styles": [
                {
                  "$source": "styles"
                },
                {
                  "name": "${name}",
                  "title": "${title}"
                }
              ],
              "services": {
                "wms": {
                  "enabled": "${services.wms.enabled}",
                  "formats": "${services.wms.formats}"
                },
                "wcs": {
                  "enabled": "${services.wcs.enabled}",
                  "formats": "${services.wcs.formats}"
                }
              }
            }
          ]
        }
      ]
    }

