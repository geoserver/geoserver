.. _tools:

Tools
=====

The following tools need to installed on the system before a GeoServer developer
environment can be set up.

Java
----

Developing with GeoServer requires a Java Development Kit (JDK) 1.5 or greater, available from `Sun Microsystems <http://java.sun.com/javase/downloads/index_jdk5.jsp>`_.

.. note::

   While it is possible to use a JDK other than the one provided by Sun, it is 
   recommended that the Sun JDK be used.

Maven
-----

GeoServer uses a tool known as `Maven <http://maven.apache.org/>`_ to build. 
The current recommended version of Maven is 2.2.1 and is available from 
`Apache <http://maven.apache.org/download.html>`_. While 2.2.1 is recommended
any version greater than 2.0.8 should also work.

Maven tracks global settings in your home directory .m2/settings.xml. This file is used to control
global options such as proxy settings or listing repository mirrors to download from.

Subversion
----------

GeoServer source code is stored and versioned in a subversion repository. There
are a variety of subversion clients available for a number of different 
platforms. Visit http://subversion.tigris.org/getting.html for more details.
