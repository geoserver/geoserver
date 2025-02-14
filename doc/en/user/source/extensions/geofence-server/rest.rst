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
      <addressRange>..</addressRange>
      <service>..</service>
      <request>..</request>
      <workspace>..</workspace>
      <layer>..</layer>
      <subfield>..</subfield>
      <access>..</access>

      <limits> 
         <allowedArea>..</allowedArea>
         <catalogMode>..</catalogMode>
      </limits>

      <layerDetails>
         <layerType> VECTOR | RASTER | LAYERGROUP </layerType>
         <defaultStyle>..</defaultStyle>
         <cqlFilterRead>..</cqlFilterRead>
         <cqlFilterWrite>..</cqlFilterWrite>
         <allowedArea>..</allowedArea>
         <catalogMode>..</catalogMode>

         <allowedStyle>..</allowedStyle>
         ..

         <attribute>
            <name>..</name>
            <datatype>..</datatype>
            <accessType> NONE | READONLY | READWRITE </accessType>
         </attribute>
         ..
			
      </layerDetails>
   </Rule>

Encoding of a rule in JSON::

  {
    "Rule": {
      "id":..,
      "priority":..,
      "userName":"..",
      "roleName":"..",
      "addressRange",
      "service":"..",
      "request":"..",
      "subfield":"..",
      "workspace":"..",
      "layer":"..",
      "access":".."
    }
  }

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

  {
    "count":n,
    "rules":[
      {..},
      ..
    ]
  }	

Rule content
~~~~~~~~~~~~

.. list-table::
   :header-rows: 1
   :widths: 15 10 10 70

   * - Name
     - Type
     - M/O/C
     - Description
   * - priority
     - integer
     - M
     - Rule priority
   * - userName
     - string
     - O
     - The user this rule should be applied to
   * - roleName
     - string
     - O
     - The group this rule should be applied to
   * - addressRange
     - IPv4 CIDR notation
     - O
     - The range of calling IP addresses this rule should be applied to.  
       Example: ``192.168.0.0/16``
   * - service
     - string
     - O
     - The OGC service this rule should be applied to
   * - request
     - string
     - O
     - The OGC request this rule should be applied to
   * - subfield
     - string
     - O
     - An additional generic field for filtering rules. 
       At the moment only used to specify WPS processes in WPS calls.
   * - workspace
     - string
     - O
     - The workspace this rule should be applied to
   * - layer
     - string
     - O
     - The layer this rule should be applied to
   * - access
     - string
     - M
     - The type of access granted. May be  ``ALLOW | DENY | LIMIT``. When ``LIMIT`` the `limits` element should be declared.
   * - limits
     - complex
     - C
     - Mandatory when ``access=LIMIT``. Allowed when ``access=ALLOW``. Tells how the access should be limited.
   * - allowedArea
     - EWKT
     - O
     - Limit the geographic area that will be returned.
   * - catalogMode
     - String
     - O
     - GeoServer cataog mode to be applied. May be ``HIDE | CHALLENGE | MIXED``.
   * - layerDetails
     - complex
     - C
     - Only allowed when ``layer`` is specified. Set further limitations to the data access when the rule is matched.
   * - defaultStyle
     - String
     - O
     - If not null, forces a different style
   * - cqlFilterRead
     - CQL
     - O
     - Apply the CQL filter to the returned data.
   * - cqlFilterWrite
     - CQL
     - O
     - Limits the features that can be modified.
   * - allowedArea
     - EWKT
     - O
     - Limit the geographic area that will be returned.
   * - catalogMode
     - String
     - O
     - GeoServer cataog mode to be applied. May be ``HIDE | CHALLENGE | MIXED``.     
   * - attributes
     - complex
     - O
     - Set R/W privileges to the single attributes


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
     - Filter rules on username (excludes all other specific usernames).
   * - userAny
     - 0 or 1. 
     - Specify whether rules matching any username are included or not.
   * - roleName
     - string
     - Filter rules on rolename (excludes all other specific rolenames).
   * - roleAny
     - 0 or 1. 
     - Specify whether rules matching any rolename are included or not.
   * - ipAddress
     - string
     - Filter rules on IP address range (only select rules with an address range that includes the passed IP address).
   * - ipAddressAny
     - 0 or 1. 
     - Specify whether rules matching any IP address are included or not.
   * - service
     - string
     - Filter rules on service (excludes all other specific services).
   * - serviceAny
     - 0 or 1. 
     - Specify whether rules matching any service are included or not.
   * - request
     - string
     - Filter rules on request (excludes all other specific requests).
   * - requestAny
     - 0 or 1. 
     - Specify whether rules matching any request are included or not.
   * - workspace
     - string
     - Filter rules on workspace (excludes all other specific workspaces).
   * - workspaceAny
     - 0 or 1. 
     - Specify whether rules matching any workspace are included or not.
   * - layer
     - string
     - Filter rules on layer (excludes all other specific layers).
   * - layerAny
     - 0 or 1. 
     - Specify whether rules matching any layer are included or not.



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

