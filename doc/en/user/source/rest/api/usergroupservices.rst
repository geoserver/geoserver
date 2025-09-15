
.. _rest_api_usergroupservices:

User Group Services
===================

Manage **User/Group Services** through the REST API.

This resource lets administrators list, retrieve, create, update, and delete
*user/group service* configurations (e.g. the default XML file‑based service,
or an LDAP service).

.. note::
   You must be authenticated as a user with administrative privileges.
   Content negotiation is supported via the ``Accept`` and ``Content-Type`` headers
   (``application/xml`` and ``application/json``).

Collection
----------

**Endpoint**

``/rest/security/usergroupservices``

**Methods**

- **GET** — List configured services.
- **POST** — Create a new service.

Item
----

**Endpoint**

``/rest/security/usergroupservices/{name}``

**Methods**

- **GET** — Retrieve a service configuration.
- **PUT** — Create or replace *{name}* with the provided configuration.
- **DELETE** — Remove the service *{name}*.

.. warning::
   The ``default`` user/group service (or any service marked as required by the installation)
   cannot be deleted.

Representations
---------------

XML (XMLUserGroupService)
~~~~~~~~~~~~~~~~~~~~~~~~~

Minimal XML configuration for the built‑in file‑based service::

  <org.geoserver.security.xml.XMLUserGroupServiceConfig>
    <name>users1</name>
    <className>org.geoserver.security.xml.XMLUserGroupService</className>
    <fileName>users1.xml</fileName>
    <passwordEncoderName>plainTextPasswordEncoder</passwordEncoderName>
    <passwordPolicyName>default</passwordPolicyName>
  </org.geoserver.security.xml.XMLUserGroupServiceConfig>

.. important::
   ``fileName`` is **required** for ``XMLUserGroupService``.

JSON (XMLUserGroupService)
~~~~~~~~~~~~~~~~~~~~~~~~~~

The equivalent JSON payload::

  {
    "org.geoserver.security.xml.XMLUserGroupServiceConfig": {
      "name": "users1",
      "className": "org.geoserver.security.xml.XMLUserGroupService",
      "fileName": "users1.xml",
      "passwordEncoderName": "plainTextPasswordEncoder",
      "passwordPolicyName": "default"
    }
  }

XML (LDAPUserGroupService)
~~~~~~~~~~~~~~~~~~~~~~~~~~

Example configuration for an LDAP‑backed service::

  <org.geoserver.security.ldap.LDAPUserGroupServiceConfig>
    <name>ldapUsers</name>
    <className>org.geoserver.security.ldap.LDAPUserGroupService</className>
    <serverURL>ldap://localhost:10389/dc=acme,dc=org</serverURL>
    <groupSearchBase>ou=groups</groupSearchBase>
    <allGroupsSearchFilter>cn=*</allGroupsSearchFilter>
    <groupSearchFilter>member=uid={0},ou=people,dc=acme,dc=org</groupSearchFilter>
    <userSearchBase>ou=people</userSearchBase>
    <allUsersSearchFilter>uid=*</allUsersSearchFilter>
    <useTLS>true</useTLS>
    <useNestedParentGroups>true</useNestedParentGroups>
    <maxGroupSearchLevel>10</maxGroupSearchLevel>
    <nestedGroupSearchFilter>(member={0})</nestedGroupSearchFilter>
    <bindBeforeGroupSearch>true</bindBeforeGroupSearch>
    <rolePrefix>ROLE_</rolePrefix>
    <convertToUpperCase>true</convertToUpperCase>
    <user>admin</user>
    <password>geoserver</password>
    <passwordEncoderName>digestPasswordEncoder</passwordEncoderName>
    <passwordPolicyName>default</passwordPolicyName>
  </org.geoserver.security.ldap.LDAPUserGroupServiceConfig>

