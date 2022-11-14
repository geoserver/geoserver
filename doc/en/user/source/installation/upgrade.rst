.. _installation_upgrade:

Upgrading existing versions
===========================

.. warning:: Be aware that some upgrades are not reversible, meaning that the data directory may be changed so that it is no longer compatible with older versions of GeoServer. See :ref:`datadir_migrating` for more details.

The general GeoServer upgrade process is as follows:

#. Back up the current data directory. This can involve simply copying the directory to an additional place.

#. Make sure that the current data directory is external to the application (not located inside the application file structure).

   Check the GeoServer Server status page to double check the data directory location.

#. Make a note of any extensions you have installed.

   * The GeoServer :menuselection:`About --> Server Status` page provides a :guilabel:`Modules` tab listing the modules installed.
   * Some extensions include more than one module, as an example the WPS extension is listed as :file:`gs-wps-core` and :file:`gs-web-wps`.

#. Uninstall the old version and install the new version.
   
   * Download :website:`maintenance <release/maintenance>` release to update existing installation
   * Download :website:`stable <release/stable>` release when ready to upgrade
   
#. Be sure to download and install each extension used by your prior installation.

#. Make sure that the new installation continues to point to the same data directory used by the previous version.

Notes on upgrading specific versions
------------------------------------

Log4J Upgrade (GeoServer 2.21 and newer)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As of GeoServer 2.21, the logging system used by GeoServer has been upgraded from Log4J 1.2 to Log4J 2.

* GeoServer now uses :file:`xml` files for the built-in logging profiles (previously :file:`properties` files were used).

* The built-in logging profiles are upgraded with :file:`xml` files:

  | :file:`DEFAULT_LOGGING.xml`
  | :file:`DEFAULT_LOGGING.properties.bak`
  
  
* A backup of the prior :file:`properties` files are created during the upgrade process. If you had previously made any customizations to a built-in profiles these backup files may be used as a reference when customizing the xml file.

* Log4J 2 does have the ability to read Log4j 1.2 properties files although not all features are supported.

  Any custom :file:`properties` files you created will continue to be available for use.
  
* If necessary you can recover a customization you performed to a built-in logging profile by restoring to a different filename. To recover a customization from :file:`PRODUCTION_LOGGING.properties.bak` rename the file to  :file:`PRODUCTION_LOGGING.properties.bak` to :file:`CUSTOM_LOGGING.properties`.

* If you never plan to customize the built-in logging profiles the ``UPDATE_BUILT_IN_LOGGING_PROFILES=true`` system property will always ensure you have our latest recommendation.

JTS Type Bindings (GeoServer 2.14 and newer)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As of GeoServer 2.14, the output produced by :ref:`REST <rest>` featuretype and structured coverage requests using a different package name (``org.locationtech`` instead of ``com.vividsolutions``) for geometry type bindings, due to the upgrade to JTS (Java Topology Suite) 1.16.0. For example:

Before::

    ...
    <attribute>
      <name>geom</name>
      <minOccurs>0</minOccurs>
      <maxOccurs>1</maxOccurs>
      <nillable>true</nillable>
      <binding>com.vividsolutions.jts.geom.Point</binding>
    </attribute>
    ...

After::

    ...
    <attribute>
      <name>geom</name>
      <minOccurs>0</minOccurs>
      <maxOccurs>1</maxOccurs>
      <nillable>true</nillable>
      <binding>org.locationtech.jts.geom.Point</binding>
    </attribute>
    ...


Any REST clients which rely on this binding information should be updated to support the new names.

GeoJSON encoding (GeoServer 2.6 and newer)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As of GeoServer 2.6, the GeoJSON produced by the WFS service no longer uses a non-standard encoding for the CRS. To re-enable this behavior for compatibility purposes, set ``GEOSERVER_GEOJSON_LEGACY_CRS=true`` as a system property, context parameter, or environment variable.