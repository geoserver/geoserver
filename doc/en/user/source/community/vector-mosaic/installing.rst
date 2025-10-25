.. _community_vector_mosaic_installing:

Installing Vector Mosaic Datastore
==================================

To install the Vector Mosaic datastore:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``vector-mosaic`` zip archive.
   
   * |version| example: :nightly_community:`vector-mosaic`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-vector-mosaic-plugin.zip above).

#. Perform any configuration required by your servlet container, and then restart. 

