.. _installation_osx_installer:

Mac OS X installer
==================

The Mac OS X installer provides an easy way to set up GeoServer on your system, as it requires no configuration files to be edited or command line settings.

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 8** environment, and the JRE supplied by OS X is not sufficient. For more information, please see the `instructions for installing Oracle Java on OS X <http://java.com/en/download/faq/java_mac.xml>`_.

   .. note:: Java 9 is not currently supported.

   .. note:: For more information about Java and GeoServer, please see the section on :ref:`production_java`.

#. Navigate to the `GeoServer Download page <http://geoserver.org/download>`_.

#. Select the version of GeoServer that you wish to download. If you're not sure, select `Stable <http://geoserver.org/release/stable>`_.

#. Click the link for the Mac OS X installer to begin the download.

#. When downloaded, double click on the file to open it.
      
#. Drag the GeoServer icon to the Applications folder. 

    .. figure:: images/osx1.png
       
       Drag the GeoServer icon to Applications to install

#. Navigate to your Applications folder and double click the GeoServer icon.

#. In the resulting GeoServer console window, start GeoServer by going to :menuselection:`Server --> Start`.

    .. figure:: images/osx2.png
   
       Starting GeoServer

#. The console window will be populated with log entries showing the GeoServer loading process. Once GeoServer is completely started, a browser window will open at ``http://localhost:8080/geoserver``, which is the :ref:`web_admin` for GeoServer.

    .. include:: ./osx_jaierror.txt
