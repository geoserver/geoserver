.. _mongodb:

MongoDB Data Store
==================

This module provides support for MongoDB data store. This extension is build on top of :geotools:`GeoTools MongoDB plugin
<library/data/mongodb.html>`.

Installation
------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Vector Formats** extensions download **MongoDB**.

   * |release| example: :download_extension:`mongodb`
   * |version| example: :nightly_extension:`mongodb`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

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