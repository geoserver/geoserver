.. _rest_api_geofence_server_adminrule:

AdminRules Rest API
===================

Security
--------

The Geofence Rest API is only accessible to users with the role ROLE_ADMIN.

Input/Output
------------

Data Object Transfer
~~~~~~~~~~~~~~~~~~~~
Both XML and JSON are supported for transfer of data objects. The default is XML. Alternatively, JSON may be used by setting the 'content-type' (POST) and 'accept' (GET) http headers to 'application/json' in your requests.

Encoding of an AdminRule in XML::

	<AdminRule>
		<id>..</id>
		<priority>..</priority>
		<userName>..</userName>
		<roleName>..</roleName>
		<addressRange>..</addressRange>
		<workspace>..</workspace>
		<access>..</access>
	</AdminRule>

Encoding of a rule in JSON::

	{"id":..,"priority":..,"userName":"..","roleName":"..","addressRange":"..","workspace":"..","access":".."}

In case a rule that has "any" ("*") for a particular field the field is either not included (default), left empty or specified with a single asterisk 
(the latter two may be used for updates to distinguish from "do not change this field").

``access`` should be either ``ADMIN`` or ``USER``.

``addressRange`` is a string in CIDR notation (block/bits: e.g. ``127.0.0.1/32``).

Encoding of a list of rules in XML::

	<AdminRules count="n">
		<AdminRule> ... </AdminRule>
		<AdminRule> ... </AdminRule>
		...		
	</AdminRules>

The result of a count would not include the actual <AdminRule> tags.

Encoding of a list of rules in JSON::

	{"count":n,"adminrules":[{..},{..},..]}	


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
     - Specify whether rules matching any username should be included or not.
   * - roleName
     - string
     - Filter rules on rolename (excludes all other specified rolenames).
   * - roleAny
     - 0 or 1. 
     - Specify whether rules matching any rolename should be included or not.
   * - workspace
     - string
     - Filter rules on workspace (excludes all other specified workspaces).
   * - workspaceAny
     - 0 or 1. 
     - Specify whether rules matching any workspace should be included or not.



Requests
--------

``/geofence/rest/adminrules/``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query all adminrules or add a new adminrule.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Supported parameters
     - Response
   * - GET
     - List all adminrules, with respect to any added filters
     - page, entries, userName, userAny, roleName, roleAny, workspace, workspaceAny
     - 200 OK. List of adminrules in XML.
   * - POST
     - Add a new adminrule
     - None
     - 201 Inserted. Created ``ID`` header.


``/geofence/rest/adminrules/count``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Counts (filtered) adminrules.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Supported parameters
     - Response
   * - GET
     - Count all adminrules, with respect to any added filters
     - userName, userAny, roleName, roleAny, workspace, workspaceAny
     - 200 OK. Rule list count in XML.

``/geofence/rest/adminrules/id/<id>``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query, modify or delete a specific adminrule.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Supported parameters
     - Response
   * - GET
     - Read adminrule information
     - None
     - 200 OK. AdminRule in XML.
   * - POST
     - Modify the adminrule, unspecified fields remain unchanged.
     - None
     - 200 OK.
   * - DELETE
     - Delete the adminrule
     - None
     - 200 OK.

