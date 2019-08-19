.. _csw_iso_installing:

Installing Catalog Services for Web (CSW) - ISO Metadata Profile
================================================================

To install the CSW ISO extension:

#. If you do not have the CSW extension yet, get it first. Visit the GeoServer `Download <http://geoserver.org/download>`_ and navigate to the download page for the version of GeoServer your are using. The **csw** download is listed under extensions. The file name is called :file:`geoserver-*-csw-plugin.zip`, where ``*`` matches the version number of GeoServer you are using.

#. Additionally, download   the **csw-iso** community extension from the appropiate `nightly build <https://build.geoserver.org/geoserver/>`_. The file name is called :file:`geoserver-*-csw-plugin.zip`, where ``*`` matches the version number of GeoServer you are using. 

#. Extract this these files and place the JARs in ``WEB-INF/lib``.

#. Perform any configuration required by your servlet container, and then restart.

#. Verify that the CSW module was installed correctly by going to the Welcome page of the :ref:`web_admin` and seeing that :guilabel:`CSW` is listed in the :guilabel:`Service Capabilities` list. Open the CSW capabilities and search for the text `gmd:MD_Metadata` to verify that the ISO metadata profile was installed correctly.
