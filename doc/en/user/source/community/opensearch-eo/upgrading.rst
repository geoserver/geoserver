.. _upgrading:

Upgrading from previous version
-------------------------------

Removal of ``htmlDescription``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Starting with version 2.20 the OpenSearch module dropped the HTML template columns in the
database, and switched to freemarker templates instead. This relieves the database from a 
significant burden, especially on the products table.

The default templates are automatically used, and the old ``htmlDescription`` columns ignored
(they should therefore be removed). 

In order for the default `collection.ftl <https://github.com/geoserver/geoserver/blob/main/src/community/oseo/oseo-service/src/main/resources/org/geoserver/opensearch/eo/response/collection.json>`_
to work, two new fields, ``title`` and ``description``, should be added to the database
structure, if not already present.

As a result of these changes, the REST resources previously used to manage the description templates
have been removed, and residual HTML description templates included in product or collection
zips will be ignored.

The replacement Freemarker templates are :ref:`located in the data directory<oseo_html_templates>`
and can be thus managed via the :api:`/resource <resource.yaml>` REST API.

Removal of ``collection_metadata`` and ``product_metadata``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Starting with version 2.20 the OpenSearch module dropped the metadata storage tables from the
database, and switched to freemarker templates instead. This relieves the database from a 
significant burden, especially on the products metadata table.

The default templates are automatically used, and the old metadata tables are ignored
(they should therefore be removed). 

As a result of these changes, the REST resources previously used to manage the metadata
have been removed, and residual metadata xml files included in product or collection
zips will be ignored.

The replacement Freemarker templates are :ref:`located in the data directory<oseo_metadata_templates>`
and can be thus managed via the :api:`/resource <resource.yaml>` REST API.

