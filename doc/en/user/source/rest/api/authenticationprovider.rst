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


Formats:

**XML**

For Get (List - Response)

.. code-block:: xml

        <authProviderList>
            <authProvider>
                <id>-4e7ef1a4:196d967dcea:-8000</id>
                <name>-4e7ef1a4:196d967dcea:-8000</name>
                <className>org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig</className>
                <userGroupServiceName>default</userGroupServiceName>
                <position>-1</position>
                <config class="org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig">
                    <id>-4e7ef1a4:196d967dcea:-8000</id>
                    <name>JDBC</name>
                    <className>org.geoserver.security.jdbc.JDBCConnectAuthProvider</className>
                    <driverClassName>org.postgresql.Driver</driverClassName>
                    <connectURL>Jdbc::/postgresSQl</connectURL>
                    <userGroupServiceName>default</userGroupServiceName>
                </config>
                <disabled>true</disabled>
            </authProvider>
        </authProviders>

For Post (Create - Request)

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

For Post (Create - Response)

.. code-block:: xml

    <authProvider>
        <id>4394eafb:19744df3931:-8000</id>
        <name>Test_Provider15</name>
        <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>0</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <id>4394eafb:19744df3931:-8000</id>
            <name>Test_Provider15</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

**JSON**

For Get (list)

.. code-block:: json

    {
        "authProviderList": {
            "authProvider": [
                {
                    "id": "-4e7ef1a4:196d967dcea:-8000",
                    "name": "-4e7ef1a4:196d967dcea:-8000",
                    "className": "org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig",
                    "userGroupServiceName": "default",
                    "position": -1,
                    "config": {
                        "@class": "org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig",
                        "id": "-4e7ef1a4:196d967dcea:-8000",
                        "name": "JDBC",
                        "className": "org.geoserver.security.jdbc.JDBCConnectAuthProvider",
                        "driverClassName": "org.postgresql.Driver",
                        "connectURL": "Jdbc::/postgresSQl",
                        "userGroupServiceName": "default"
                    },
                    "disabled": true
                },
            ]
        }
    }

For Post (create - request)

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


For Post (create - response)

.. code-block:: json

    {
        "authProvider": {
            "id": "4394eafb:19744df3931:-7fff",
            "name": "Test_Provider18",
            "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
            "userGroupServiceName": "default",
            "position": 0,
            "config": {
                "@class": "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
                "id": "4394eafb:19744df3931:-7fff",
                "name": "Test_Provider18",
                "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
                "userGroupServiceName": "default"
            },
            "disabled": false
        }
    }

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


Formats:

**XML**

For PUT and GET

** XML Response **

GET

.. code-block:: xml

    <authProvider>
        <id>7787843a:196d38d0a14:-7fcc</id>
        <name>7787843a:196d38d0a14:-7fcc</name>
        <className>org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>0</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <id>7787843a:196d38d0a14:-7fcc</id>
            <name>default</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

PUT:

Request:

.. code-block:: xml

    <authProvider>
        <id>-3e8020b4:1973ebc2c56:-8000</id>
        <name>Test_Provider13</name>
        <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>0</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

Response: 200

.. code-block:: xml

    <authProvider>
        <id>-3e8020b4:1973ebc2c56:-8000</id>
        <name>Test_Provider13</name>
        <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>0</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <id>-3e8020b4:1973ebc2c56:-8000</id>
            <name>Test_Provider13</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

DELETE:

HTTP Status:200

.. code-block:: xml

    <authProvider>
        <id>2373abe0:1973f07017d:-8000</id>
        <name>Test_Provider13</name>
        <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>3</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <id>2373abe0:1973f07017d:-8000</id>
            <name>Test_Provider13</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

**JSON**

GET

.. code-block:: json

    {
        "authProvider": {
            "id": "4394eafb:19744df3931:-7fff",
            "name": "Test_Provider18",
            "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
            "userGroupServiceName": "default",
            "position": 6,
            "config": {
                "@class": "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
                "id": "4394eafb:19744df3931:-7fff",
                "name": "Test_Provider18",
                "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
                "userGroupServiceName": "default"
            },
            "disabled": false
        }
    }

Delete:
Success:200

.. code-block:: json

    {
        "authProvider": {
            "id": "521a99b9:1973eb9aa52:-8000",
            "name": "Test_Provider12",
            "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
            "userGroupServiceName": "default",
            "position": 2,
            "config": {
                "@class": "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
                "id": "521a99b9:1973eb9aa52:-8000",
                "name": "Test_Provider12",
                "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
                "userGroupServiceName": "default"
            },
            "disabled": false
        }
    }

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