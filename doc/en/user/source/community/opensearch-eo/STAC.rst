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

More information about writing templates can be found in the :ref:`templates guide <oseotemplates>`.
