.. _gwc_config:

Configuration
=============

GeoWebCache is automatically configured for use with GeoServer using the most common options, with no setup required. All communication between GeoServer and GeoWebCache happens by passing messages inside the JVM.

By default, all layers served by GeoServer will be known to GeoWebCache. See the :ref:`gwc_webadmin_layers` page to test the configuration.

.. note:: Version 2.2.0 of GeoServer introduced changes to the configuration of the integrated GeoWebCache.

Integrated user interface
-------------------------

GeoWebCache has a full integrated web-based configuration. See the :ref:`gwc_webadmin` section in the :ref:`web_admin`.

Determining tiled layers
------------------------

In versions of GeoServer prior to 2.2.0, the GeoWebCache integration was done in a such way that every GeoServer layer and layer group was forced to have an associated GeoWebCache tile layer. In addition, every such tile layer was forcedly published in the EPSG:900913 and EPSG:4326 gridsets with PNG and JPEG output formats.

It is possible to selectively turn caching on or off for any layer served through GeoServer. This setting can be configured in the :ref:`gwc_webadmin_layers` section of the :ref:`web_admin`.

Configuration files
-------------------

It is possible to configure most aspects of cached layers through the :ref:`gwc_webadmin` section in the :ref:`web_admin` or the :ref:`gwc_rest`. 

GeoWebCache keeps the configuration for each GeoServer tiled layer separately, inside the :file:`<data_dir>/gwc-layers/` directory. There is one XML file for each tile layer. These files contain a different syntax from the ``<wmsLayer>`` syntax in the standalone version and are *not* meant to be edited by hand. Instead you can configure tile layers on the :ref:`gwc_webadmin_layers` page or through the :ref:`gwc_rest`.

Configuration for the defined gridsets is saved in :file:`<data_dir>/gwc/geowebcache.xml`` so that the integrated GeoWebCache can continue to serve externally-defined tile layers from WMS services outside GeoServer.

If upgrading from a version prior to 2.2.0, a migration process is run which creates a tile layer configuration for all the available layers and layer groups in GeoServer with the old defaults. From that point on, you should configure the tile layers on the :ref:`gwc_webadmin_layers` page.


Changing the cache directory
----------------------------

GeoWebCache will automatically store cached tiles in a ``gwc`` directory inside your GeoServer data directory. To set a different directory, stop GeoServer (if it is running) and add the following code to your GeoServer :file:`web.xml` file (located in the :file:`WEB-INF` directory):

.. code-block:: xml 

   <context-param>
      <param-name>GEOWEBCACHE_CACHE_DIR</param-name>
      <param-value>C:\temp</param-value>
   </context-param>

Change the path inside ``<param-value>`` to the desired cache path (such as :file:`C:\\temp` or :file:`/tmp`). Restart GeoServer when done.

.. note:: Make sure GeoServer has write access in this directory.

GeoWebCache with multiple GeoServer instances
---------------------------------------------

For stability reasons, it is not recommended to use the embedded GeoWebCache with multiple GeoServer instances. If you want to configure GeoWebCache as a front-end for multiple instances of GeoServer, we recommend using the `standalone GeoWebCache <http://geowebcache.org>`_.

.. _gwc_data_security:

GeoServer Data Security
-----------------------

GWC Data Security is an option that can be turned on and turned off through the :ref:`gwc_webadmin_defaults` page. By default it is turned off. 

When turned on, the embedded GWC will do a data security check before calling GeoWebCache, i.e. verify whether the user actually has access to the layer, and reject the request if this is not the case. 
In the case of WMS-C requests, there is also limited support for data access limit filters, only with respect to geographic boundaries (all other types of data access limits will be ignored).
The embedded GWC will reject requests for which the requested bounding box is (partly) inaccessible. It is only possible to request a tile within a bounding box that is fully accessible.
This behaviour is different from the regular WMS, which will filter the data before serving it. 
However, if the integrated WMS/WMS-C is used, the request will be forwarded back to WMS and give the desired result.

When using the default GeoServer security system, rules cannot combine data security with service security. However, when using a security subsystem it may be possible
to make such particular combinations. In this case the WMS-C service inherits all security rules from the regular WMS service; while all other GWC services will get their security
from rules associated with the 'GWC' service itself.

Configuring In Memory Caching
------------------------------
GWC In Memory Caching is a new feature which allows to cache GWC tiles in memory reducing their access time. User can also choose to avoid to store the files on the disk if needed. 
For enabling/disabling these features the user may see the related section on the TileCaching :ref:`gwc_webadmin_defaults` page.  

Actually there are only two Caching methods:

	* Guava Caching
	* Hazelcast Caching
	
Guava Cache
+++++++++++

`Guava <https://code.google.com/p/guava-libraries/wiki/CachesExplained>`_ Cache provides a local in-memory cache to use for a single GeoServer instance. For configuring Guava Caching the user must only edit the configuration parameters in the *Caching Defaults* page.

Hazelcast Cache
+++++++++++++++

`Hazelcast <http://docs.hazelcast.org/docs/3.3/manual/html/>`_ is an open-source API for distributed structures like clusters. GWC supports this API for creating a distributed in memory cache. 
At the time of writing, Hazelcast requires the installation of the *gs-gwc-distributed* plugin in the `WEB_INF/lib` directory of your geoserver application. This plugin can be found at the following `link <https://build.geoserver.org/geoserver/master/community-latest/>`_.

