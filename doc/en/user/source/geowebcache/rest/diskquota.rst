.. _rest.diskquota:

Disk Quota REST API
===================

The REST API for Disk Quota management provides a RESTful interface through which clients can 
configure the disk usage limits and expiration policies for a GeoWebCache instance through simple HTTP calls.

Operations
----------

``/gwc/rest/diskquota.<format>``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - Return the global disk quota configuration
     - 200
     - XML, JSON
   * - POST
     -
     - 405
     -
   * - PUT
     - Modify the global disk quota configuration
     - 200
     - XML, JSON
   * - DELETE
     -
     - 405
     -

*Representations*:

- :download:`XML <representations/diskquota_xml.txt>`
- :download:`JSON <representations/diskquota_json.txt>`


Disk quota cURL Examples
------------------------

The examples in this section use the `cURL <http://curl.haxx.se/>`_
utility, which is a handy command line tool for executing HTTP requests and 
transferring files. Though cURL is used the examples apply to any HTTP-capable
tool or library.

Getting the current Disk Quota configuration
++++++++++++++++++++++++++++++++++++++++++++

The following obtains the current disk quota configuration in XML format:

.. code-block:: xml

  curl -u admin:geoserver -v -XGET http://localhost:8080/geoserver/gwc/rest/diskquota.xml

The response should look like:

.. code-block:: xml

	< HTTP/1.1 200 OK
	< Date: Mon, 21 Mar 2011 13:50:49 GMT
	< Server: Noelios-Restlet-Engine/1.0..8
	< Content-Type: text/xml; charset=ISO-8859-1
	< Content-Length: 422
	< 
	<gwcQuotaConfiguration>
	  <enabled>true</enabled>
	  <diskBlockSize>2048</diskBlockSize>
	  <cacheCleanUpFrequency>5</cacheCleanUpFrequency>
	  <cacheCleanUpUnits>SECONDS</cacheCleanUpUnits>
	  <maxConcurrentCleanUps>5</maxConcurrentCleanUps>
	  <globalExpirationPolicyName>LRU</globalExpirationPolicyName>
	  <globalQuota>
	    <value>100</value>
	    <units>MiB</units>
	  </globalQuota>
	  <layerQuotas/>
	</gwcQuotaConfiguration>

The following obtains the current disk quota configuration in JSON format:

.. code-block:: xml

  curl -u admin:geoserver -v -XGET http://localhost:8080/geoserver/gwc/rest/diskquota.json

The response should look like:

.. code-block:: xml

	< HTTP/1.1 200 OK
	< Date: Mon, 21 Mar 2011 13:53:42 GMT
	< Server: Noelios-Restlet-Engine/1.0..8
	< Content-Type: application/json; charset=ISO-8859-1
	< Content-Length: 241
	< 
	* Connection #0 to host localhost left intact
	* Closing connection #0
	{"gwcQuotaConfiguration":{"diskBlockSize":2048,"enabled":true,"maxConcurrentCleanUps":5,"cacheCleanUpFrequency":5,"globalExpirationPolicyName":"LRU","globalQuota":{"value":"100","units":"MiB"},"cacheCleanUpUnits":"SECONDS"}}


Changing Disk Quota configuration
+++++++++++++++++++++++++++++++++

Request body for PUT can contain only the desired properties to be modified, and a diff will be applied to the current configuration. For example:

The following will only change the maxConcurrentCleanups property in XML format:

.. code-block:: xml

  <gwcQuotaConfiguration><maxConcurrentCleanUps>2</maxConcurrentCleanUps></gwcQuotaConfiguration>

The following will only change the diskBlockSize, enabled, and globalQuota properties in JSON format:

