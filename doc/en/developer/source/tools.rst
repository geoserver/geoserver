.. _tools:

Tools
=====

The following tools need to installed on the system before a GeoServer developer
environment can be set up.

Java
----

Developing with GeoServer requires a Java Development Kit (JDK), version 8, available from `OpenJDK <http://openjdk.java.net>`__, `AdoptOpenJDK <https://adoptopenjdk.net>`__ for Windows and macOS installers, or provided by your OS distribution.

Due to subtle changes in Java class libraries we require development on Java 8 at this time (although the result is tested on Java 11).

Maven
-----

GeoServer uses a tool known as `Maven <http://maven.apache.org/>`_ to build. 

Maven tracks global settings in your home directory .m2/settings.xml. This file is used to control
global options such as proxy settings or listing repository mirrors to download from.

Git
---

GeoServer source code is stored and version in a git repository on `github <http://github.com/geoserver/geoserver>`_
There are a variety of git clients available for a number of different 
platforms. Visit http://git-scm.com/ for more details.
