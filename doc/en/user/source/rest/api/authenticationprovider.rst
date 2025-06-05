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
     - Default Format
   * - GET
     - List all auth providers in the system
     - 200,403,500
     - XML, JSON
     -
   * - POST
     - Great a new authProvider
     - 200,400,403,500
     - XML, JSON
     -

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
        <name>Test_Provider13</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>1</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

For Post (Create - Response)

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

For Post (create - response)

.. code-block:: json

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

**JSON**

For PUT and GET

.. code-block:: json

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
   * - Gone - On Delete Only
     - 410
   * - Internal Server Error
     - 500