.. code-block:: xml

  {"gwcQuotaConfiguration":{"diskBlockSize":2048,"enabled":true,"globalQuota":{"value":"100","units":"MiB"}}

(valid values for "units" are <B|KiB|MiB|GiB|TiB>)

Invalid XML request:
^^^^^^^^^^^^^^^^^^^^
Invalid parameter (here maxConcurrentCleanUps must be > 0) produce a 400 response code and contains the error message as plain text: 

.. code-block:: xml

  curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.xml" -X PUT -d "<gwcQuotaConfiguration><maxConcurrentCleanUps>-1</maxConcurrentCleanUps></gwcQuotaConfiguration>"

.. code-block:: xml

	< HTTP/1.1 400 Bad Request
	< Date: Fri, 18 Mar 2011 20:53:26 GMT
	< Server: Noelios-Restlet-Engine/1.0..8
	< Content-Type: text/plain; charset=ISO-8859-1
	< Content-Length: 53
	< 
	* Connection #0 to host localhost left intact
	* Closing connection #0
	maxConcurrentCleanUps shall be a positive integer: -1

Invalid JSON request:
^^^^^^^^^^^^^^^^^^^^^

.. code-block:: xml

  curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.json" -X PUT -d "{"gwcQuotaConfiguration":{"globalQuota":{"value":"100","units":"ZZiB"}}}"

.. code-block:: xml

	< HTTP/1.1 400 Bad Request
	< Date: Fri, 18 Mar 2011 20:56:23 GMT
	< Server: Noelios-Restlet-Engine/1.0..8
	< Content-Type: text/plain; charset=ISO-8859-1
	< Content-Length: 601
	< 
	No enum const class org.geowebcache.diskquota.storage.StorageUnit.ZZiB : No enum const class org.geowebcache.diskquota.storage.StorageUnit.ZZiB
	---- Debugging information ----
	message             : No enum const class org.geowebcache.diskquota.storage.StorageUnit.ZZiB
	cause-exception     : java.lang.IllegalArgumentException
	cause-message       : No enum const class org.geowebcache.diskquota.storage.StorageUnit.ZZiB
	class               : org.geowebcache.diskquota.DiskQuotaConfig
	required-type       : org.geowebcache.diskquota.storage.Quota
	line number         : -1
	* Connection #0 to host localhost left intact
	* Closing connection #0

Valid XML requests:
^^^^^^^^^^^^^^^^^^^
(note upon successfully applying the changes the full config in the given format is returned)

Change enabled and globalQuota in XML format:

.. code-block:: xml

  curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.xml" -X PUT -d "<gwcQuotaConfiguration><enabled>true</enabled><globalQuota><value>100</value><units>GiB</units></globalQuota></gwcQuotaConfiguration>"

.. code-block:: xml

	< HTTP/1.1 200 OK
	< Date: Fri, 18 Mar 2011 20:59:31 GMT
	< Server: Noelios-Restlet-Engine/1.0..8
	< Content-Type: text/xml; charset=ISO-8859-1
	< Content-Length: 422
	< 
	<gwcQuotaConfiguration>
	  <enabled>true</enabled>
	  <diskBlockSize>2048</diskBlockSize>
	  <cacheCleanUpFrequency>5</cacheCleanUpFrequency>
	  <cacheCleanUpUnits>SECONDS</cacheCleanUpUnits>
	  <maxConcurrentCleanUps>5</maxConcurrentCleanUps>
	  <globalExpirationPolicyName>LFU</globalExpirationPolicyName>
	  <globalQuota>
	    <value>100</value>
	    <units>GiB</units>
	  </globalQuota>
	  <layerQuotas/>
	</gwcQuotaConfiguration>

Valid JSON request:
^^^^^^^^^^^^^^^^^^^
Change globalQuota and expirationPolicyName in JSON format:

.. code-block:: xml

  curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.json" -X PUT -d "{"gwcQuotaConfiguration":{"globalQuota":{"value":"100","units":"MiB"},"globalExpirationPolicyName":"LRU"}}"

.. code-block:: xml

	< HTTP/1.1 200 OK
	< Date: Fri, 18 Mar 2011 21:02:20 GMT
	< Server: Noelios-Restlet-Engine/1.0..8
	< Content-Type: application/json; charset=ISO-8859-1
	< Content-Length: 241
	< 
	* Connection #0 to host localhost left intact
	* Closing connection #0
	{"gwcQuotaConfiguration":{"diskBlockSize":2048,"enabled":true,"maxConcurrentCleanUps":5,"cacheCleanUpFrequency":5,"globalExpirationPolicyName":"LRU","globalQuota":{"value":"100","units":"MiB"},"cacheCleanUpUnits":"SECONDS","layerQuotas":[]}}


