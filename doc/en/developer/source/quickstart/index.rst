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


Generate Eclipse project files with Maven
-----------------------------------------

Generate the eclipse ``.project`` and  ``.classpath`` files::

  mvn eclipse:eclipse

Import modules into Eclipse
---------------------------

#. Run the Eclipse IDE
#. Open the Eclipse ``Preferences``
#. Navigate to ``Java``, ``Build Path``, ``Classpath Variables`` and click
   ``New...``

   .. image:: m2repo1.jpg

#. Create a classpath variable named "M2_REPO" and set the value to the location
   of the local Maven repository, and click ``Ok``

   .. image:: m2repo2.jpg

#. Click ``Ok`` to apply the new Eclipse preferences
#. Right-click in the ``Package Explorer`` and click ``Import...``

   .. image:: import1.jpg
      :width: 300

#. Select ``Existing Projects into Workspace`` and click ``Next``

   .. image:: import2.jpg
      :width: 400

#. Navigate to the ``geoserver/src`` directory
#. Ensure all modules are selected and click ``Finish``

   .. image:: import3.jpg
      :width: 350

Run GeoServer from Eclipse
--------------------------

#. From the ``Package Explorer`` select the ``web-app`` module
#. Navigate to the ``org.geoserver.web`` package
#. Right-click the ``Start`` class and navigate to ``Run as``, ``Java Application``

   .. image:: run1.jpg
      :width: 600

#. After running the first time you can return to the ``Run Configurations`` dialog
   to fine tune your launch environment (including setting a GEOSERVER_DATA_DIR).

.. note::
   
   If you already have a server running on localhost:8080 see the :ref:`eclipse_guide` for instructions on changing to a different port.

Run GeoServer with Extensions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The above instructions assume you want to run GeoServer without any extensions enabled. In cases where you do need certain extensions, the ``web-app`` module declares a number of profiles that will enable specific extensions when running ``Start``. To enable an extension, re-generate the root eclipse profile with the appropriate maven profile(s) enabled::

  % mvn eclipse:eclipse -P wps

The full list of supported profiles can be found in ``src/web/app/pom.xml``.
   
Access GeoServer front page
---------------------------

* After a few seconds, GeoServer should be accessible at: `<http://localhost:8080/geoserver>`_
* The default ``admin`` password is ``geoserver``.

