.. _csw_installing:

Installing Catalog Services for Web (CSW)
=========================================

To install the CSW extension:

#. Visit the GeoServer `Download <http://geoserver.org/download>`_ and navigate to the download page for the version od GeoServer your are using. The **csw** download is listed under extensions. The file name is called :file:`geoserver-*-csw-plugin.zip`, where ``*`` matches the version number of GeoServer you are using.

#. Extract this this file and place the JARs in ``WEB-INF/lib``.

#. Perform any configuration required by your servlet container, and then restart.

#. Verify that the module was installed correctly by going to the Welcome page of the :ref:`web_admin` and seeing that :guilabel:`CSW` is listed in the :guilabel:`Service Capabilities` list.
