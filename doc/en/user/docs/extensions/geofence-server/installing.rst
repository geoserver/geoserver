.. _geofence_server_install:

Installing the GeoServer GeoFence Server extension
==================================================

#. Visit the :website:`website download <download>` page, locate your release, and download: :download_extension:`geofence-server`
   
   The download link will be in the :guilabel:`Extensions` section under :guilabel:`Other`.
   
   .. warning:: Ensure to match plugin (example |release| above) version to the version of the GeoServer instance.

 #. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

    .. note:: By default GeoFence will store this data in a `H2 database <http://www.h2database.com/html/main.html>`__ and the database schema will be automatically managed by Hibernate.
    
       The `GeoFence documentation <https://github.com/geoserver/geofence/wiki/GeoFence-configuration>`__ explains how to configure a different backed database and configure Hibernate behavior.
 
 #. Add the following system variable among the JVM startup options (location varies depending on installation type): ``-Dgwc.context.suffix=gwc``

 #. Restart GeoServer
