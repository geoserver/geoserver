.. _app-schema.configuration:

Configuration
=============

Configuration of an app-schema complex feature type requires manual construction of a GeoServer data directory that contains an XML mapping file and a ``datastore.xml`` that points at this mapping file. The data directory also requires all the other ancillary configuration files used by GeoServer for simple features. GeoServer can serve simple and complex features at the same time.


Workspace layout
----------------

The GeoServer data directory contains a folder called ``workspaces`` with the following structure::

    workspaces
        - gsml
            - SomeDataStore
                - SomeFeatureType
                    - featuretype.xml
                - datastore.xml
                - SomeFeatureType-mapping-file.xml

.. note:: The folder inside ``workspaces`` must have a name (the workspace name) that is the same as the namespace prefix (``gsml`` in this example).


Datastore
---------

Each data store folder contains a file ``datastore.xml`` that contains the configuration parameters of the data store. To create an app-schema feature type, the data store must be configured to load the app-schema service module and process the mapping file. These options are contained in the ``connectionParameters``:

* ``namespace`` defines the XML namespace of the complex feature type.

* ``url`` is a ``file:`` URL that gives the location of the app-schema mapping file relative to the root of the GeoServer data directory.

* ``dbtype`` must be ``app-schema`` to trigger the creation of an app-schema feature type.