JSON (LDAPUserGroupService)
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The equivalent JSON payload::

  {
    "org.geoserver.security.ldap.LDAPUserGroupServiceConfig": {
      "name": "ldapUsers",
      "className": "org.geoserver.security.ldap.LDAPUserGroupService",
      "serverURL": "ldap://localhost:10389/dc=acme,dc=org",
      "groupSearchBase": "ou=groups",
      "allGroupsSearchFilter": "cn=*",
      "groupSearchFilter": "member=uid={0},ou=people,dc=acme,dc=org",
      "userSearchBase": "ou=people",
      "allUsersSearchFilter": "uid=*",
      "useTLS": true,
      "useNestedParentGroups": true,
      "maxGroupSearchLevel": 10,
      "nestedGroupSearchFilter": "(member={0})",
      "bindBeforeGroupSearch": true,
      "rolePrefix": "ROLE_",
      "convertToUpperCase": true,
      "user": "admin",
      "password": "geoserver",
      "passwordEncoderName": "digestPasswordEncoder",
      "passwordPolicyName": "default"
    }
  }

Operations
----------

List
~~~~

**GET** ``/rest/security/usergroupservices``

**Response**

- **200 OK** with a document containing the configured services.

**cURL**::

  curl -u admin:geoserver -H "Accept: application/xml" \
    "http://localhost:8080/geoserver/rest/security/usergroupservices"

Retrieve
~~~~~~~~

**GET** ``/rest/security/usergroupservices/{name}``

**Response**

- **200 OK** with the service configuration.
- **404 Not Found** if the service does not exist.

**cURL**::

  curl -u admin:geoserver -H "Accept: application/json" \
    "http://localhost:8080/geoserver/rest/security/usergroupservices/users1"

Create
~~~~~~

**POST** ``/rest/security/usergroupservices``

- **Request body**: one of the configuration payloads shown above.
- **Content-Type**: ``application/xml`` or ``application/json``

**Response**

- **201 Created** (some versions may return **200 OK**) and a ``Location`` header.
- **400 Bad Request** on validation errors (e.g. missing ``fileName`` for XML service).
- **400 Bad Request** if a service with the same name already exists.

**cURL**::

  curl -u admin:geoserver -H "Content-Type: application/xml" -H "Accept: application/xml" \
    -d @xml-usergroup-service.xml \
    "http://localhost:8080/geoserver/rest/security/usergroupservices"

Update / Replace
~~~~~~~~~~~~~~~~

**PUT** ``/rest/security/usergroupservices/{name}``

- Replaces (or creates) the service named *{name}* with the provided configuration.
- The ``name`` inside the payload must match the path parameter.

**Response**

- **200 OK** on successful update, or **201 Created** if newly created.
- **400 Bad Request** if the payload name does not match the path parameter.
- **400 Bad Request** on validation errors.

**cURL**::

  curl -u admin:geoserver -X PUT -H "Content-Type: application/json" -H "Accept: application/json" \
    -d @xml-usergroup-service.json \
    "http://localhost:8080/geoserver/rest/security/usergroupservices/users1"

Delete
~~~~~~

**DELETE** ``/rest/security/usergroupservices/{name}``

**Response**

- **200 OK** on successful deletion.
- **404 Not Found** if the service does not exist (some deployments may return **410 Gone**).
- **400 Bad Request** if attempting to delete a required service (e.g., the default one).

**cURL**::

  curl -u admin:geoserver -X DELETE \
    "http://localhost:8080/geoserver/rest/security/usergroupservices/users1"

Content Negotiation
-------------------

All operations accept/produce both XML and JSON. Either:

- Set headers: ``Accept: application/xml`` and/or ``Content-Type: application/xml`` (or JSON), or
- Use ``.xml`` / ``.json`` suffixes (if enabled in your deployment).

Notes & Tips
------------

- When creating an ``XMLUserGroupService``, the file referenced by ``fileName`` will be created
  under GeoServer's security directory if it does not already exist.
- For LDAP services, make sure the ``serverURL`` and search parameters match your directory
  layout. The ``groupSearchFilter`` and ``nestedGroupSearchFilter`` usually need adjustment.
- Passwords supplied in configuration payloads may be stored according to the chosen
  ``passwordEncoderName`` and policy.
