.. _mongodb:

MongoDB Data Store
==================

This module provides support for MongoDB data store. This extension is build on top of :geotools:`GeoTools MongoDB plugin
<library/data/mongodb.html>`.

Installation
------------

#. Visit the :website:`website download <download>` page, locate your release, and download:  :download_extension:`mongodb`

   The download link will be in the :guilabel:`Extensions` section under :guilabel:`Vector Formats`.
   
   .. warning:: Make sure to match the version of the extension (for example |release| above) to the version of the GeoServer instance!

#. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

#. Restart GeoServer

Usage
-----

If the extension was successfully installed a new type of data store named ``MongoDB`` should be available:

.. figure:: images/mongodb_store_1.png
   :align: center

   *MongoDB data store.*

Configuring a new MongoDB data store requires providing:

#. The URL of a MongoDB database.

#. The absolute path to a data directory where GeoServer will store the schema produced for the published collections.

.. figure:: images/mongodb_store_2.png
   :align: center

   *Configuring a MongoDB data store.*

For more details about the usage of this data store please check the :geotools:`GeoTools MongoDB plugin documentation
<library/data/mongodb.html>`.