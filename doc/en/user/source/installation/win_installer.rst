.. _installation_windows_installer:

Windows installer
=================

The Windows installer provides an easy way to set up GeoServer on your system, as it requires no configuration files to be edited or command line settings.

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 17** or **Java 21** environment, as provided by `Adoptium <https://adoptium.net>`__ Windows installers.

   .. note:: For more information about Java and GeoServer, please see the section on :ref:`production_java`.

#. Navigate to the :website:`GeoServer Download page <download>`.

#. Select the version of GeoServer that you wish to download.  If you're not sure, select :website:`Stable <release/stable>` release.

   .. only:: snapshot
      
      This documentation covers GeoServer |version|-SNAPSHOT which is under development and
      is available as a :website:`Nightly <release/main>` release.
      
      Nightly releases are used to
      test out try out new features and test community modules and do not provide a windows
      installer. When GeoServer |version|.0 is released a windows installer will be provided.
      
   .. only:: not snapshot

      These instructions are for GeoServer |release|.

#. Click the link for the :guilabel:`Windows Installer`.

   .. figure:: images/win_download.png

      Downloading the Windows installer

#. After downloading, double-click the file to launch.

#. At the Welcome screen, click :guilabel:`Next`.

   .. figure:: images/win_welcome.png

      Welcome screen

#. Read the :ref:`license` and click :guilabel:`I Agree`.

   .. figure:: images/win_license.png

      GeoServer license

#. Select the directory of the installation, then click :guilabel:`Next`.

   .. figure:: images/win_installdir.png

      GeoServer install directory

#. Select the Start Menu directory name and location, then click :guilabel:`Next`.

   .. figure:: images/win_startmenu.png

      Start menu location

#. Enter the path to a **valid Java Runtime Environment (JRE)**. GeoServer requires a valid JRE in order to run, so this step is required. The installer will inspect your system and attempt to automatically populate this box with a JRE if it is found, but otherwise you will have to enter this path manually. When finished, click :guilabel:`Next`.
   
   .. note:: A typical path on Windows would be :file:`C:\\Program Files\\Java\\jre8`.

   .. note:: Don't include the :file:`\\bin` in the JRE path. So if :file:`java.exe` is located at :file:`C:\\Program Files (x86)\\Java\\jre8\\bin\\java.exe`, set the path to be :file:`C:\\Program Files (x86)\\Java\\jre8`.

   .. note:: For more information about Java and GeoServer, please see the section on :ref:`production_java`.
   
   .. figure:: images/win_jre.png

      Selecting a valid JRE

#. Enter the path to your GeoServer data directory or select the default. If this is your first time using GeoServer, select the :guilabel:`Default data directory`. When finished, click :guilabel:`Next`.

   .. figure:: images/win_datadir.png

      Setting a GeoServer data directory

#. Enter the username and password for administration of GeoServer. GeoServer's :ref:`web_admin` requires authentication for management, and what is entered here will become those administrator credentials.  The defaults are :guilabel:`admin / geoserver`.  It is recommended to change these from the defaults. When finished, click :guilabel:`Next`.

   .. figure:: images/win_creds.png

      Setting the username and password for GeoServer administration

#. Enter the port that GeoServer will respond on. This affects the location of the GeoServer :ref:`web_admin`, as well as the endpoints of the GeoServer services such as :ref:`wms` and :ref:`wfs`.  The default port is :guilabel:`8080`, though any valid and unused port will work. When finished, click :guilabel:`Next`.

   .. figure:: images/win_port.png

      Setting the GeoServer port

#. Select whether GeoServer should be run manually or installed as a service. When run manually, GeoServer is run like a standard application under the current user. When installed as a service, GeoServer is integrated into Windows Services, and thus is easier to administer. If running on a server, or to manage GeoServer as a service, select :guilabel:`Install as a service`. Otherwise, select :guilabel:`Run manually`.  When finished, click :guilabel:`Next`.

   .. figure:: images/win_service.png

      Installing GeoServer as a service

#. Review your selections and click the :guilabel:`Back` button if any changes need to be made.  Otherwise, click :guilabel:`Install`.

   .. figure:: images/win_review.png

      Verifying settings

#. GeoServer will install on your system.

   .. figure:: images/win_install_process.png
      
      Install progress


#. When finished, click :guilabel:`Finish` to close the installer.
   
   .. figure:: images/win_completing.png
      
      Completing

#. If you installed GeoServer as a service, it is already running.  Otherwise, you can start GeoServer by going to the Start Menu, and clicking :guilabel:`Start GeoServer` in the GeoServer folder.

#. Navigate to ``http://localhost:8080/geoserver`` (or wherever you installed GeoServer) to access the GeoServer :ref:`web_admin`.

   If you see the GeoServer Welcome page, then GeoServer is successfully installed.

   .. figure:: images/success.png
      
      GeoServer Welcome Page

Uninstallation
--------------

GeoServer can be uninstalled in two ways: by running the :file:`uninstall.exe` file in the directory where GeoServer was installed, or by standard Windows program removal.
