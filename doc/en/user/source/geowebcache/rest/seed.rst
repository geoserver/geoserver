.. _gwc_rest_seed:

Seeding and Truncating
======================

The GeoWebCache REST API provides a RESTful interface through which users can add or remove tiles from the cache on a per-layer basis.

Operations
----------

URL: ``/gwc/rest/seed/<layer>.<format>``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - Return the status of the seeding threads
     - 200
     - JSON
   * - POST
     - Issue a seed or truncate task request
     - 200
     - XML, JSON
   * - PUT
     - 
     - 405
     - 
   * - DELETE
     -
     - 405
     -

**Representations**:

* :download:`XML <representations/seed_xml.txt>`
* :download:`JSON <representations/seed_json.txt>`

The examples below use the `cURL <http://curl.haxx.se/>`_ tool, though the examples apply to any HTTP-capable tool or library.

Seeding
~~~~~~~

The following XML request initiates a seeding task:

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d '<seedRequest><name>nurc:Arc_Sample</name><srs><number>4326</number></srs><zoomStart>1</zoomStart><zoomStop>12</zoomStop><format>image/png</format><type>truncate</type><threadCount>2</threadCount></seedRequest>'  "http://localhost:8080/geoserver/gwc/rest/seed/nurc:Arc_Sample.xml"
 
