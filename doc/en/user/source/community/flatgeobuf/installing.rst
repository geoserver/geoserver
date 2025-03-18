.. _flatgeobuf_installing:

Installing WFS FlatGeobuf output format
=======================================

To install the WFS FlatGeobuf output format extension:

#. Download the **flatgeobuf** community extension from the appropriate `nightly build <https://build.geoserver.org/geoserver/>`_. The file name is called :file:`geoserver-*-flatgeobuf-plugin.zip`, where ``*`` matches the version number of GeoServer you are using. 

#. Extract this these files and place the JARs in ``WEB-INF/lib``.

#. Perform any configuration required by your servlet container, and then restart.

The result will be a new datastore (which can be a file or a directory) and a new WFS output format (``&outputFormat=application/flatgeobuf``)
