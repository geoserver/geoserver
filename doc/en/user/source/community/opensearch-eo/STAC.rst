.. _STAC:

The STAC extension  
==================

The OpenSeach for EO subsytem exposes also a `STAC <https://stacspec.org/>`__ service, implemented
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

* `$GEOSERER_DATA_DIR/templates/ogc/stac/collections.json` is the `collections template <https://raw.githubusercontent.com/geoserver/geoserver/main/src/community/oseo/oseo-stac/src/main/resources/org/geoserver/ogcapi/stac/collections.json>`_
* `$GEOSERER_DATA_DIR/templates/os-eo/items.json` is the `items template <https://raw.githubusercontent.com/geoserver/geoserver/main/src/community/oseo/oseo-service/src/main/resources/org/geoserver/opensearch/eo/items.json>`_

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
- If ``fields`` value is specified GeoServer will return always the default attributes, if the user doesn't target them as exluded. Eg. ``assets`` will always be present if not exluced explicitly (``fields=-assets,...``).
- If only include is specified, these attributes are added to the default set of attributes (set union operation).
- If only exclude is specified, these attributes are subtracted from the union of the default set of attributes and the include attributes (set difference operation). This will result in an entity that is not a valid Item if any of the excluded attributes are in the default set of attributes, but no error message will be raised by GeoServer.
- If a attribute is included, e.g. ``properties``, but one or more of the nested attributes is excluded, e.g. ``-properties.datetime``, then the excluded nested attributes will not appear in properties.
- If an attribute is excluded, e.g. ``-properties.nestedObj``, but one of more of the nested attributes is included, e.g. ``properties.nestedObject.attribute``, then ``nestedObject`` will appear in the output with the included attributes only.
