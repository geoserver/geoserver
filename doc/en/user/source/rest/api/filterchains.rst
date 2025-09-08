.. _rest_api_filterchains:

Filter Chains
==============

.. _security_filterchains:

``/security/filterChains``
--------------------------

Adds or Lists the filter chains in the geoserver systems

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all filter chains in the system
     - 200,403,500
     - XML, JSON
     -
   * - POST
     - Create a new filter chain
     - 200,400,403,500
     - XML, JSON
     -

Formats:

**XML**

For Get (List - Response)

.. code-block:: xml

    <filterChains>
        <filterChain>
            <name>web-test-2</name>
            <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/security/filterChains/web-test-2.xml" type="application/atom+xml"/>
        </filterChain>
        <filterChain>
            <name>web-test-5</name>
            <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/security/filterChains/web-test-5.xml" type="application/atom+xml"/>
        </filterChain>
    </fiterChains>



For Post (Create - Request)

Content-Type: application/xml
Authentication: XXXXXX

.. code-block:: xml

    <filterChain>
        <name>web-test-2</name>
        <className>org.geoserver.security.HtmlLoginFilterChain</className>
        <patterns>
            <string>/web/**</string>
            <string>/gwc/rest/web/**</string>
            <string>/</string>
        </patterns>
        <filters>
            <string>rememberme</string>
            <string>form</string>\
            <string>anonymous</string>
        </filters>
        <disabled>false</disabled>
        <allowSessionCreation>true</allowSessionCreation>
        <requireSSL>false</requireSSL>
        <matchHTTPMethod>false</matchHTTPMethod>
        <position>0</position>
    </filterChain>

For Post (Create - Response)

201 Created
Content-Type: text/plain
Location: "http://localhost:9002/geoserver/rest/security/filterChains/web-test-2"

**JSON**

For Get (list)

.. code-block:: json

    {
        "filterChains": {
            "filterChain": [
                {
                    "name": "web-test-2",
                    "href": "http://localhost:8080/geoserver/rest/security/filterChains/web-test-2.json"
                },
                {
                    "name": "web-test-5",
                    "href": "http://localhost:8080/geoserver/rest/security/filterChains/web-test-5.json"
                }
            ]
        }
    }


For Post (create - request)

.. code-block:: json

    {
        "filterChain": {
            "name": "rest",
            "className": "org.geoserver.security.ServiceLoginFilterChain",
            "patterns": {
                "string": [
                    "/rest.*",
                    "/rest/**"
                ]
            },
            "filters": {
                "string": [
                    "basic",
                    "anonymous"
                ]
            },
            "disabled": false,
            "allowSessionCreation": false,
            "requireSSL": false,
            "matchHTTPMethod": false,
            "position": 6
        }
    }

For Post (create - response)

.. code-block:: json

201 Created
Content-Type: text/plain
Location: "http://localhost:9002/geoserver/rest/security/filterChains/rest"


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


.. _security_authfilters_filterchain:

``/security/filterChains/{filterChain}``
----------------------------------------

View, Update or Delete an existing auth filter


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - View the details of a filter chain on the geoserver
     - 200,403,404,500
     - XML, JSON
     -
   * - PUT
     - Update the details of a filter chain on the geoserver
     - 200,400,403,404,500
     - XML, JSON
     -
   * - DELETE
     - Delete a filter chain on the geoserver
     - 200,403,410,500
     -
     -


Formats:

**XML**

Request GET: http://localhost:9002/geoserver/rest/security/filterChains/web-test-1
Header Accept: application/xml

.. code-block:: xml

    <filterChain>
        <name>web-test-1</name>
        <className>org.geoserver.security.HtmlLoginFilterChain</className>
        <patterns>
            <string>/web/**</string>
            <string>/gwc/rest/web/**</string>
            <string>/</string>
        </patterns>
        <filters>
            <string>rememberme</string>
            <string>form</string>
            <string>anonymous</string>
        </filters>
        <disabled>false</disabled>
        <allowSessionCreation>true</allowSessionCreation>
        <requireSSL>false</requireSSL>
        <matchHTTPMethod>false</matchHTTPMethod>
        <position>1</position>
    </filterChain>

Request PUT: http://localhost:9002/geoserver/rest/security/filterChains/web-test-1
Header Content-Type: application/xml
Header Accept: application/xml

.. code-block:: xml

    <filterChain>
        <name>web-test-1</name>
        <className>org.geoserver.security.HtmlLoginFilterChain</className>
        <patterns>
            <string>/web/**</string>
            <string>/gwc/rest/web/**</string>
            <string>/</string>
        </patterns>
        <filters>
            <string>rememberme</string>
            <string>form</string>
            <string>anonymous</string>
        </filters>
        <disabled>false</disabled>
        <allowSessionCreation>true</allowSessionCreation>
        <requireSSL>false</requireSSL>
        <matchHTTPMethod>false</matchHTTPMethod>
        <position>1</position>
    </filterChain>

Response
Status: 200

.. code-block:: xml

    <filterChain>
        <name>web-test-1</name>
        <className>org.geoserver.security.HtmlLoginFilterChain</className>
        <patterns>
            <string>/web/**</string>
            <string>/gwc/rest/web/**</string>
            <string>/</string>
        </patterns>
        <filters>
            <string>rememberme</string>
            <string>form</string>
            <string>anonymous</string>
        </filters>
        <disabled>false</disabled>
        <allowSessionCreation>true</allowSessionCreation>
        <requireSSL>false</requireSSL>
        <matchHTTPMethod>false</matchHTTPMethod>
        <position>1</position>
    </filterChain>

Request DELETE: http://localhost:9002/geoserver/rest/security/filterChains/web-test-1

Response:
Status: 200

.. code-block:: xml

    <filterChain>
        <name>web-test-1</name>
        <className>org.geoserver.security.HtmlLoginFilterChain</className>
        <patterns>
            <string>/web/**</string>
            <string>/gwc/rest/web/**</string>
            <string>/</string>
        </patterns>
        <filters>
            <string>rememberme</string>
            <string>form</string>
            <string>anonymous</string>
        </filters>
        <disabled>false</disabled>
        <allowSessionCreation>true</allowSessionCreation>
        <requireSSL>false</requireSSL>
        <matchHTTPMethod>false</matchHTTPMethod>
        <position>1</position>
    </filterChain>

**JSON**

Request GET: http://localhost:9002/geoserver/rest/security/filterChains/web-test-2
Header Accept: application/json

Response
Status: 200

.. code-block:: json

    {
        "filterChain": {
            "name": "web-test-2",
            "className": "org.geoserver.security.HtmlLoginFilterChain",
            "patterns": {
                "string": [
                    "/web/**",
                    "/gwc/rest/web/**",
                    "/"
                ]
            },
            "filters": {
                "string": [
                    "rememberme",
                    "form",
                    "anonymous"
                ]
            },
            "disabled": false,
            "allowSessionCreation": true,
            "requireSSL": false,
            "matchHTTPMethod": false,
            "position": 0
        }
    }

Request PUT: http://localhost:9002/geoserver/rest/security/filterChains/web-test-2
Header Content-Type: application/json
Header Accept: application/json

.. code-block:: json

    {
        "filterChain": {
            "name": "web-test-2",
            "className": "org.geoserver.security.HtmlLoginFilterChain",
            "patterns": {
                "string": [
                    "/web/**",
                    "/gwc/rest/web/**",
                    "/"
                ]
            },
            "filters": {
                "string": [
                    "rememberme",
                    "form",
                    "anonymous"
                ]
            },
            "disabled": false,
            "allowSessionCreation": true,
            "requireSSL": false,
            "matchHTTPMethod": false,
            "position": 0
        }
    }

Response
Status: 200

.. code-block:: json

    {
        "filterChain": {
            "name": "web-test-2",
            "className": "org.geoserver.security.HtmlLoginFilterChain",
            "patterns": {
                "string": [
                    "/web/**",
                    "/gwc/rest/web/**",
                    "/"
                ]
            },
            "filters": {
                "string": [
                    "rememberme",
                    "form",
                    "anonymous"
                ]
            },
            "disabled": false,
            "allowSessionCreation": true,
            "requireSSL": false,
            "matchHTTPMethod": false,
            "position": 0
        }
    }

Request DELETE: http://localhost:9002/geoserver/rest/security/filterChains/web-test-2

Response:
Status: 200

.. code-block:: json

    {
        "filterChain": {
            "name": "web-test-2",
            "className": "org.geoserver.security.HtmlLoginFilterChain",
            "patterns": {
                "string": [
                    "/web/**",
                    "/gwc/rest/web/**",
                    "/"
                ]
            },
            "filters": {
                "string": [
                    "rememberme",
                    "form",
                    "anonymous"
                ]
            },
            "disabled": false,
            "allowSessionCreation": true,
            "requireSSL": false,
            "matchHTTPMethod": false,
            "position": 0
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
   * - Authentication filter not found
     - 404
   * - Gone - On Delete Only
     - 410
   * - Internal Server Error
     - 500
