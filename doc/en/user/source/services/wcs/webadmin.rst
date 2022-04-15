.. _services_webadmin_wcs:

WCS settings
============

This page details the configuration options for WCS in the web administration interface.

The Web Coverage Service (WCS) provides few options for changing coverage functionality. While various elements can be configured for WFS and WMS requests, WCS allows only metadata information to be edited. This metadata information, entitled :guilabel:`Service Metadata`, is common to WCS, WFS and WMS requests. 

.. figure:: img/services_WCS.png
   
   WCS Configuration page

Workspace
---------

Select :guilabel:`workspace` empty to configure global WCS settings.

See section on :ref:`workspace_services` to override settings used by WCS :ref:`virtual_services`.

WCS Service Metadata
--------------------

For a description of WCS settings, see the section on :ref:`service_metadata`.

i18n Settings
-------------

Select the default language for the WCS Service.

.. figure:: img/i18n_default_language.png
   
   Default language

See the :ref:`internationalization` section for a description of how this setting is used.

Compression Settings
--------------------

Specify the default level for Deflate compression when requesting a coverage in TIFF format with Deflate compression.

.. figure:: img/wcs_default_deflate.png
   
   Default Deflate compression level

