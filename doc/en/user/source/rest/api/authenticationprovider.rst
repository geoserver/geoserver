.. _rest_api_authproviders:

Auth Providers
==============

.. _security_authproviders:

``/security/authProviders``
----------------------------------

Adds or Lists the authentication providers in the geoserver systems


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
   * - GET
     - List all auth providers in the system
     - 200,403,404,500
     - XML, JSON
   * - POST
     - Create a new authProvider
     - 200,400,403,500
     - XML, JSON

Get
---

GET http://localhost:9002/geoserver/rest/security/authProviders
Accept: application/xml

*Response*
Status: 200
Content-Type: application/xml
Body:

.. code-block:: xml

    <authProviders>
        <authProvider>
            <name>Test_Provider10</name>
            <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider10.xml" type="application/atom+xml"/>
        </authProvider>
        <authProvider>
            <name>Test_Provider11</name>
            <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11.xml" type="application/atom+xml"/>
        </authProvider>
    </authProviders>


GET http://localhost:9002/geoserver/rest/security/authProviders
Accept: application/json

*Response*

Status: 200
Content-Type: application/json
Body:

.. code-block:: json

    {
        "authProviders": {
            "authProvider": [
                {
                    "name": "Test_Provider10",
                    "href": "http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider10.json"
                },
                {
                    "name": "Test_Provider11",
                    "href": "http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11.json"
                }
            ]
        }
    }

POST
----

*Request*
POST http://localhost:9002/geoserver/rest/security/authProviders
Content-Type: application/xml
Body:

.. code-block:: xml

    <authProvider>
        <name>Test_Provider15</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>1</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

*Response*
Status: 201
Location: http//localhost:9002/geoserver/rest/security/authProviders/Test_Provider15


*Request*
POST http://localhost:9002/geoserver/rest/security/authProviders
Content-Type: application/json
Body:

.. code-block:: json

    {
        "authProvider":
            {
                "name": "Test_Provider18",
                "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
                "userGroupServiceName": "default",
                "position": 0,
                "config": {
                    "@class": "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
                    "userGroupServiceName": "default"
                },
                "disabled": false
            }
    }

*Response*
Status: 201
Location: http//localhost:9002/geoserver/rest/security/authProviders/Test_Provider18

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - Malformed request
     - 400
   * - No administrative privileges
     - 403
   * - Internal Server Error
     - 500


.. _security_authproviders_authprovider:

``/security/authProviders/{authProvider}``
------------------------------------------

View, Update or Delete an existing auth provider


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - View the details of an authentication provider on the geoserver
     - 200,403,404,500
     - XML, JSON
     -
   * - PUT
     - Update the details of an authentication provider on the geoserver
     - 200,400,403,404,500
     - XML, JSON
     -
   * - DELETE
     - Update the details of an authentication provider on the geoserver
     - 200,403,410,500
     -
     -


GET
---

*Request*
GET: http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11
Accept: application/xml


*Response*
Status: 200 OK
Content-Type: application/xml

.. code-block:: xml

    <authProvider>
        <id>655557d7:1973eb7ba3a:-8000</id>
        <name>Test_Provider11</name>
        <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>1</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <id>655557d7:1973eb7ba3a:-8000</id>
            <name>Test_Provider11</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>


*Request*
GET: http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11
Accept: application/json


*Response*
Status: 200 OK
Content-Type: application/json
Body:

.. code-block:: json

    {
        "authProvider": {
            "id": "655557d7:1973eb7ba3a:-8000",
            "name": "Test_Provider11",
            "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
            "userGroupServiceName": "default",
            "position": 1,
            "config": {
                "@class": "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
                "id": "655557d7:1973eb7ba3a:-8000",
                "name": "Test_Provider11",
                "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
                "userGroupServiceName": "default"
            },
            "disabled": false
        }
    }

PUT
---

*Request*
PUT: http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11
Content-Type: application/xml


.. code-block:: xml

    <authProvider>
        <id>-3e8020b4:1973ebc2c56:-8000</id>
        <name>Test_Provider11</name>
        <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>0</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

*Response*
Status: 200 OK

*Request*
PUT: http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11
Content-Type: application/json
Body:

.. code-block:: json

    {
        "authProvider": {
            "id": "655557d7:1973eb7ba3a:-8000",
            "name": "Test_Provider11",
            "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
            "userGroupServiceName": "default",
            "position": 1,
            "config": {
                "@class": "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
                "id": "655557d7:1973eb7ba3a:-8000",
                "name": "Test_Provider11",
                "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
                "userGroupServiceName": "default"
            },
            "disabled": false
        }
    }

DELETE
------

DELETE:  http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11

*Response*
Status: 200

HTTP Status:200

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - Malformed request
     - 400
   * - No administrative privileges
     - 403
   * - Authentication provider not found
     - 404
   * - Internal Server Error
     - 500