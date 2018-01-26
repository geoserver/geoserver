Extension Install
=================

This workshop course requires GeoServer with a few additional extensions.

* CSS Styling: Quickly and easily generate SLD files
* YSLD Styling: An alternative styling language to SLD
* Importer: Wizard for bulk import of data

On Windows the following is recommended:
          
* `FireFox <http://www.mozilla.org/en-US/firefox/new/>`_
* `Notepad++ <http://notepad-plus-plus.org>`_

The **CSS extension** is distributed as a supported GeoServer extension. Extensions are unpacked into the ``libs`` folder of the GeoServer application. The **YSLD extension** is a new addition to geoserver and is distributed as an unsupported GeoServer extension.

.. note:: In a classroom setting these extensions have already been installed.

Manual Install
--------------

To download and install the required extensions by hand:

#. Download geoserver-2.10-M0-css-plugin.zip and geoserver-2.10-M0-css-plugin.zip from:

   * `Development Release <http://geoserver.org/download/>`_ (GeoServer WebSite)
   
   It is important to download the version that matches the GeoServer you are running.

#. Download the geoserver-2.10-SNAPSHOT-ysld-plugin.zip from:

   * `Community Builds <https://build.geoserver.org/geoserver/master/community-latest/>`_ (GeoServer WebSite)

#. Stop the GeoServer application.

#. Navigate into the :file:`webapps/geoserver/WEB-INF/lib` folder.

   These files make up the running GeoServer application.

#. Unzip the contents of the three zip files into the :file:`lib` folder.

#. Restart the Application Server.
   
#. Login to the Web Administration application. Select **Styles** from the naviagion menu. Click :guilabel:`Create a new style` and ensure both CSS and YSLD are available in the formats dropdown. Click :guilabel:`Cancel` to return to the **Styles** page without saving.