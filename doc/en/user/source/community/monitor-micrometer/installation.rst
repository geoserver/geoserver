.. _monitor_micrometer_installation:

Installing the Monitor Micrometer Extension
===========================================

As a community module, the package needs to be downloaded from the `nightly builds <https://build.geoserver.org/geoserver/>`_,
picking the community folder of the corresponding GeoServer series (e.g. if working on GeoServer main development branch nightly
builds, pick the zip file form ``main/community-latest``).

To install the module, unpack the zip file contents into GeoServer own ``WEB-INF/lib`` directory and
restart GeoServer.

For the module to work, the :ref:`monitor_extension` extensions must also be installed.