::

   * About to connect() to localhost port 8080 (#0)
   *   Trying 127.0.0.1... connected
   * Connected to localhost (127.0.0.1) port 8080 (#0)
   * Server auth using Basic with user 'admin'
   > POST /geoserver/gwc/rest/seed/nurc:Arc_Sample.xml HTTP/1.1
   > Authorization: Basic YWRtaW46Z2Vvc2VydmVy
   > User-Agent: curl/7.21.3 (x86_64-pc-linux-gnu) libcurl/7.21.3 OpenSSL/0.9.8o zlib/1.2.3.4 libidn/1.18
   > Host: localhost:8080
   > Accept: */*
   > Content-type: text/xml
   > Content-Length: 209
   > 
   < HTTP/1.1 200 OK


The following is a more complete XML fragment for a seed request, including parameter filters:

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <seedRequest>
     <name>topp:states</name>
     <bounds>
       <coords>
         <double>-2495667.977678598</double>
         <double>-2223677.196231552</double>
         <double>3291070.6104286816</double>
         <double>959189.3312465074</double>
       </coords>
     </bounds>

     <!-- These are listed on http://localhost:8080/geoserver/gwc/demo -->
     <gridSetId>EPSG:2163</gridSetId>
     <zoomStart>0</zoomStart>
     <zoomStop>2</zoomStop>
     <format>image/png</format>
 
     <!-- type can be seed, reseed, or truncate -->
     <type>truncate</type> 

     <!-- Number of seeding threads to run in parallel. 
          If type == truncate only one thread will be used
          regardless of this parameter -->
     <threadCount>1</threadCount>
     <!-- Parameter filters -->
     <parameters>
       <entry>
         <string>STYLES</string>
         <string>pophatch</string>
       </entry>
       <entry>
         <string>CQL_FILTER</string>
         <string>TOTPOP > 10000</string>
       </entry>
     </parameters>
   </seedRequest>


Truncating
~~~~~~~~~~

The following XML request initiates a truncating task:

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: application/json" -d "{'seedRequest':{'name':'topp:states','bounds':{'coords':{ 'double':['-124.0','22.0','66.0','72.0']}},'srs':{'number':4326},'zoomStart':1,'zoomStop':12,'format':'image\/png','type':'truncate','threadCount':4}}}"  "http://localhost:8080/geoserver/gwc/rest/seed/nurc:Arc_Sample.json"
 
::

   * About to connect() to localhost port 8080 (#0)
   *   Trying 127.0.0.1... connected
   * Connected to localhost (127.0.0.1) port 8080 (#0)
   * Server auth using Basic with user 'admin'
   > POST /geoserver/gwc/rest/seed/nurc:Arc_Sample.json HTTP/1.1
   > Authorization: Basic YWRtaW46Z2Vvc2VydmVy
   > User-Agent: curl/7.21.3 (x86_64-pc-linux-gnu) libcurl/7.21.3 OpenSSL/0.9.8o zlib/1.2.3.4 libidn/1.18
   > Host: localhost:8080
   > Accept: */*
   > Content-type: application/json
   > Content-Length: 205
   > 
   < HTTP/1.1 200 OK
   < Date: Fri, 14 Oct 2011 22:09:21 GMT
   < Server: Noelios-Restlet-Engine/1.0..8
   < Transfer-Encoding: chunked
   < 
   * Connection #0 to host localhost left intact
   * Closing connection #0


Querying running tasks
----------------------

URL: ``/gwc/rest/seed[/<layer>].json``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - Get the global or per layer state of running and pending tasks
     - 200
     - JSON
   * - POST
     - 
     - 405
     - 
   * - PUT
     - 
     - 405
     - 
   * - DELETE
     -
     - 405
     -

Getting current state of the seeding threads
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sending a GET request to the ``/gwc/rest/seed.json`` resource returns a list of pending (scheduled) and running tasks for all the layers.

Sending a GET request to the ``/gwc/rest/seed/<layer name>.json`` resource returns a list of pending (scheduled) and running tasks for that specific layer.

The returned content is a JSON array of the form::

   {"long-array-array":[[<long>,<long>,<long>,<long>,<long>],...]}

If there are no pending or running tasks, the returned array is empty::

   {"long-array-array":[]}
   
The returned array of arrays contains one array per seeding/truncating task.
The meaning of each long value in each thread array is::

  [tiles processed, total # of tiles to process, # of remaining tiles, Task ID, Task status]

The returned ``Task Status`` value will be one of the following::

  -1 = ABORTED 
   0 = PENDING
   1 = RUNNING
   2 = DONE

The example below returns the current state of tasks for the ``topp:states`` layer:

.. code-block:: console

   curl -u <user>:<password> -v -XGET http://localhost:8080/geoserver/gwc/rest/seed/topp:states.json

.. code-block:: json

   {"long-array-array":[[17888,44739250,18319,1,1],[17744,44739250,18468,2,1],[16608,44739250,19733,3,0],[0,1000,1000,4,0]]}
  
In the above response, tasks ``1`` and ``2``  for the ``topp:states`` layer are running, and
tasks ``3`` and ``4`` are in a pending state waiting for an available thread.

The example below returns a list of tasks for all the layers.

.. code-block:: console 

   curl -u <user>:<password> -XGET http://localhost:8080/geoserver/gwc/rest/seed.json

.. code-block:: json

   {"long-array-array":[[2240,327426,1564,2,1],[2368,327426,1477,3,1],[2272,327426,1541,4,1],[2176,327426,1611,5,1],[1056,15954794690,79320691,6,1],[1088,15954794690,76987729,7,1],[1040,15954794690,80541010,8,1],[1104,15954794690,75871965,9,1]]}
  

Terminating running tasks
-------------------------

URL: ``/gwc/rest/seed[/<layer>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - 
     - 405
     - 
   * - POST
     - Issue a kill running and/or pending tasks request
     - 200
     - 
   * - PUT
     - 
     - 405
     - 
   * - DELETE
     -
     - 405
     -

A POST request to the ``/gwc/rest/seed`` resource terminates pending and/or running tasks for **all layers**. A POST request to the ``/gwc/rest/seed/<layername>`` resource terminates pending and/or running tasks for a specific layer.

It is possible to terminate individual or all pending and/or running tasks. Use the parameter ``kill_all`` with one of the following values: ``running``, ``pending``, or ``all``.

.. note::  For backward compatibility, the ``kill_all`` parameter value ``1`` is also accepted and is equivalent to ``running``.

The following request terminates all running seed and truncate tasks.

.. code-block:: console 

   curl -v -u admin:geoserver -d "kill_all=all"  "http://localhost:8080/geoserver/gwc/rest/seed"
 
::

   * About to connect() to localhost port 8080 (#0)
   *   Trying 127.0.0.1... connected
   < HTTP/1.1 200 OK
   < Date: Fri, 14 Oct 2011 22:23:04 GMT
   < Server: Noelios-Restlet-Engine/1.0..8
   < Content-Type: text/html; charset=ISO-8859-1
   < Content-Length: 426
   < 
   <html>
   ...
   * Connection #0 to host localhost left intact
   * Closing connection #0

