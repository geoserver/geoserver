.. _quickstart:

Quickstart
==========

A step by step guide describing how to quickly get up and running with a
GeoServer development environment. This guide assumes that all the necessary
:ref:`tools` are installed.

.. note::

  This guide is designed to get developers up and running as quick as possible.
  For a more comprehensive guide see the :ref:`maven_guide` and the
  :ref:`eclipse_guide`.

.. contents:: :local:

Check out source code
---------------------

Check out the source code from the git repository.::

   git clone git://github.com/geoserver/geoserver.git geoserver

To list the available branches.::

  % git branch
     2.1.x
     2.2.x
   * master

Choose ``master`` for the latest development.::

  % git checkout master

Or chose a stable branch for versions less likely to change often::

  % git checkout 2.6.x

In this example we will pretend that your source code is in a directory
called ``geoserver``, but a more descriptive name is recommended.

Build with Maven
----------------

Change directory to the root of the source tree and execute the maven build
command::

  cd geoserver/src
  mvn clean install

This will result in significant downloading of dependencies on the first build.

A successful build will result in output that ends with something like the following::

    [INFO] Reactor Summary:
    [INFO] 
    [INFO] GeoServer ......................................... SUCCESS [1:09.446s]
    [INFO] Core Platform Module .............................. SUCCESS [57.626s]
    [INFO] Open Web Service Module ........................... SUCCESS [1:12.050s]
    [INFO] Main Module ....................................... SUCCESS [6:38.549s]
    [INFO] GeoServer Security Modules ........................ SUCCESS [2.273s]
    [INFO] GeoServer JDBC Security Module .................... SUCCESS [58.881s]
    [INFO] GeoServer LDAP Security Module .................... SUCCESS [30.752s]
    [INFO] Web Coverage Service Module ....................... SUCCESS [6.876s]
    [INFO] Web Coverage Service 1.0 Module ................... SUCCESS [1:15.801s]
    [INFO] Web Coverage Service 1.1 Module ................... SUCCESS [59.588s]
    [INFO] Web Coverage Service 2.0 Module ................... SUCCESS [2:02.129s]
    [INFO] Web Feature Service Module ........................ SUCCESS [3:27.534s]
    [INFO] Web Map Service Module ............................ SUCCESS [4:00.844s]
    [INFO] KML support for GeoServer ......................... SUCCESS [1:03.458s]
    [INFO] GeoWebCache (GWC) Module .......................... SUCCESS [2:02.134s]
    [INFO] REST Support Module ............................... SUCCESS [38.312s]
    [INFO] REST Configuration Service Module ................. SUCCESS [2:33.951s]
    [INFO] GeoServer Web Modules ............................. SUCCESS [0.365s]
    [INFO] Core UI Module .................................... SUCCESS [1:56.290s]
    [INFO] WMS UI Module ..................................... SUCCESS [52.232s]
    [INFO] GWC UI Module ..................................... SUCCESS [1:10.771s]
    [INFO] WFS UI Module ..................................... SUCCESS [29.946s]
    [INFO] Demoes Module ..................................... SUCCESS [54.479s]
    [INFO] WCS UI Module ..................................... SUCCESS [39.285s]
    [INFO] Security UI Modules ............................... SUCCESS [0.161s]
    [INFO] Security UI Core Module ........................... SUCCESS [3:01.187s]
    [INFO] Security UI JDBC Module ........................... SUCCESS [2:26.734s]
    [INFO] Security UI LDAP Module ........................... SUCCESS [46.564s]
    [INFO] REST UI Module .................................... SUCCESS [31.175s]
    [INFO] GeoServer Web Application ......................... SUCCESS [12.499s]
    [INFO] Community Space ................................... SUCCESS [0.943s]
    [INFO] GeoServer Extensions .............................. SUCCESS [0.209s]
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 42:49.104s
    [INFO] Finished at: Sun Feb 08 11:05:22 AEDT 2015
    [INFO] Final Memory: 48M/115M
    [INFO] ------------------------------------------------------------------------


Import modules into your IDE
----------------------------

The next step is to import the GeoServer project into the IDE of your choice:

.. toctree::
   :maxdepth: 1

   eclipse
   intellij

If your IDE is not listed, consider :docguide:`adding <contributing>` a new quickstart page for it.

