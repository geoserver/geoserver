.. _opensearch_eo_install:

Installing the OpenSearch for EO module
=======================================

The installation of the module requires several steps:

* Setting up a PostGIS database with the required schema
* Install the OpenSearch for EO plugin and configure it
* Fill the database with information about collection and metadata

Setting up the PostGIS database
-------------------------------

Create a PostgreSQL database and run the following SQL script::

  https://raw.githubusercontent.com/geoserver/geoserver/main/src/community/oseo/oseo-core/src/test/resources/postgis.sql

.. literalinclude:: /../../../../src/community/oseo/oseo-core/src/test/resources/postgis.sql
   :language: sql
   
Downloading and installing the OpenSearch extension
---------------------------------------------------

This module is a community module pending graduation, and is available alongside the official release
for production testing.

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Pending** community plugins download **OpenSearch (EO)**.
   
   * |release| example: :download_pending:`opensearch-eo-plugin`
  
   * |version| example: :nightly_extension:`opensearch-eo-plugin`
   
   The website lists community modules for active nightly builds providing feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier versions.
   
#. Download the plugin zip file, and unzip its contents in the GeoServer unpacked WAR lib directory,
   e.g., :file:`geoserver/WEB-INF/lib`

#. Restart GeoServer

   .. figure:: images/admin.png
      :align: center

      The GeoServer home page after the OpenSearch for EO module installation.
