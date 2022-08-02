.. _rest_api_geofence_server_batch:

Batch Rest API
===================

Batch operations allow to run multiple insert, update and delete at the same time over rules and admin rules. All the operations are executed in a single transaction: this means that either all of them are successful or all the operations are rolled back.

Security
--------

The Geofence REST API is only accessible to users with the role ``ROLE_ADMIN``.

Input/Output
------------

Data Object Transfer
~~~~~~~~~~~~~~~~~~~~
Both XML and JSON are supported for transfer of data objects. The default is XML. Alternatively, JSON may be used by setting the ``Content-Type`` and ``Accept`` HTTP headers to ``application/json`` in your requests.

A ``Batch`` data object transfer must declare a list of ``operations``. Each operation needs to declare:

* The ``service`` name (``rules`` for a Rule operation or ``adminrules`` for an AdminRule operation).

* The ``type`` of the operation (``insert``, ``update``, ``delete``).

* The ``id`` of the entity over which the operation is being performed in case of an ``update`` or ``delete`` types.

* The ``Rule`` or ``AdminRule`` data object transfer in case of ``insert`` or ``update`` operation.

Encoding of a Batch in XML::

	<Batch>
   <operations service="rules" id="2" type="update">
      <Rule id="2">
         <access>ALLOW</access>
         <layer>layer</layer>
         <priority>5</priority>
         <request>GETMAP</request>
         <roleName>ROLE_AUTHENTICATED</roleName>
         <service>WMS</service>
         <workspace>ws</workspace>
      </Rule>
   </operations>
   <operations service="rules" id="5" type="delete" />
   <operations service="adminrules" type="insert">
      <RuleAdmin>
         <priority>2</priority>
         <roleName>ROLE_USER</roleName>
         <workspace>ws</workspace>
         <access>ADMIN</access>
      </RuleAdmin>
   </operations>
 </Batch>

Encoding of a Batch in JSON::

	{
   "Batch":{
      "operations":[
         {
            "@service":"adminrules",
            "@type":"update",
            "@id":"3",
            "Rule":{
               "access":"ALLOW",
               "layer":"layer",
               "priority":5,
               "request":"GETMAP",
               "service":"WMS",
               "roleName":"ROLE_AUTHENTICATED",
               "workspace":"ws"
            }
         },
         {
            "@service":"rules",
            "@type":"delete",
            "@id":5
         },
         {
            "@service":"adminrules",
            "@type":"insert",
            "AdminRule":{
               "priority":2,
               "roleName":"ROLE_USER",
               "workspace":"ws",
               "access":"ADMIN"
            }
         }
      ]
   }
 }


Requests
--------

``/rest/geofence/batch/exec``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Issue a Batch operation executing all the declared operations.

+------------+-----------------+--------------+-------------------------------------------------------------+
| Method     | Action          | Response code| Response                                                    |
+============+=================+==============+=============================================================+
| POST       | Execute a batch | 200          | OK                                                          |
|            |                 +--------------+-------------------------------------------------------------+
|            |                 | 400          | BadRequest: malformed request body, duplicate rule addition |
|            |                 +--------------+-------------------------------------------------------------+
|            |                 | 404          | NotFound: rule not found                                    |
|            |                 +--------------+-------------------------------------------------------------+
|            |                 | 500          | InternalServerError: unexpected error                       |
+------------+-----------------+--------------+-------------------------------------------------------------+

