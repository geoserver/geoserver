.. _rest_api_templates:

Freemarker templates
====================

`Freemarker <http://freemarker.sourceforge.net/>`_ is a simple yet powerful template engine that GeoServer uses for user customization of outputs.

It is possible to use the GeoServer REST API to manage Freemarker templates for catalog resources.

``/templates/<template>.ftl``
-----------------------------

This endpoint manages a template that is global to the entire catalog.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code
     - Formats
     - Default Format
   * - GET
     - Return a template
     - 200
     - 
     -  
   * - PUT
     - Insert or update a template
     - 405
     - 
     - 
   * - DELETE
     - Delete a template
     - 405
     - 
     - 

Identical operations apply to the following endpoints:

* Workspace templates—``/workspaces/<ws>/templates/<template>.ftl``
* Vector store templates—``/workspaces/<ws>/datastores/<ds>/templates/<template>.ftl``
* Feature type templates—``/workspaces/<ws>/datastores/<ds>/featuretypes/<f>/templates/<template>.ftl``
* Raster store templates—``/workspaces/<ws>/coveragestores/<cs>/templates/<template>.ftl``
* Coverage templates—``/workspaces/<ws>/coveragestores/<cs>/coverages/<c>/templates/<template>.ftl``
   
``/templates[.<format>]``
-------------------------

This endpoint manages all global templates.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code
     - Formats
     - Default Format
   * - GET
     - Return templates 
     - 200
     - HTML, XML, JSON
     - HTML

Identical operations apply to the following endpoints:

* Workspace templates—``/workspaces/<ws>/templates[.<format>]``
* Vector store templates—``/workspaces/<ws>/datastores/<ds>/templates[.<format>]``
* Feature type templates—``/workspaces/<ws>/datastores/<ds>/featuretypes/<f>/templates[.<format>]``
* Raster store templates—``/workspaces/<ws>/coveragestores/<cs>/templates[.<format>]``
* Coverage templates—``/workspaces/<ws>/coveragestores/<cs>/coverages/<c>/templates[.<format>]``
  
