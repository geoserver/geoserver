.. _hazelcast_clustering:

Hazelcast based process status clustering
=========================================

Starting with version 2.7.0 GeoServer has a new WPS extension point allowing GeoServer nodes 
in the same cluster to share the status of current WPS requests. 
This is particularly important for asynchronous ones, as the client polling for the progress/results
might not be hitting the same node that's currently running the requests.

The Hazelcast based status sharing module leverages the Hazelcast library to share the information
about the current process status using a replicated map.

Installation
------------

The installation of the module follows the usual process for most extensions:

* Stop GeoServer
* Unpack the contents of gs-wps-hazelcast-status.zip into the ``geoserver/WEB-INF/lib`` folder
* Restart GeoServer

Configuration
-------------

The module does not require any configuration in case the default behavior is suitable for the
deploy enviroment.

By default, the module will use multicast messages to locate other nodes in the same cluster
and will automatically start sharing information about the process status with them.

In case this is not satisfactory, a ``hazelcast.xml`` file can be created/edited in the 
root of the GeoServer data directory to modify the network connection methods.

The file is not using a GeoServer specific syntax, it's instead a regular 
`Hazelcast configuration <http://docs.hazelcast.org/docs/3.3/manual/html-single/hazelcast-documentation.html#configuring-hazelcast>`_
file with a simple distributed map declaration:

.. code-block:: xml

    <hazelcast xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.hazelcast.com/schema/config
                                   http://www.hazelcast.com/schema/config/hazelcast-config-3.3.xsd"
      xmlns="http://www.hazelcast.com/schema/config">
    
      <!-- Protecting against accidental cluster joining -->
      <group>
        <name>geoserver</name>
        <password>geoserver</password>
      </group>
      
      <!-- 
         Make Hazelcast use log4j just like GeoServer. Remember to add:
           log4j.category.com.hazelcast=INFO
         in the geoserver logging configuration to see Hazelcast log messages
      -->
      <properties>
        <property name="hazelcast.logging.type">log4j</property>
      </properties>
    
      <!-- Network section, by default it enables multicast, tune it to use tcp in case 
        multicast is not allowed, and list the nodes that make up a reasonable core of the 
        cluster (e.g., machines that will never be all down at the same time) -->
      <network>
        <port auto-increment="true">5701</port>
        <join>
          <multicast enabled="true">
            <multicast-group>224.2.2.3</multicast-group>
            <multicast-port>54327</multicast-port>
          </multicast>
          <tcp-ip enabled="false">
            <interface>127.0.0.1</interface>
          </tcp-ip>
          <aws enabled="false">
            <access-key>my-access-key</access-key>
            <secret-key>my-secret-key</secret-key>
            <region>us-east-1</region>
          </aws>
        </join>
      </network>
      
      
    
      <!-- The WPS status map -->
      <map name="wpsExecutionStatusMap">
        <indexes>
          <!-- Add indexes to support the two most common queries -->
          <index ordered="false">executionId</index>
          <index ordered="true">completionTime</index>
        </indexes>
      </map>
    </hazelcast>
 
In case a TCP based configuration is desired, one just needs to disable the multicast one,
enable the tcp-ip one, and add a list of interface addresses in it that will form the core
of the cluster. 
Not all nodes in the cluster need to be listed in said section, but a list long enough to ensure
that not all the nodes in the list might go down at the same time: as long as at least one 
of said nodes lives, the cluster will maintain its integrity.   