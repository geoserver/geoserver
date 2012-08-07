.. _rest.seed:

Seeding and truncating through the REST API
===========================================

The REST API for cache seeding and truncation provides a RESTful interface through which clients can 
programatically add or remove tiles from the cache, on a layer by layer basis.

Operations
----------

``/gwc/rest/seed/<layer>.<format>``

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

*Representations*:

- :download:`XML <representations/seed_xml.txt>`
- :download:`JSON <representations/seed_json.txt>`


Seed/Truncate cURL Examples
---------------------------

The examples in this section use the `cURL <http://curl.haxx.se/>`_
utility, which is a handy command line tool for executing HTTP requests and 
transferring files. Though cURL is used the examples apply to any HTTP-capable
tool or library.

Seeding XML example
+++++++++++++++++++

Sample request:

.. code-block:: xml 

 curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d '<seedRequest><name>nurc:Arc_Sample</name><srs><number>4326</number></srs><zoomStart>1</zoomStart><zoomStop>12</zoomStop><format>image/png</format><type>truncate</type><threadCount>2</threadCount></seedRequest>'  "http://localhost:8080/geoserver/gwc/rest/seed/nurc:Arc_Sample.xml"
 
Sample response:

.. code-block:: xml 

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


Here's a more complete xml fragment for a seed request, including parameter filters:

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
   <!-- This will be reduced to 3, since the layer is only defined for 0-3 -->
   <zoomStop>2</zoomStop>
   <format>image/png</format>
 
   <!-- type can be * seed (add tiles) * reseed (replace tiles) * truncate (remove tiles) -->
   <type>truncate</type> 

   <!-- Number of seeding threads to run in parallel. 
        If type == truncate only one thread will be used regardless of this parameter -->
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


Truncate JSON example
+++++++++++++++++++++

Sample request:

.. code-block:: xml 

 curl -v -u admin:geoserver -XPOST -H "Content-type: application/json" -d "{'seedRequest':{'name':'topp:states','bounds':{'coords':{ 'double':['-124.0','22.0','66.0','72.0']}},'srs':{'number':4326},'zoomStart':1,'zoomStop':12,'format':'image\/png','type':'truncate','threadCount':4}}}"  "http://localhost:8080/geoserver/gwc/rest/seed/nurc:Arc_Sample.json"
 
Sample response:

.. code-block:: xml 

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


Querying the running tasks
==========================

Operations
----------

``/gwc/rest/seed[/<layer>].json``

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

Getting the current state of the seeding threads
++++++++++++++++++++++++++++++++++++++++++++++++

Sending a GET request to the ``/gwc/rest/seed.json`` resource returns a list of pending (scheduled) and running
tasks for all the layers.

Sending a GET reques to the ``/gwc/rest/seed/<layer name>.json`` resource returns a list of pending (scheduled) and running
tasks for that specific layer.

The returned content is a JSON array of the form:

.. code-block:: xml 

   {"long-array-array":[[<long>,<long>,<long>,<long>,<long>],...]}

If there are no pending or running tasks, the returned array is empty:

.. code-block:: xml 

   {"long-array-array":[]}
   
The returned array of arrays contains one array per seeding/truncate Task.
The meaning of each long value in each thread array is: ``[tiles processed, total # of tiles to process, # of remaining tiles, Task ID, Task status]``.
The meaning of the ``Task status`` field is:
-1 = ABORTED, 
0 = PENDING, 
1 = RUNNING, 
2 = DONE.

Sample request:

.. code-block:: xml 

  curl -u <user>:<password> -v -XGET http://localhost:8080/geoserver/gwc/rest/seed/topp:states.json

Sample response:

.. code-block:: xml 

   {"long-array-array":[[17888,44739250,18319,1,1],[17744,44739250,18468,2,1],[16608,44739250,19733,3,0],[0,1000,1000,4,1]]}
  
In the sample response above tasks ``1`` and ``2``  for the ``topp:states`` layer are running, and
tasks ``3`` and ``4`` are in pending state waiting for an available thread:


Sample request:

.. code-block:: xml 

   curl -u <user>:<password> -XGET http://localhost:8080/geoserver/gwc/rest/seed.json

Sample response:

.. code-block:: xml 

   {"long-array-array":[[2240,327426,1564,2,1],[2368,327426,1477,3,1],[2272,327426,1541,4,1],[2176,327426,1611,5,1],[1056,15954794690,79320691,6,1],[1088,15954794690,76987729,7,1],[1040,15954794690,80541010,8,1],[1104,15954794690,75871965,9,1]]}
  
The sample response response above contains the list of tasks for all the layers.


Terminating running tasks
=========================

Operations
----------

``/gwc/rest/seed[/<layer>]``

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


A POST request to the ``/gwc/rest/seed`` resource terminates pending and/or running tasks for any layer.

A POST request to the ``/gwc/rest/seed/<layer name>`` resource terminates pending and/or running tasks for that specific layer.

In order to indicate whether to terminate pending and/or running tasks, the form parameter ``"kill_all"`` needs to be specified,
with one of the following values: ``all``, ``running``, ``pending`` (for backwards compatibility, the kill_all parameter
value ``1`` is also accepted and equivalent to ``running``).

For example: ``curl -d "kill_all=all" <host>/rest/seed`` kills both pending and running tasks for any layer,
``curl -d "kill_all=all" <host>/rest/seed/topp:states`` kills only pending tasks for the ``topp:states`` layer, and so on.
 
The following request terminates all running seed and truncate tasks.

Sample request:

.. code-block:: xml 

 curl -v -u admin:geoserver -d "kill_all=all"  "http://localhost:8080/geoserver/gwc/rest/seed"
 
Sample response:

.. code-block:: xml 

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


