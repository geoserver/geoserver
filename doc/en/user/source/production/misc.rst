.. _production_misc:

Other Considerations
====================

Host your application separately
--------------------------------

GeoServer includes a few sample applications in the demo section of the :ref:`web_admin`.  For production instances, we recommend against this bundling of your application.  To make upgrades and troubleshooting easier, please use a separate container for your application.  It is perfectly fine, though, to use one container manager (such as Tomcat or Jetty) to host both GeoServer and your application.

Proxy your server
-----------------

GeoServer can have the capabilities documents properly report a proxy.  You can configure this in the Server configuration section of the :ref:`web_admin` and entering the URL of the external proxy in the field labeled ``Proxy base URL``.

Publish your server's capabilities documents
--------------------------------------------

In order to make it easier to find your data, your service must be available.

* Put a link to your capabilities document somewhere on the web. This will ensure that a search engine will crawl and index it.

* If your GeoServer is operating as part of a "Spatial Data Infrastructure" it may be registered with a catalogue service.
  
  Catalogue services such as `GeoNetwork-opensource <http://geonetwork-opensource.org/>`__ or `PyCSW <https://pycsw.org>`__ can harvest GeoServer listing the web serivces and layers published allowing content to be more easily browsed and searched.

Clustering
----------

Setting up a `Cluster <http://en.wikipedia.org/wiki/Cluster_(computing)>`_ is one of the best ways to improve the reliability and performance of your GeoServer installation.

There are a variety of techniques to configure a cluster, including at the container level, the virtual machine level, and the physical server level.

This results in running multiple GeoServer nodes behind a common proxy or load balancer:
  
1. The ``PROXY_BASE_URL`` setting allows all nodes in the cluster operate as the same web service.
  
2. Optional: Provide settings to identify :ref:`identify` individual nodes.

3. Optional: Use :ref:`logging_location` settings to direct logs from individual nodes to different files.

4. While most web services are stateless, some functionality like importer or WPS may require the use of an extension to share state between nodes.

  WPS clustering has extensions for Hazelcast and JDBC (shared database).
  
  Importer provides an extension to establish a Berkley shared database.

5. The challenge to operating a cluster is the management of changes to the data directory configuration to ensure all the nodes are operating with the same configuration and produce the same results.

   * Using a shared file system to allow nodes to share a common data directory.

   * Using version control or a layer of a docker image to share a data directory.

   To support these workflows teams often direct `geoserver/web` requests to a single node, and use the REST API to restart other nodes when a configuration change is ready.

The limitation of these approach is the startup time associated with restarting nodes. Several community modules exist to address this limitation:

* The project `Cloud Native GeoServer <https://github.com/geoserver/geoserver-cloud>`__ breaks the GeoServer application into microservies that may be scaled by Kubernetes.
  
  This prevents the needs to restart nodes, but requires a microservice environment.

* The community module :ref:`` uses a bus to communicate configuration changes in real time between all the nodes in a cluster.
  
  Each mode is updated when configuration changes are made, preventing the need to restart nodes, while providing full web service performance.

* The community module :ref:`community_jdbcconfig` allows the configuration to be stored in a database. This is best combined with the community module :ref:`community_jdbcstore` which allows the icons, fonts and styles to also be stored in a database.
  
  Each node checks for changes during operation, preventing the need to restart nodes, with an impact to web service performance. 
