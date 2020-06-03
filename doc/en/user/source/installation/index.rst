.. _installation:

Installation
============

There are many ways to install GeoServer on your system. This section will discuss the various installation paths available.

.. note:: To run GeoServer as part of an existing servlet container such as Tomcat, please see the :ref:`installation_war` section.

.. warning:: GeoServer requires a Java 8 or Java 11 environment (JRE) to be installed on your system, available from `OpenJDK <http://openjdk.java.net>`__, `AdoptOpenJDK <https://adoptopenjdk.net>`__ for Windows and macOS installers, or provided by your OS distribution.
   
   This must be done prior to installation.

.. toctree::
   :maxdepth: 1
   
   win_binary
   osx_binary
   linux
   war
   upgrade

.. note:: At this time we no longer provide a Windows Installer, due to lack of a secure Windows machine where the installer can be built and signed. However if you realy need the Windows installer, you can create one following :developer:`the instruction on this site <win-installer.html>`.
