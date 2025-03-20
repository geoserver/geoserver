.. _monitor_micrometer_installation:

Installing the Monitor Micrometer Extension
===========================================

#. Visit the :website:`website download <download>` page and download :download_community:`monitor-micrometer`.

#. Extract the downloaded archive and copy the JAR files into the servlet container's ``WEB-INF/lib`` directory.

#. Restart GeoServer.

.. note:: For the module to work, the :ref:`monitor_extension` extension must also be installed.


Installing the Monitor Micrometer Extension with Docker
-------------------------------------------------------

To run the GeoServer Docker image with the Monitor Micrometer extension installed, use the following command:

   .. only:: not snapshot

      .. parsed-literal::

         docker run -it -p8080:8080 \\
           --env INSTALL_EXTENSIONS=true \\
           --env STABLE_EXTENSIONS="monitor" \\
           --env COMMUNITY_EXTENSIONS="monitoring-micrometer" \\
           docker.osgeo.org/geoserver:|release|

   .. only:: snapshot

      .. parsed-literal::

         docker run -it -p8080:8080 \\
           --env INSTALL_EXTENSIONS=true \\
           --env STABLE_EXTENSIONS="monitor" \\
           --env COMMUNITY_EXTENSIONS="monitoring-micrometer" \\
           docker.osgeo.org/geoserver:|version|.x

If using GeoServer in Docker Compose, use this instead:

   .. only:: not snapshot

      .. parsed-literal::

         services:
           geoserver:
             image: docker.osgeo.org/geoserver:|release|
             ports:
               - "8080:8080"
             environment:
               INSTALL_EXTENSIONS: true
               STABLE_EXTENSIONS: "monitor"
               COMMUNITY_EXTENSIONS: "monitoring-micrometer"

   .. only:: snapshot

      .. parsed-literal::

         services:
           geoserver:
             image: docker.osgeo.org/geoserver:|version|.x
             ports:
               - "8080:8080"
             environment:
               INSTALL_EXTENSIONS: true
               STABLE_EXTENSIONS: "monitor"
               COMMUNITY_EXTENSIONS: "monitoring-micrometer"
