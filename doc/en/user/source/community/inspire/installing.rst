.. _community_inspire_installing:

Installing the INSPIRE extension
================================

As the INSPIRE extension is a community extension, it is available on the `nightly download <http://gridlock.opengeo.org/geoserver/>`_ server, and not on the `GeoServer download <http://geoserver.org/display/GEOS/Download>`_ pages.

.. note:: Some versioned releases of this extension are also mirrored at http://files.opengeo.org/inspire .  Download the archive that matches the version of GeoServer and continue at step #3.

#. Navigate to the directory that matches the branch of your version of GeoServer (i.e. ``2.1.x`` for version 2.1.2).

#. Inside the ``community-latest`` directory, find and download the INSPIRE archive.  (Example: ``geoserver-2.1-SNAPSHOT-inspire-plugin.zip``)

#. Extract the archive and copy the contents into the ``<GEOSERVER_ROOT>/WEB-INF/lib`` directory.

#. Restart GeoServer.



To verify that the extension was installed successfully, please see the next section on :ref:`community_inspire_using`.
