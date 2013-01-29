.. _gwc_rest_diskquota:

Disk Quota
==========

The GeoWebCache REST API provides a RESTful interface through which users can configure the disk usage limits and expiration policies for a GeoWebCache instance.

Operations
----------

URL: ``/gwc/rest/diskquota.<format>``

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

**Representations**:

* :download:`XML <representations/diskquota_xml.txt>`
* :download:`JSON <representations/diskquota_json.txt>`

The examples below use the `cURL <http://curl.haxx.se/>`_ tool, though the examples apply to any HTTP-capable tool or library.

Retrieving the current configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following returns the current disk quota configuration in **XML** format:

.. code-block:: console

  curl -u admin:geoserver -v -XGET http://localhost:8080/geoserver/gwc/rest/diskquota.xml

::

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

The following returns the current disk quota configuration in **JSON** format:

.. code-block:: xml

  curl -u admin:geoserver -v -XGET http://localhost:8080/geoserver/gwc/rest/diskquota.json

::

   < HTTP/1.1 200 OK
   < Date: Mon, 21 Mar 2011 13:53:42 GMT
   < Server: Noelios-Restlet-Engine/1.0..8
   < Content-Type: application/json; charset=ISO-8859-1
   < Content-Length: 241
   < 
   * Connection #0 to host localhost left intact
   * Closing connection #0
   {"gwcQuotaConfiguration":{"diskBlockSize":2048,"enabled":true,"maxConcurrentCleanUps":5,"cacheCleanUpFrequency":5,"globalExpirationPolicyName":"LRU","globalQuota":{"value":"100","units":"MiB"},"cacheCleanUpUnits":"SECONDS"}}


Changing configuration
~~~~~~~~~~~~~~~~~~~~~~

.. note::

   The request body for PUT should contain only the desired properties to be modified. For example, the following will only change the maxConcurrentCleanups property in XML format:

   .. code-block:: xml

      <gwcQuotaConfiguration><maxConcurrentCleanUps>2</maxConcurrentCleanUps></gwcQuotaConfiguration>

   The following will only change the diskBlockSize, enabled, and globalQuota properties in JSON format:

   .. code-block:: json

      {"gwcQuotaConfiguration":{"diskBlockSize":2048,"enabled":true,"globalQuota":{"value":"100","units":"MiB"}}

The following XML example successfully enables the quota and sets the globalQuota size:

.. code-block:: console

  curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.xml" -X PUT -d "<gwcQuotaConfiguration><enabled>true</enabled><globalQuota><value>100</value><units>GiB</units></globalQuota></gwcQuotaConfiguration>"

::

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

The following JSON example changes the globalQuote and expirationPolicyName parameters:

.. code-block:: console

   curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.json" -X PUT -d "{"gwcQuotaConfiguration":{"globalQuota":{"value":"100","units":"MiB"},"globalExpirationPolicyName":"LRU"}}"

::

   < HTTP/1.1 200 OK
   < Date: Fri, 18 Mar 2011 21:02:20 GMT
   < Server: Noelios-Restlet-Engine/1.0..8
   < Content-Type: application/json; charset=ISO-8859-1
   < Content-Length: 241
   < 
   * Connection #0 to host localhost left intact
   * Closing connection #0
   {"gwcQuotaConfiguration":{"diskBlockSize":2048,"enabled":true,"maxConcurrentCleanUps":5,"cacheCleanUpFrequency":5,"globalExpirationPolicyName":"LRU","globalQuota":{"value":"100","units":"MiB"},"cacheCleanUpUnits":"SECONDS","layerQuotas":[]}}


The following *invalid* XML example has an invalid parameter (maxConcurrentCleanUps must be > 0). It returns a 400 response code and contains an error message as plain text: 

.. code-block:: console

   curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.xml" -X PUT -d "<gwcQuotaConfiguration><maxConcurrentCleanUps>-1</maxConcurrentCleanUps></gwcQuotaConfiguration>"

::

   < HTTP/1.1 400 Bad Request
   < Date: Fri, 18 Mar 2011 20:53:26 GMT
   < Server: Noelios-Restlet-Engine/1.0..8
   < Content-Type: text/plain; charset=ISO-8859-1
   < Content-Length: 53
   < 
   * Connection #0 to host localhost left intact
   * Closing connection #0
   maxConcurrentCleanUps shall be a positive integer: -1

The following *invalid* JSON example uses an unknown unit of measure (ZZiB). It returns a 400 response code and contains an error message as plain text: 

.. code-block:: console

   curl -v -u admin:geoserver "http://localhost:8090/geoserver/gwc/rest/diskquota.json" -X PUT -d "{"gwcQuotaConfiguration":{"globalQuota":{"value":"100","units":"ZZiB"}}}"

::

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
