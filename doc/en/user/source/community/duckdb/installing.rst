.. _duckdb_installing:

Installing the DuckDB Extension
===============================

.. warning:: Make sure to match the module version with your GeoServer version.

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact GeoServer version in use.

#. Visit the :website:`website download <download>` page, switch to the **Development** tab,
   and locate the nightly build matching your GeoServer version.

#. Follow the **Community Modules** link and download the DuckDB module:

   * |version| example: :nightly_community:`duckdb`

   The website lists active nightly builds to provide feedback to developers.
   You may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the downloaded archive into :file:`WEB-INF/lib` of your GeoServer installation.

#. Restart GeoServer.

.. note::

   If the GeoParquet community module is already installed, the DuckDB JDBC driver may already be present.
   You still need to install the DuckDB extension to register the DuckDB store type in GeoServer.

Verification
------------

After restart:

#. Open :menuselection:`Stores > Add new Store`.
#. Confirm **DuckDB** appears under vector data sources.
