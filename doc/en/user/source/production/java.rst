.. _production_java:

Java Considerations
===================

Use supported JRE
-----------------

GeoServer's speed depends a lot on the chosen Java Runtime Environment (JRE). The latest versions of GeoServer are tested with both Oracle JRE and OpenJDK. Implementations other than those tested may work correctly, but are generally not recommended.

Tested:

* Java 11 and Java 8 - GeoServer 2.15.x and above (OpenJDK tested)
* Java 8 - GeoServer 2.9.x and above (OpenJDK and Oracle JRE tested)
* Java 7 - GeoServer 2.6.x to GeoServer 2.8.x (OpenJDK and Oracle JRE tested)
* Java 6 - GeoServer 2.3.x to GeoServer 2.5.x (Oracle JRE tested)
* Java 5 - GeoServer 2.2.x and earlier (Sun JRE tested)

.. Further speed improvements can be released using `Marlin renderer <https://github.com/bourgesl/marlin-renderer>`__ alternate renderer.

As of GeoServer 2.0, a Java Runtime Environment (JRE) is sufficient to run GeoServer.  GeoServer no longer requires a Java Development Kit (JDK).

Running on Java 17 (experimental!)
----------------------------------

GeoServer 2.21.x is compatible with Java 17, but requires extra care for running in some environments.

Deployment on Tomcat 9.0.55 has been tested with success.

The "bin" packaging can work too, but requires turning off the Marlin rasterizer integration. 
This can be done by modifying the scripts, or by simply removing the Marlin jars::

   rm webapps/geoserver/WEB-INF/lib/marlin-0.9.3.jar


GeoServer code depends on a variety of libraries trying to access the JDK internals. As reported above,
it does not seem to matter when running as a web application. However, in case of need, here is
the full list of opens used by the build process::

   --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED  --add-opens=java.desktop/sun.awt.image=ALL-UNNAMED --add-opens=java.naming/com.sun.jndi.ldap=ALL-UNNAMED

Running on Java 11
------------------

GeoServer 2.15 will run under Java 11 with no additional configuration on **Tomcat 9** or newer and **Jetty 9.4.12** or newer.

Running GeoServer under Java 11 on other Application Servers may require some additional configuration. Some Application Servers do not support Java 11 yet.

* **Wildfly 14** supports Java 11, with some additional configuration - in the run configuration, under VM arguments add:

      --add-modules=java.se

  Future WildFly releases should support Java 11 with no additional configuration.

* **GlassFish** does not currently Java 11, although the upcoming 5.0.1 release is expected to include support for it.

* **WebLogic** do not yet support Java 11.

.. _java_policyfiles:

Installing Unlimited Strength Jurisdiction Policy Files
-------------------------------------------------------
These policy files are needed for unlimited cryptography. As an example, Java does not support AES
with a key length of 256 bit. Installing the policy files removes these restrictions.

Open JDK
````````

Since Open JDK is Open Source, the policy files are already installed.   

Oracle Java
```````````

The policy files are available at   

* `Java 8 JCE policy jars <http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html>`__ 

The download contains two files, **local_policy.jar** and  **US_export_policy.jar**. The default
versions of these two files are stored in JRE_HOME/lib/security. Replace these two files with the
versions from the download. 


Test if unlimited key length is available
"""""""""""""""""""""""""""""""""""""""""

Start or restart GeoServer and login as administrator. The annotated warning should have disappeared.

.. figure:: ../security/webadmin/images/unlimitedkey.png

Additionally, the GeoServer log file should contain the following line::

   "Strong cryptography is available"

.. note::

   The replacement has to be done for each update of the Java runtime. 

IBM Java
````````

The policy files are available at

* `IBM JCE policy jars <https://www14.software.ibm.com/webapp/iwm/web/preLogin.do?source=jcesdk>`_ 

An IBM ID is needed to log in. The installation is identical to Oracle.

 