There are 2 ways for configuring distributed caching in GWC:

	* Using an XML file called *hazelcast.xml*. This file must be located in a directory indicated by the JVM parameter **hazelcast.config.dir**
	* Directly, by configuring a bean inside the GeoServer application context
	
Both the 2 configurations should define the following parameters:

	#. The Hazelcast configuration requires a Map object with name *CacheProviderMap*
	#. Map eviction policy must be *LRU* or *LFU*
	#. Map configuration must have a fixed size defined in Mb
	#. Map configuration must have **USED_HEAP_SIZE** as *MaxSizePolicy* 
	
.. warning:: Be careful that all the cluster instances have the same configuration, because different configurations may result in incorrect Hazelcast behaviour. 
.. warning:: In order to avoid missing tiles, the cluster instances should access the same data.

Configuration with *hazelcast.xml*
``````````````````````````````````
Here can be found an example file:

		.. code-block:: xml
			
			<hazelcast xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-2.3.xsd"
					   xmlns="http://www.hazelcast.com/schema/config"
					   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			  <group>
				<name>cacheCluster</name>
				<password>geoserverCache</password>
			  </group>

			  <network>
				<!--
					Typical usage: multicast enabled with port auto-increment enabled
					or tcp-ip enabled with port auto-increment disabled. Note that you 
					must choose between multicast and tcp-ip. Another option could be
					aws, but will not be described here.
				
				-->
				<port auto-increment="false">5701</port>
					<join>
						 <multicast enabled="false">
							<multicast-group>224.2.2.3</multicast-group>
							<multicast-port>54327</multicast-port>
						</multicast>
						<tcp-ip enabled="true">
							<interface>192.168.1.32</interface>     
							<interface>192.168.1.110</interface> 
						</tcp-ip>
					</join>
			  </network>
			  
			  <map name="CacheProviderMap">
					<eviction-policy>LRU</eviction-policy>
					<max-size policy="USED_HEAP_SIZE">16</max-size>
			  </map>

			</hazelcast>

Configuration with ApplicationContext
`````````````````````````````````````
For configuring caching directly from the GeoServer application context, the user must edit the file *geowebcache-distributed.xml* inside the *gs-gwc* jar file. More informations about using
Spring with Hazelcast can be found in the `Hazelcast related documentation <http://docs.hazelcast.org/docs/3.3/manual/html/springintegration.html>`_. The modified application context is presented 
below:

		.. code-block:: xml
		
				<hz:hazelcast id="instance1">
					<hz:config>
						<hz:group name="dev" password="password" />
						<hz:network port="5701" port-auto-increment="true">
							<hz:join>
								<hz:multicast enabled="true" multicast-group="224.2.2.3"
									multicast-port="54327" />
							<hz:tcp-ip enabled="false">
							  <hz:members>10.10.1.2, 10.10.1.3</hz:members>
							</hz:tcp-ip>
							</hz:join>
						</hz:network>
						<hz:map name="CacheProviderMap" max-size="16" eviction-policy="LRU"
							max-size-policy="USED_HEAP_SIZE" />
					</hz:config>
				</hz:hazelcast>
				
				<bean id="HazelCastLoader1"
					class="org.geowebcache.storage.blobstore.memory.distributed.HazelcastLoader">
					<property name="instance" ref="instance1" />
				</bean>
				
				<bean id="HazelCastCacheProvider1"
					class="org.geowebcache.storage.blobstore.memory.distributed.HazelcastCacheProvider">
					<constructor-arg ref="HazelCastLoader1" />
				</bean>

Optional configuration parameters
``````````````````````````````````	
In this section are described other available configuration parameters to configure:

	* Cache expiration time:
	
			.. code-block:: xml
				
				<map name="CacheProviderMap">
				...
				
					<time-to-live-seconds>0</time-to-live-seconds>
					<max-idle-seconds>0</max-idle-seconds>
				
				</map>

		Where *time-to-live-seconds* indicates how many seconds an entry can stay in cache and *max-idle-seconds* indicates how many seconds an entry may be not accessed before being evicted.

	* Near Cache.
	
			.. code-block:: xml
	
				<map name="CacheProviderMap">
				...
				<near-cache>
				  <!--
					Same configuration parameters of the Hazelcast Map. Note that size indicates the maximum number of 
					entries in the near cache. A value of Integer.MAX_VALUE indicates no limit on the maximum 
					size.
				  -->
				  <max-size>5000</max-size>
				  <time-to-live-seconds>0</time-to-live-seconds>
				  <max-idle-seconds>60</max-idle-seconds>
				  <eviction-policy>LRU</eviction-policy>

				  <!--
					Indicates if a cached entry can be evicted if the same value is modified in the Hazelcast Map. Default is true.
				  -->
				  <invalidate-on-change>true</invalidate-on-change>

				  <!--
					Indicates if local entries must be cached. Default is false.
				  -->
				  <cache-local-entries>false</cache-local-entries>
				</near-cache>
				
				</map>	

		Near Cache is a local cache for each cluster instance which is used for caching entries in the other cluster instances. This behaviour avoids requesting those entries each time by executing a remote call. This feature could be helpful in order to improve Hazelcast Cache performance.
		
		.. note:: A value of *max-size* bigger or equal to Integer.MAX_VALUE cannot be used in order to avoid an uncontrollable growth of the cache size.
