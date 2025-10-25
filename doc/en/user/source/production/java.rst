.. _production_java:

Java Considerations
===================

Use supported JRE
-----------------

GeoServer's speed depends a lot on the chosen Java Runtime Environment (JRE). The latest versions of GeoServer are tested with both Oracle JRE and OpenJDK. Implementations other than those tested may work correctly, but are generally not recommended.

Tested:

* Java 21 - GeoServer 2.28.x and above
* Java 17 - GeoServer 2.28.x and above
* Java 11 - GeoServer 2.15.x to GeoServer 2.27.x
* Java 8 - GeoServer 2.9.x to GeoServer 2.22.x (OpenJDK and Oracle JRE tested)
* Java 7 - GeoServer 2.6.x to GeoServer 2.8.x (OpenJDK and Oracle JRE tested)
* Java 6 - GeoServer 2.3.x to GeoServer 2.5.x (Oracle JRE tested)
* Java 5 - GeoServer 2.2.x and earlier (Sun JRE tested)

.. Further speed improvements can be released using `Marlin renderer <https://github.com/bourgesl/marlin-renderer>`__ alternate renderer.

As of GeoServer 2.0, a Java Runtime Environment (JRE) is sufficient to run GeoServer.  GeoServer no longer requires a Java Development Kit (JDK).

Running on Java 17
----------------------------------

GeoServer 2.28.x and above requires Java 17 as the minimum version. GeoServer has been tested and fully supports Java 17.

Deployment on Tomcat 9.0.55 has been tested with success.

GeoServer code depends on a variety of libraries trying to access the JDK internals. 
It does not seem to matter when running as a web application. However, in case of need, 
here is the full list of opens used by the build process::

   --add-exports=java.desktop/sun.awt.image=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED --add-opens=java.desktop/sun.awt.image=ALL-UNNAMED --add-opens=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.desktop/sun.java2d.pipe=ALL-UNNAMED

Running on Java 21
------------------

GeoServer 2.28.x and above supports Java 21 with no additional configuration on **Tomcat 9** and **Jetty 9.4.12** or newer.

Running GeoServer under Java 21 on other Application Servers may require some additional configuration. Most modern Application Servers now support Java 21.
