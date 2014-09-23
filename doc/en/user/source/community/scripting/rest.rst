.. _scripting_rest:

Scripting Rest API
==================

Like other modules in GeoServer, you can add, update, read, and delete scripts using a restful interface.

.. warning:: 

    The scripting rest API will not work until you have changed the GeoServer default admin password. 

WPS Scripts
-----------

``/scripts/wps[.<format>]``
---------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - List all scripts
      - 200
      - HTML, XML, JSON
      - HTML

List WPS Scripts
----------------
curl -u username:password -XGET -H "Accept: text/xml" http://localhost:8080/geoserver/rest/scripts/wps

``/scripts/wps/<script.ext>``
-----------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - Get the contents of a script
      - 200
      - Text
      - Text
    * - PUT
      - Add a new script
      - 200
      - Text
      - Text
    * - PUT
      - Update an existing script
      - 200
      - Text
      - Text
    * - DELETE
      - Delete an existing script
      - 200
      - 
      -

Get a WPS Script
----------------
curl -u username:password -XGET -H "Accept: text/xml" http://localhost:8080/geoserver/rest/scripts/wps/buffer.groovy

Add a WPS Script
----------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @buffer.groovy http://localhost:8080/geoserver/rest/scripts/wps/buffer.groovy

Update a WPS Script
-------------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @buffer.groovy http://localhost:8080/geoserver/rest/scripts/wps/buffer.groovy

Delete a WPS Script
-------------------
curl -u username:password -XDELETE http://localhost:8080/geoserver/rest/scripts/wps/buffer.groovy

Filter Function Scripts
-----------------------

``/scripts/function[.<format>]``
--------------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - List all scripts
      - 200
      - HTML, XML, JSON
      - HTML

List Function Scripts
---------------------
curl -u username:password -XGET -H "Accept: text/xml" http://localhost:8080/geoserver/rest/scripts/function

``/scripts/function/<script.ext>``
----------------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - Get the contents of a script
      - 200
      - Text
      - Text
    * - PUT
      - Add a new script
      - 200
      - Text
      - Text
    * - PUT
      - Update an existing script
      - 200
      - Text
      - Text
    * - DELETE
      - Delete an existing script
      - 200
      - 
      -

Get a Function Script
---------------------
curl -u username:password -XGET -H "Accept: text/xml" http://localhost:8080/geoserver/rest/scripts/function/bufferedCentroid.groovy


Add a Function Script
---------------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @bufferedCentroid.groovy http://localhost:8080/geoserver/rest/scripts/function/bufferedCentroid.groovy

Update a Function Script
------------------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @bufferedCentroid.groovy http://localhost:8080/geoserver/rest/scripts/function/bufferedCentroid.groovy

Delete a Function Script
------------------------
curl -u username:password -XDELETE http://localhost:8080/geoserver/rest/scripts/function/bufferedCentroid.groovy

WFS Transaction Scripts
-----------------------

``/scripts/wfs/tx[.<format>]``
------------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - List all scripts
      - 200
      - HTML, XML, JSON
      - HTML

List WFSTX Scripts
------------------
curl -u username:password -XGET -H "Accept: text/json" http://localhost:8080/geoserver/rest/scripts/wfs/tx

``/scripts/wfs/tx/<script.ext>``
--------------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - Get the contents of a script
      - 200
      - Text
      - Text
    * - PUT
      - Add a new script
      - 200
      - Text
      - Text
    * - PUT
      - Update an existing script
      - 200
      - Text
      - Text
    * - DELETE
      - Delete an existing script
      - 200
      - 
      -

Get a WFSTX Script
------------------
curl -u username:password -XGET -H "Accept: text/xml" http://localhost:8080/geoserver/rest/scripts/wfs/tx/check.groovy

Add a WFSTX Script
------------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @check.groovy http://localhost:8080/geoserver/rest/scripts/wfs/tx/check.groovy

Update a WFSTX Script
---------------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @check.groovy http://localhost:8080/geoserver/rest/scripts/wfs/tx/check.groovy

Delete a WFSTX Script
---------------------
curl -u username:password -XDELETE http://localhost:8080/geoserver/rest/scripts/wfs/tx/check.groovy

Application Scripts
-------------------

``/scripts/apps/[.<format>]``
-----------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - List all scripts
      - 200
      - HTML, XML, JSON
      - HTML

List App 
--------
curl -u username:password -XGET -H "Accept: text/xml" http://localhost:8080/geoserver/rest/scripts/apps

``/scripts/apps/<name>/main.<ext>``
-----------------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - Get the contents of a script
      - 200
      - Text
      - Text
    * - PUT
      - Add a new script
      - 200
      - Text
      - Text
    * - PUT
      - Update an existing script
      - 200
      - Text
      - Text
    * - DELETE
      - Delete an existing script
      - 200
      - 
      -

Get an App 
----------
curl -u username:password -XGET -H "Accept: text/xml" http://localhost:8080/geoserver/rest/scripts/apps/buffer/main.groovy

Add a App Script
----------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @app_buffer.groovy http://localhost:8080/geoserver/rest/scripts/apps/buffer/main.groovy

Update a App Script
-------------------
curl -u username:password -XPUT -H "Content-type: text/plain" --data-binary @app_buffer.groovy http://localhost:8080/geoserver/rest/scripts/apps/buffer/main.groovy

Delete a Add Script
-------------------
curl -u username:password -XDELETE http://localhost:8080/geoserver/rest/scripts/apps/buffer/main.groovy

Scripting Sessions
------------------

``/scripts/sessions[.<format>]``
--------------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - List all scripts
      - 200
      - JSON
      - JSON

List Scripting Sessions
-----------------------
curl -u username:password -XGET -H "Accept: text/json" http://localhost:8080/geoserver/rest/sessions

``/scripts/sessions/<language>/<id>``
-------------------------------------

.. list-table::
    :header-rows: 1
    
    * - Method
      - Action
      - Status code
      - Formats
      - Default Format
    * - GET
      - Get the scripting session
      - 200
      - JSON
      - JSON
    * - POST
      - Create a scripting session
      - 200
      - TEXT
      - TExT
    * - PUT
      - Run a script
      - 200
      - Text
      - Text

Get a Scripting Session
-----------------------
curl -u username:password -XGET -H "Accept: text/json" http://localhost:8080/geoserver/rest/sessions/groovy/0

Create a Scripting Session
--------------------------
curl -u username:password -XPOST http://localhost:8080/geoserver/rest/sessions/groovy

Run a Script in a Session
-------------------------
curl -u username:password -XPUT --data-binary @script.groovy http://localhost:8080/geoserver/rest/sessions/groovy/0

