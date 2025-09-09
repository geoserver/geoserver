.. _rest_usergroupservices:

User/Group Services
===================

The REST API allows you to list, create, update, and delete **user/group services** in GeoServer.
These endpoints manage the *configuration* objects (e.g., XMLUserGroupService, JDBC, LDAP), not the users/groups themselves.

.. note:: Read the :api:`API reference for security/userGroupServices <usergroupservices.yaml>`.

View a User/Group Service
-------------------------

*Request*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/usergroupservices/default' \
    --header 'Accept: application/xml'

*Response (XML)* ::

    <?xml version="1.0" encoding="UTF-8"?>
    <org.geoserver.security.xml.XMLUserGroupServiceConfig>
      <name>default</name>
      <className>org.geoserver.security.xml.XMLUserGroupService</className>
      <fileName>default.xml</fileName>
      <passwordEncoderName>plainTextPasswordEncoder</passwordEncoderName>
      <passwordPolicyName>default</passwordPolicyName>
    </org.geoserver.security.xml.XMLUserGroupServiceConfig>

*Request*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/usergroupservices/default' \
    --header 'Accept: application/json'

*Response (JSON)* ::

    {
      "org.geoserver.security.xml.XMLUserGroupServiceConfig": {
        "name": "default",
        "className": "org.geoserver.security.xml.XMLUserGroupService",
        "fileName": "default.xml",
        "passwordEncoderName": "plainTextPasswordEncoder",
        "passwordPolicyName": "default"
      }
    }

List User/Group Services
------------------------

*Request*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/usergroupservices' \
    --header 'Accept: application/xml'

*Response (XML)* ::

    <?xml version="1.0" encoding="UTF-8"?>
    <userGroupService>
      <userGroupService>
        <name>default</name>
        <className>org.geoserver.security.xml.XMLUserGroupService</className>
      </userGroupService>
      <!-- ... possibly more entries ... -->
    </userGroupService>

*Request*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/usergroupservices' \
    --header 'Accept: application/json'

*Response (JSON)* ::

    {
      "userGroupService": [
        { "name": "default", "className": "org.geoserver.security.xml.XMLUserGroupService" }
      ]
    }

Create a User/Group Service
---------------------------

To create an **XML-backed** user/group service you must provide a `fileName` and the usual password encoder/policy.

*Request (XML)*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/usergroupservices' \
    --header 'Content-Type: application/xml' \
    --header 'Accept: application/xml' \
    --data-binary @- <<'XML'
    <org.geoserver.security.xml.XMLUserGroupServiceConfig>
      <name>users1</name>
      <className>org.geoserver.security.xml.XMLUserGroupService</className>
      <fileName>users1.xml</fileName>
      <passwordEncoderName>plainTextPasswordEncoder</passwordEncoderName>
      <passwordPolicyName>default</passwordPolicyName>
    </org.geoserver.security.xml.XMLUserGroupServiceConfig>
    XML

*Response*

- **201 Created** (some builds may return **200 OK**).

*Request (JSON)*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/usergroupservices' \
    --header 'Content-Type: application/json' \
    --header 'Accept: application/json' \
    --data-raw '{
      "org.geoserver.security.xml.XMLUserGroupServiceConfig": {
        "name": "users2",
        "className": "org.geoserver.security.xml.XMLUserGroupService",
        "fileName": "users2.xml",
        "passwordEncoderName": "plainTextPasswordEncoder",
        "passwordPolicyName": "default"
      }
    }'

*Response*

- **201 Created** (some builds may return **200 OK**).

Update a User/Group Service
---------------------------

The payload **name** must match the path parameter. A mismatch is a **400 Bad Request**.

*Request (XML)*

.. admonition:: curl

    curl --location --request PUT 'http://localhost:8080/geoserver/rest/security/usergroupservices/users1' \
    --header 'Content-Type: application/xml' \
    --header 'Accept: application/xml' \
    --data-binary @- <<'XML'
    <org.geoserver.security.xml.XMLUserGroupServiceConfig>
      <name>users1</name>
      <className>org.geoserver.security.xml.XMLUserGroupService</className>
      <fileName>users1.xml</fileName>
      <passwordEncoderName>digestPasswordEncoder</passwordEncoderName>
      <passwordPolicyName>default</passwordPolicyName>
    </org.geoserver.security.xml.XMLUserGroupServiceConfig>
    XML

*Response*

- **200 OK** (updated).

*Request (JSON)*

.. admonition:: curl

    curl --location --request PUT 'http://localhost:8080/geoserver/rest/security/usergroupservices/users2' \
    --header 'Content-Type: application/json' \
    --header 'Accept: application/json' \
    --data-raw '{
      "org.geoserver.security.xml.XMLUserGroupServiceConfig": {
        "name": "users2",
        "className": "org.geoserver.security.xml.XMLUserGroupService",
        "fileName": "users2.xml",
        "passwordEncoderName": "plainTextPasswordEncoder",
        "passwordPolicyName": "default"
      }
    }'

*Response*

- **200 OK** (updated).

Delete a User/Group Service
---------------------------

*Request*

.. admonition:: curl

    curl --location --request DELETE 'http://localhost:8080/geoserver/rest/security/usergroupservices/users1' \
    --header 'Accept: application/xml'

*Response*

- **200 OK** when deleted, **404 Not Found** if unknown, **410 Gone** for a previously deleted name.

Error Handling
--------------

- **400 Bad Request** for duplicate names, name mismatch on update, or missing required fields (e.g., ``fileName`` for XMLUserGroupService).
- **404 Not Found** when the named service does not exist.
- **405 Method Not Allowed** if an unsupported method is attempted.
- **500 Internal Server Error** may be returned by older builds when validation errors bubble up.
