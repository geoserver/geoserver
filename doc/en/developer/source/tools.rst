.. _tools:

Tools
=====

The following tools need to installed on the system before a GeoServer developer
environment can be set up.

Java
----

Developing with GeoServer requires a Java Development Kit (JDK), available from `OpenJDK <http://openjdk.java.net>`__, `Adoptium <https://adoptium.net/>`__ for Windows and macOS installers, or provided by your OS distribution.

The GeoServer project works with Java Long Term Support releases: Java 11 and Java 17.

Due to subtle changes in Java class libraries we require development on Java 11 at this time (although the result is tested on Java 17). When calling a method of java core library a type case may (or not be) implied and represented in the bytecode.  We when the core libraries change the return type of a method weh can have a very subtle incompatibility.

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
