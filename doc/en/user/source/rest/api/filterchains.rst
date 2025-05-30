.. _rest_api_filterchains:

Filter Chains
==============

.. _security_filterchains:

``/security/filterChains``
----------------------------------

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

    <filterChainList>
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
            <position>0</position>
        </filterChain>
    </filterChainList>


For Post (Create - Request)

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
            <string>form</string>
            <string>anonymous</string>
        </filters>
        <disabled>false</disabled>
        <allowSessionCreation>true</allowSessionCreation>
        <requireSSL>false</requireSSL>
        <matchHTTPMethod>false</matchHTTPMethod>
        <position>0</position>
    </filterChain>

**JSON**

For Get (list)

.. code-block:: json

    {
        "filterChainList": {
            "filterChain": [
                {
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

.. code-block:: json

    {
        "id": "2d3ea9bb:196c91945a2:-7ffe",
        "name": "restInterceptor16",
        "config": {
            "@class": "org.geoserver.security.config.SecurityInterceptorFilterConfig",
            "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
            "allowIfAllAbstainDecisions": true,
            "securityMetadataSource": "restFilterDefinitionMap"
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


.. _security_authfilters_authfilter:

``/security/filterChains/{filterChain}``
-----------------------------------------

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
