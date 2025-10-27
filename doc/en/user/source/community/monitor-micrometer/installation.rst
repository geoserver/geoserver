.. _monitor_micrometer_installation:

Installing the Monitor Micrometer Extension
===========================================

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. For the module to work, the :ref:`monitor_extension` extension must also be installed.

   Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Miscellaneous** extensions download **Monitor (Core)**.

   * |release| example: :download_extension:`monitor`
   * |version| example: :nightly_extension:`monitor`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download `monitor-micrometer` zip archive.
   
   * |version| example: :nightly_community:`monitor-micrometer`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

#. Restart GeoServer.

Installing the Monitor Micrometer Extension with Docker
-------------------------------------------------------

To run the GeoServer Docker image with the Monitor Micrometer extension installed, use the following command:

|release| example:

.. parsed-literal::

   docker run -it -p8080:8080 \\
     --env INSTALL_EXTENSIONS=true \\
     --env STABLE_EXTENSIONS="monitor" \\
     --env COMMUNITY_EXTENSIONS="monitor-micrometer" \\
     docker.osgeo.org/geoserver:|release|

|version| example:

.. parsed-literal::

   docker run -it -p8080:8080 \\
     --env INSTALL_EXTENSIONS=true \\
     --env STABLE_EXTENSIONS="monitor" \\
     --env COMMUNITY_EXTENSIONS="monitor-micrometer" \\
     docker.osgeo.org/geoserver:|version|.x

If using GeoServer in Docker Compose, use this instead:

|release| example:

.. parsed-literal::

   services:
     geoserver:
       image: docker.osgeo.org/geoserver:|release|
       ports:
         - "8080:8080"
       environment:
         INSTALL_EXTENSIONS: true
         STABLE_EXTENSIONS: "monitor"
         COMMUNITY_EXTENSIONS: "monitor-micrometer"

|version| example:

.. parsed-literal::

   services:
     geoserver:
       image: docker.osgeo.org/geoserver:|version|.x
       ports:
         - "8080:8080"
       environment:
         INSTALL_EXTENSIONS: true
         STABLE_EXTENSIONS: "monitor"
         COMMUNITY_EXTENSIONS: "monitor-micrometer"
