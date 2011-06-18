.. _community_dds:

DDS/BIL(World Wind Data Formats) Extension
==========================================

This output module allows GeoServer to output imagery and terrain in formats
understood by `NASA World Wind <http://worldwind.arc.nasa.gov/java/>`_. The
mime-types supported are:

#. Direct Draw Surface (DDS) - image/dds
#. Binary Interleaved by Line(BIL) - image/bil


Installing the DDS/BIL extension
-----------------------------------

#. Download the DDS/BIL extension from the `nightly GeoServer community module builds <http://gridlock.opengeo.org/geoserver/trunk/community-latest/>`_.

    .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Checking if the extension is enabled
------------------------------------

Once the extension is installed, the provided mime-types should appear in the layer preview dropbox as shown:

.. figure:: images/bil_dds.jpg
   :align: center
   
Configuring World Wind to access Imagery/Terrain from GeoServer
---------------------------------------------------------------

Please refer to the `WorldWind Forums <http://forum.worldwindcentral.com/index.php>`_ for instructions on how to setup World Wind to work with layers 
published via GeoServer.