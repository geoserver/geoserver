.. _jboss_tutorial:

geoserver on JBoss
==================
This tutorial documents how to install various versions of geoserver onto various versions of JBoss.


geoserver 2.7.0 on JBoss AS 5.1
-------------------------------
To install geoserver onto JBoss AS 5.1, the following is required:

1. Create the file ``jboss-classloading.xml`` with the following content then copy it into the ``WEB-INF`` directory in the geoserver.war:

.. code-block:: xml

	<classloading xmlns="urn:jboss:classloading:1.0"
		name="geoserver.war"
		domain="GeoServerDomain">
	</classloading>

2. Extract the ``hsqldb-2.2.8.jar`` file from the ``WEB-INF/lib`` directory from the geoserver.war and copy it as ``hsqldb.jar`` to the ``common/lib`` directory in the JBoss deployment.

3. Add the following text to the ``WEB-INF/web.xml`` file in the geoserver.war so that JBoss logging does not end up in the geoserver.log:

.. code-block:: xml

	<context-param>
		<param-name>RELINQUISH_LOG4J_CONTROL</param-name>
		<param-value>true</param-value>
	</context-param>    

