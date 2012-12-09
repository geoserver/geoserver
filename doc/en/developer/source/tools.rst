.. _tools:

Tools
=====

The following tools need to installed on the system before a GeoServer developer
environment can be set up.

Java
----

Developing with GeoServer requires a Java Development Kit (JDK) 6 or greater, available from `Oracle <http://www.oracle.com/technetwork/java/javase/downloads/index.html>`_.

.. note::

   While it is possible to use a JDK other than the one provided by Oracle, it is 
   recommended that the Oracle JDK be used.

Maven
-----

GeoServer uses a tool known as `Maven <http://maven.apache.org/>`_ to build. 
The current recommended version of Maven is 2.2.1 and is available from 
`Apache <http://maven.apache.org/download.html>`_. While 2.2.1 is recommended
any version greater than 2.0.8 should also work.

Maven tracks global settings in your home directory .m2/settings.xml. This file is used to control
global options such as proxy settings or listing repository mirrors to download from.

Git
---

GeoServer source code is stored and version in a git repository on `github <http://github.com/geoserver/geoserver>`_
There are a variety of git clients available for a number of different 
platforms. Visit http://git-scm.com/ for more details.
