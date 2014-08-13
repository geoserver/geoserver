.. _installation_osx:

Mac OS X
========

There are a few ways to install GeoServer on OS X.  The simplest way is to use the OS X installer, but you can also perform a manual installation with the OS-independent binary.

.. note:: Java Runtime Environment
   
   GeoServer requires the installation of *Java 7* distributed for OSX by Oracle:
   
   * http://java.com/en/download/faq/java_mac.xml
   
   The JRE provided by Apple is limited to Java 6 and may be used with GeoServer 2.5 or earlier. GeoServer does not work with Java 8 at this time.
   
.. note:: To run GeoServer as part of a servlet container such as Tomcat, please see the :ref:`installation_war` section.

.. toctree::
   :maxdepth: 2

   installer
   binary