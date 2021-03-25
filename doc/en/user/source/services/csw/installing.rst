.. _csw_installing:

Installing Catalog Services for Web (CSW)
=========================================


The CSW extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

#. Download the :download_extension:`csw`
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer or the servlet container, as appropriate to your configuration.

#. Verify that the module was installed correctly by navigating to the Welcome page of the :ref:`web_admin` and seeing the :guilabel:`CSW` entry is listed in the :guilabel:`Service Capabilities` list, and the CSW modules are listed in the :ref:`web_admin` Module list.

.. figure:: images/install.png

   CSW Installation Verification

