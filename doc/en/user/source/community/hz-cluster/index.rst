.. _hz_cluster_plugin:

Hazelcast Clustering Plugin
---------------------------

Overview
--------

The **Hazelcast Clustering Community Module** for **GeoServer** enables clustering capabilities to
support clustering GeoServer in conjunction with `jdbcconfig` and `jdbcdstore`.
This module primarily focuses on JDBC modules cache invalidation, ensuring that when data changes
on one instance, caches on other instances are appropriately invalidated or refreshed.

The module also offers some support for **Wicket session clustering**.

Simple clustering cache invalidation setup
------------------------------------------

The module can be simply dropped in GeoServer own `WEB-INF/lib` directory, and it will automatically
configure itself to use the default Hazelcast configuration, using multicast.
This is suitable for most users who want to enable clustering without extensive customization.

If multicast is not available, then one can customize the ``hazelcast.xml`` file found in the
``GEOSERVER_DATA_DIR/cluster`` directory, disabling multicast and enabling TCP/IP or AWS instead.
Here are sample contents for the file, for reference:

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <!--
    Configure Hazelcast for clustering GeoServer's catalog and web sessions
    For more information, see:
    https://docs.hazelcast.com/hazelcast/5.3/configuration/configuring-declaratively
    -->
    <hazelcast xmlns="http://www.hazelcast.com/schema/config"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.hazelcast.com/schema/config
                                   https://hazelcast.com/schema/config/hazelcast-config-5.3.xsd">
      <cluster-name>gsEventCluster</cluster-name>
      <instance-name>gsEventCluster</instance-name>

      <!--
         Make Hazelcast use log4j2 just like GeoServer. Remember to add
         a Logger for com.hazelcast with the appropriate logging level
         in the geoserver logging configuration to see Hazelcast log messages
      -->
      <properties>
        <property name="hazelcast.logging.type">log4j2</property>
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
    </hazelcast>

In the same directory a `cluster.properties` file can be found, that can be used to tune the
event notification mechanism. The default settings are sufficient for small catalogs, for larger
ones it is recommended to set the `sync_method` to `event`, which will avoid a full data directory
reload. One might also want to have a shorter `sync_delay` (can be set to zero).

.. code-block:: properties

    #
    # Configuration for cluster module
    #
    #
    # To change the configuration of the Hazelcast instance to be used, look at hazelcast.xml
    #

    # Enable clustering. Requires restart.
    enabled = true

    # Select sync method. Requires restart.

    # Collapse all synchronization events and reload the entire catalog and configuration
    sync_method = reload

    # Notify system of each individual catalog or configuration object updated via event callbacks
    # sync_method = event

    # Time to delay before doing synchronization. Does not require restart.
    sync_delay = 5

    # Enable session sharing. Requires restart.
    session_sharing = true

    # Load Balancer provides sticky sessions. Requires restart.
    session_sticky = false

    # milliseconds to wait for node ack notifications upon sending a config change event.
    acktimeout = 2000

Enabling Wicket session sharing
-------------------------------

The Wicket session sharing allows different nodes of the same cluster to share the same session,
allowing the UI to be put under load balancing too. This is an alternative to using a single
node for GUI/REST operations, a simpler approach that can be configured at the load balancer level.cluster

The session sharing is enabled by default in the configuration files, but in order to actually
work, it also needs an additional declarations in the ``WEB-INF/web.xml`` file.

1) Open the file, and place the following XML block as the first filter declaration in the file:

.. code-block:: xml

    <filter>
      <filter-name>hazelcast</filter-name>
      <filter-class>org.geoserver.cluster.hazelcast.web.HzSessionShareFilter</filter-class>
    </filter>

2) Scroll down and look for filter mappings, and add the following as the first filter mapping:

.. code-block:: xml

    <filter-mapping>
      <filter-name>hazelcast</filter-name>
      <url-pattern>/*</url-pattern>
    </filter-mapping>

3) Finally, reach out to the section that declares listeners, and add the following as well (order is not important here):

.. code-block:: xml

    <listener>
      <listener-class>org.geoserver.cluster.hazelcast.web.HzSessionShareListener</listener-class>
    </listener>

4) Save the file and restart GeoServer. The session sharing should now be active.