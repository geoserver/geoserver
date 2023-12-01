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

In order to make it easier to find your data, put a link to your capabilities document somewhere on the web. This will ensure that a search engine will crawl and index it.

Set up clustering
-----------------

Setting up a `Cluster <http://en.wikipedia.org/wiki/Cluster_(computing)>`_ is one of the best ways to improve the reliability and performance of your GeoServer installation.  All the most stable and high performance GeoServer instances are configured in some sort of cluster.  There are a huge variety of techniques to configure a cluster, including at the container level, the virtual machine level, and the physical server level.  

Andrea Aime is currently working on an overview of what some of the biggest GeoServer users have done, for his 'GeoServer in Production' talk at FOSS4G 2009.  In time that information will be migrated to tutorials and white papers.