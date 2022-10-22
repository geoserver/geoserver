.. _features_templating_rest:

Features Templatring Rest API
==============================

Introduction
-------------

The Features Templating Rest API allows performing CRUD operation over Features Templates and Template Layer Rules.

Template Configuration
-----------------------


``/rest/featurestemplates``

Finds all templates in the global  (``features-templating``) directory or creates a new template in the global directory.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Supported parameters
     - Response
   * - GET
     -
     - application/xml, application/json.
     - List of all the templates available in the  ``features-templating`` directory.
     - 
     - 200. List of rules in XML or JSON.
   * - POST
     - application/xml, text/xml, application/json, text/json, application/xhtml+xml, application/zip.
     - text/plain.
     - Add the template in the request body (text or zip file) as a new Template in the ``features-templating`` directory.
     - templateName (mandatory when posting a raw template, optional when posting a zip file)
     - 201. Created ``Location`` header.


``/rest/workspaces/<workspace name>/featurestemplates``

Finds all templates in the ``workspace`` directory or creates a new template in the ``workspace`` directory.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Supported parameters
     - Response
   * - GET
     -
     - application/xml, application/json.
     - List of all the templates available in the  ``workspace`` directory
     - 
     - 200. List of rules in XML or JSON.
   * - POST
     - application/xml, text/xml, application/json, text/json, application/xhtml+xml, application/zip.
     - text/plain.
     - Add the template in the request body (text or zip file) as a new Template in the ``workspace`` directory.
     - templateName (mandatory when posting a raw template, optional when posting a zip file)
     - 201. Created ``Location`` header.


``/rest/workspaces/<workspace name>/featuretypes/<featureType name>/featurestemplates``


Finds all templates in the ``featuretype`` directory or creates a new template in the ``featuretype`` directory.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Supported parameters
     - Response
   * - GET
     -
     - application/json, application/xml.
     - List of all the templates available in the  ``featuretype`` directory
     - 
     - 200. List of rules in XML or JSON.
   * - POST
     - application/xml, text/xml, application/json, text/json, application/xhtml+xml, application/zip.
     - text/plain.
     - Add the template in the request body (text or zip file) as a new Template in the ``Feature Type`` directory.
     - templateName (mandatory when posting a raw template, optional when posting a zip file)
     - 201. Created ``Location`` header.



``/rest/featurestemplates/<template name>``

If the template with the specified name exists in the global  (``features-templating``) directory, returns the template or replaces the template content with the one in the request body.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Response
   * - GET
     - 
     - application/xml, application/json, application/xhtml+xml.
     - the template with the specified name if present in the ``features-templating`` directory.
     - 200. The template.
   * - PUT
     - application/xml, text/xml, application/json, text/json, application/xhtml+xml, application/zip.
     - text/plain.
     - replace the template, if found in the ``features-templating`` directory with the template in the request body (text or zip file).
     - 201.
   * - DELETE
     -
     -
     - delete the template, if found in the ``features-templating`` directory.
     - 204.


``/rest/workspaces/<workspace name>/featurestemplates/<template name>``


If the template with the specified name exists in the ``workspace`` directory, returns the template or replaces the template content with the one in the request body.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Response
   * - GET
     - 
     - application/xml, application/json, application/xhtml+xml.
     - the template with the specified name if present in the ``workspace`` directory.
     - 200. The template.
   * - PUT
     - application/xml, text/xml, application/json, text/json, application/xhtml+xml, application/zip.
     - text/plain.
     - replace the existing template, if found in the ``workspace`` directory with the template in the request body (text or zip file).
     - 201.
   * - DELETE
     -
     -
     - delete the template, if found in the ``workspace`` directory.
     - 204.


``/rest/workspaces/<workspace name>/featuretypes/<featureType name>``
``/featurestemplates/<template name>``

If the template with the specified name exists in the ``featuretype`` directory, returns the template or replaces the template content with the one in the request body.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Response
   * - GET
     -
     - application/xml, application/json, application/xhtml+xml.
     - the template with the specified name if present in the ``featuretype`` directory.
     - 200. The template.
   * - PUT
     - application/xml, text/xml, application/json, text/json, application/xhtml+xml, application/zip.
     - text/plain.
     - replace the existing template, if found in the ``featuretype`` directory with the template in the request body (text or zip file).
     - 201.
   * - DELETE
     -
     -
     - delete the template, if found in the ``featuretype`` directory.
     - 204.


Template Rule Configuration
----------------------------


``/rest/workspaces/<workspace name>/featuretypes/<featureType name>/templaterules``

Finds all the configured template rules for the ``featuretype`` or creates a new one.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Response
   * - GET
     -
     - application/xml, application/json.
     - List of all the template rules available for the ``featuretype``.
     - 200. List of rules in XML or JSON.
   * - POST
     - application/xml, text/xml, application/json, text/json.
     - text/plain.
     - Add the template rule in the request body.
     - 201. Created ``Location`` header.


``/rest/workspaces/<workspace name>/featuretypes/<featureType name>``
``/templaterules/<rule identifier>``

Finds, replaces, updates or deletes the template rule with the specified identifier.

.. list-table::
   :header-rows: 1
   :widths: 5 20 20 20 10

   * - Method
     - Consumes
     - Produces
     - Action
     - Response
   * - GET
     -
     - application/xml, application/json.
     - The rule with the specified ``rule identifier``.
     - 200. List of rules in XML or JSON.
   * - PUT
     - application/xml, text/xml, application/json, text/json.
     - text/plain.
     - Replace the rule with the specified id with the one provided in the request body.
     - 201.
   * - PATCH
     - application/xml, text/xml, application/json, text/json.
     - text/plain.
     - Allows partial updates of the rule with the specified id using the fields specified in the rule provided in the request body. It uses a `JSON merge patch like strategy <https://datatracker.ietf.org/doc/html/rfc7386>`_
     - 201.
   * - DELETE
     - 
     -
     - Delete the rule with the specified id.
     - 204.


Data Object Transfer
~~~~~~~~~~~~~~~~~~~~
Both XML and JSON are supported for transfer of data objects.

Encoding of a template rule in XML::

	<Rule>
		<ruleId>..</ruleId>
		<priority>..</priority>
		<templateName>..</templateName>
		<outputFormat>..</outputFormat>
		<cqlFilter>..</cqlFilter>
    <profileFilter>...</profileFilter>
	</Rule>

Encoding of a rule in JSON::

	{"Rule": {"ruleId":..,"priority":..,"templateName":"..","outputFormat":"..","cqlFilter":"..","profileFilter":".."}}

When applying partial updates missing attributes/element in incoming object are left unchanged. Properties can be set to null. E.g. the following example will allow to set the profileFilter to null:

XML:: 

  <Rule>
    <profileFilter xsi:nil="true"/>
  </Rule>

JSON:: 

  {"Rule":{"profileFilter":null}}

