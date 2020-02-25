.. _rest_api_geofence_server:

GeoFence Rest API
=================

Security
--------

The Geofence Rest API is only accessible to users with the role ROLE_ADMIN.

Input/Output
------------

Data Object Transfer
~~~~~~~~~~~~~~~~~~~~
Both XML and JSON are supported for transfer of data objects. The default is XML. Alternatively, JSON may be used by setting the 'content-type' (POST) and 'accept' (GET) http headers to 'application/json' in your requests.

Encoding of a rule in XML::

	<Rule>
		<id>..</id>
		<priority>..</priority>
		<userName>..</userName>
		<roleName>..</roleName>
		<workspace>..</workspace>
		<layer>..</layer>
		<service>..</service>
		<request>..</request>
		<access> ALLOW | DENY | LIMIT </access>
		
		<!-- for LIMIT access rules-->
		<limits> 
			<allowedArea>..</allowedArea>
			<catalogMode> HIDE | CHALLENGE | MIXED </catalogMode>
		</limits>
		
		<!-- for ALLOW access rules with specified layer -->
		<layerDetails>
			<layerType> VECTOR | RASTER | LAYERGROUP </layerType>

			<defaultStyle>..</defaultStyle>

			<cqlFilterRead>..</cqlFilterRead>

			<cqlFilterWrite>..</cqlFilterWrite>

			<allowedArea>..</allowedArea>

			<catalogMode> HIDE | CHALLENGE | MIXED </catalogMode>

			<allowedStyle>..</allowedStyle>
			
			<allowedStyle>..</allowedStyle>
			
			..

			<attribute>
				<name>..</name>
				<datatype>..</datatype>
				<accessType> NONE | READONLY | READWRITE </accessType>
			</attribute>			

			<attribute>
				<name>..</name>
				<datatype>..</datatype>
				<accessType> NONE | READONLY | READWRITE </accessType>
			</attribute>
			
			..
			
		</layerDetails>
	</Rule>

Encoding of a rule in JSON::

	{"Rule": {"id":..,"priority":..,"userName":"..","roleName":"..","workspace":"..","layer":"..","service":"..","request":"..","access":".."}}

In case a rule that has "any" ("*") for a particular field the field is either not included (default), left empty or specified with a single asterisk 
(the latter two may be used for updates to distinguish from "do not change this field").

Encoding of a list of rules in XML::

	<Rules count="n">
		<Rule> ... </Rule>
		<Rule> ... </Rule>
		...		
	</Rules>

The result of a count would not include the actual <Rule> tags.

Encoding of a list of rules in JSON::

	{"count":n,"rules":[{..},{..},..]}	


Filter Parameters
~~~~~~~~~~~~~~~~~

All filter parameters are optional.

.. list-table::
   :header-rows: 1
   :widths: 15 10 70

   * - Name
     - Type
     - Description
   * - page
     - number
     - Used for paging a list of rules. Specifies the number of the page. Leave out for no paging. If specified, ``entries`` should also be specified.
   * - entries
     - number
     - Used for paging a list of rules. Specifies the number of entries per page. Leave out for no paging. If specified, ``page`` should also be specified.
   * - userName
     - string
     - Filter rules on username (excludes all other specified usernames).
   * - userAny
     - 0 or 1. 
     - Specify whether rules for 'any' username are included or not.
   * - roleName
     - string
     - Filter rules on rolename (excludes all other specified rolenames).
   * - roleAny
     - 0 or 1. 
     - Specify whether rules for 'any' rolename are included or not.
   * - service
     - string
     - Filter rules on service (excludes all other specified services).
   * - serviceAny
     - 0 or 1. 
     - Specify whether rules for 'any' service are included or not.
   * - request
     - string
     - Filter rules on request (excludes all other specified requests).
   * - requestAny
     - 0 or 1. 
     - Specify whether rules for 'any' request are included or not.
   * - workspace
     - string
     - Filter rules on workspace (excludes all other specified workspaces).
   * - workspaceAny
     - 0 or 1. 
     - Specify whether rules for 'any' workspace are included or not.
   * - layer
     - string
     - Filter rules on layer (excludes all other specified layers).
   * - layerAny
     - 0 or 1. 
     - Specify whether rules for 'any' layer are included or not.



Requests
--------

``/rest/geofence/rules/``
~~~~~~~~~~~~~~~~~~~~~~~~~

Query all rules or add a new rule.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Supported parameters
     - Response
   * - GET
     - List all rules, with respect to any added filters
     - page, entries, userName, userAny, roleName, roleAny, service, serviceAny, request, requestAny, workspace, workspaceAny, layer, layerAny
     - 200 OK. List of rules in XML.
   * - POST
     - Add a new rule
     - None
     - 201 Inserted. Created ``ID`` header.


``/rest/geofence/rules/count``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Counts (filtered) rules.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Supported parameters
     - Response
   * - GET
     - Count all rules, with respect to any added filters
     - userName, userAny, roleName, roleAny, service, serviceAny, request, requestAny, workspace, workspaceAny, layer, layerAny
     - 200 OK. Rule list count in XML.

``/rest/geofence/rules/id/<id>``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query, modify or delete a specific rule.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Supported parameters
     - Response
   * - GET
     - Read rule information
     - None
     - 200 OK. Rule in XML.
   * - POST
     - Modify the rule, unspecified fields remain unchanged.
     - None
     - 200 OK.
   * - DELETE
     - Delete the rule
     - None
     - 200 OK.

