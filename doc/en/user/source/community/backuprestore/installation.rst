.. _backup_restore_installation:

Installation
============

Manual Install
--------------

To download and install the required extensions by hand:

#. Download the geoserver-2.15-SNAPSHOT-backup-restore-plugin.zip from:

   * `Community Builds <https://build.geoserver.org/geoserver/master/community-latest/>`_ (GeoServer WebSite)
   
   It is important to download the version that matches the GeoServer you are running.

#. Stop the GeoServer application.

#. Navigate into the :file:`webapps/geoserver/WEB-INF/lib` folder.

   These files make up the running GeoServer application.

#. Unzip the contents of the three zip files into the :file:`lib` folder.

#. Restart the Application Server.
   
#. Login to the Web Administration application. Select **Data** from the naviagion menu. Click :guilabel:`Backup and Restore` and ensure the page is rendered correctly and without errors.


Backup and Restore plugin can be used both via user interface and via HTTP REST interface. For more details please see the next sections.

