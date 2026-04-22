# Upgrading GeoServer 3

## Upgrading GeoServer 3.0.0 Guidance

GeoServer 3.0.x is scheduled for release in April, 2026.

The upgrade to GeoServer 3.0 is seamless maintaining the same data directory and Java 17 environment as used previously for GeoServer 2.28.

### JDK 17 Required

Just like in GeoServer 2.28 series, GeoServer 3.0 requires Java 17 to run. Be sure to install Java 17 before upgrading to GeoServer 3.0.

Reference:

* [Java Considerations](../production/java.md)

### Tomcat 11.0 Required

GeoServer 3.0 makes the transition to Jakarta EE Servlet 6.1, and requires Tomcat 11.0 for those using WebArchive distribution.

Reference:

* [Web Archive Installation](war.md)
* [Container Considerations](../production/container.md)

### Core modules downgraded to extension

Some of the less used core modules have been downgraded to extensions in GeoServer 3.0. If you were using any of the following features, be sure to install the corresponding extension after upgrading:

* [WCS 1.0](../services/wcs/install.md)
* [WCS 1.1](../services/wcs/install.md)
* [World Image](../data/raster/worldimage.md#image_install) raster data source
* [ArcGRID](../data/raster/arcgrid.md#arcgrid_install) raster data source

### H2 Datastore Removal

As of GeoServer 3.0, the H2 datastore extension (`gs-h2`) has been removed and is no longer available for use as a GeoServer vector datastore.

If your installation contains any H2-based datastores, migrate those layers to another supported datastore before upgrading to GeoServer 3.0.

After upgrading, review the startup logs for missing datastore errors and verify all layers and services load as expected.

### Log Location Configuration

As of GeoServer 3.0, the **Log location** setting has been removed from the Admin Console (**Global Settings** page) and is no longer configurable via the REST API.

The [log file location](../configuration/logging.md#logging_location) must now be set using the `GEOSERVER_LOG_LOCATION` application property which can be set as system property, environment variable, or servlet context parameter:

```shell
# System property
-DGEOSERVER_LOG_LOCATION=/var/log/geoserver/geoserver.log

# Environment variable
export GEOSERVER_LOG_LOCATION=/var/log/geoserver/geoserver.log
```

Any existing `location` value in the data directory `logging.xml` file is retained for backward compatibility but is ignored at runtime. REST API clients that send a `location` field in PUT requests to `/rest/logging` will receive a warning in the server logs; the value is silently discarded.

### NetCDF Index removal

Starting with GeoServer 3.0, NetCDF plugin (and coverage multidim machinery) has been simplified and is now self contained. With this improvement NetCDF no longer needs a databse or `idx` files to operate. In particular, it no longer relies on: 

- a database (e.g., H2 or PostGIS) to map temporal and elevation domains to image indices, 
- nor on the auxiliary **`.idx`** binary files previously used to link image indices to NetCDF dimension coordinates. 

These relationships are now resolved directly from the NetCDF structure at runtime, reducing configuration overhead, 
improving portability, and eliminating synchronization issues between datasets and external indexes.

The NetCDF plugin no longer generates the legacy binary **`.idx`** files and the embedded H2 database previously used for indexing temporal and elevation domains.
All indexing is now handled in-memory and derived directly from the NetCDF dataset structure at runtime, eliminating the need to manage or clean up external index artifacts.

**Cleanup of Existing Files**

Existing installations may still contain legacy index artifacts that are not needed anymore in GeoServer 3. 
Hidden directories named like: `.<FILENAME_HASH>` (e.g. `.polyphemus_20130301_710e4edfc7d0ff89faf932b208ca22bda37a6921`) are companion folders created next to the NetCDF file (e.g. `polyphemus_20130301.nc`) and used to store:
- the H2 db files:
  - the primary H2 data storage file. (e.g. `polyphemus_20130301.data.db`)
  - the index storage file (e.g. `polyphemus_20130301.index.db`)
  - the transaction log (e.g. `polyphemus_20130301.4.log.db`)
  - the debug/trace log (e.g. `polyphemus_20130301.trace.db`)

- the binary index file:
  - FILENAME.idx (e.g. `polyphemus_20130301.idx`)

All such files are now obsolete and can be deleted without affecting functionality.
If the `NETCDF_DATA_DIR JAVA_OPT` is configured, these hidden folders are grouped within the specified directory instead of being located alongside each NetCDF file.

**Mosaic of NetCDF files**

ImageMosaics built on NetCDF files could previously depend on an AuxiliaryDatastoreFile parameter defined in the  `indexer.xml `. This parameter referenced a  `netcdf_datastore.properties ` file containing connection settings for a shared database used to store the NetCDF entries catalog (allowing a single DB for the entire mosaic instead of one H2 database per NetCDF file).

With the GeoServer 3 refactoring, this configuration is no longer required. The  `FILENAME_datastore.properties ` file can be safely removed, along with the corresponding entry in the indexer.xml. Update the configuration as follows, going from (for example):
```xml
  <parameters>
    <parameter name="AuxiliaryFile" value="_auxiliary.xml" />
    <parameter name="AbsolutePath" value="true" />
    <parameter name="AuxiliaryDatastoreFile" value="netcdf_datastore.properties" />
  </parameters>
```
  to
```xml
  <parameters>
    <parameter name="AuxiliaryFile" value="_auxiliary.xml" />
    <parameter name="AbsolutePath" value="true" />
  </parameters>
```
### OAuth and Keycloak and migrate to new OIDC plugin

GeoServer 3 marks the end-of-life for several popular community plugins - which all have a single replacement in the [OIDC plugin](https://docs.geoserver.org/main/en/user/community/oidc/) module.

* If you previously used keycloak support, there are setup instructions for [configuring with keycloak](https://docs.geoserver.org/main/en/user/community/oidc/oauth2/keycloak/).
* If you previously used an OAuth2 integration can find indivudal setup instructions for [google](https://docs.geoserver.org/main/en/user/community/oidc/oauth2/google/), [Azure](https://docs.geoserver.org/main/en/user/community/oidc/oauth2/azure/), and [GitHub](https://docs.geoserver.org/main/en/user/community/oidc/oauth2/azure/).

For more information, and installation instructions, use the user guide [OAuth2 OpenID Connect](https://docs.geoserver.org/main/en/user/community/oidc/) page. 

## How to upgrade

Care should be taken to backup your data directory, outlined as the first step in the upgrade process below.

!!! warning
    Be aware that some upgrades are not reversible, meaning that the data directory may be changed so that it is no longer compatible with older versions of GeoServer. See [Migrating a data directory between versions](../datadirectory/migrating.md) for more details.

!!! note
    Upgrade instructions for [GeoServer 2](upgrade2.md) are also available.

## General Upgrade Process

The general GeoServer upgrade process is as follows:

1.  Back up the current data directory. This can involve simply copying the directory to an additional place.

2.  Make sure that the current data directory is external to the application (not located inside the application file structure).

    Check the GeoServer Server status page to double check the data directory location.

3.  Make a note of any extensions you have installed.
    
    - The GeoServer **About --> Server Status** page provides a **Modules** tab listing the modules installed.
    - Some extensions include more than one module, as an example the WPS extension is listed as **`gs-wps-core`** and **`gs-web-wps`**.

4.  Uninstall the old version and install the new version.
    
    - Download [maintenance](https://geoserver.org/release/maintain) release to **update** existing installation.
       
       There should be no problems or issues updating data directories between patch versions of GeoServer (for example, from 2.28.0 to 2.28.1).
       
       It is also generally possible to downgrade a minor update and maintain data directory compatibility (for example from 2.28.1 to 2.28.0).
    
    - Download [stable](https://geoserver.org/release/stable) release when ready to **upgrade**.
       
       There should rarely be any issues involved with upgrading between minor versions (for example, from 2.27.x to 2.28.x).

5. Always check the upgrade guidance, as upgrading GeoServer may not be reversible, since:

   - newer versions of GeoServer may make backwards-incompatible changes to the data directory
     (when upgrading from 2.20 to 2.21 the logging library changed), or
     
   - Newer versions may change what extensions are provided
     (as is the case when moving from 2.28 to 3.0 with the OIDC plugin replacing the keycloak plugin).

6.  Be sure to download and install each extension used by your prior installation.

7.  Make sure that the new installation continues to point to the same data directory used by the previous version.

8. During initial startup check the logs for any warnings that may need to be addressed.

## Q: How often should I upgrade GeoServer

GeoServer [release schedule](https://github.com/geoserver/geoserver/wiki/Release-Schedule) follows a predictabe time boxed release cycle, maintaining "stable" and "maintenance" releases, over the course of a year.

- Plan to upgrade GeoServer **at least twice a year** as new stable releases are made in march

  Once the release you are using has entered "maintenance" it is a good idea to upgrade (before the release is no longer supported).

  GeoServer provides some overlap between "stable" and "maintenance" releases to provide you a window of opportunity to upgrade between supported versions.

- GeoServer [security policy](https://github.com/geoserver/geoserver/security/policy) indicates each release is supported with bug fixes for a year, with releases made approximately every two months.

  You may also contact our service providers for extended support beyond this timeframe.

- Monitor release announcements in case a new release is made that provides "Security Considerations" guidance.

  It is always advisable to stay up to date with security patches. The blog post will indicate when the update is urgent, and several releases will be made concurrently (for both stable and maintenance) when urgent action is required.

!!! note
      Do not wait for a release to fall out of support before upgrading. Doing so places you in a position of having to perform an upgrade quickly with a lot of pressure in the event a security vulnerability is announced.

!!! warning  
      If you do see several releases being made concurrently, in response to an urgent vulnerability, the developers will not be in a position to tell you what is going on. Their goal is to provide you an opportunity to upgrade prior to public disclosure.
    
      Those seeking more information, or with a legal obligation to be informed, are welcome to volunteer on the geoserver-security email list. See [Security Process](../../developer/policies/security.md) for details on how to participate.

### Troubleshooting

#### Forgetting to include an Extension

The most common difficulty when updating GeoServer is forgetting to include an extension.

- This may result in the application being unable to start up, as it attempts to read a configuration file without the corresponding extension that understands the setting.
- This may result in missing functionality, for example forgetting to install an output format will result in that format not being available for use.

#### Upgrading more than one version

If it has been a while since you have upgraded GeoServer be cautious when upgrading from an unsupported release of GeoServer all the way to the latest release. Consider **first trying a quick update** in one go, but be prepared to perform a sequential **update to each major release in turn**, applying the guidance in the section below at each stage.

!!! note
    Trying a quick update GeoServer 2.26.0 to GeoServer 3.0.
    
    1.  Back up the current data directory
    
    2.  Check the [Download](https://geoserver.org/download) page and download the target release:
    
        - GeoServer 2.28.0
    
        You may also make a note of the version of Java to download:
    
        - OpenJDK 17
    
    3.  Perform the upgrade in one go, checking the guidance for [GeoServer 2](upgrade2.md) or [GeoServer 3](upgrade3.md) work to perform.
        
        The application property `ENTITY_RESOLUTION_UNRESTRICTED` is noted, if you are affected by a change in XML Parsing. This should only affect Application Schema that made use of the **Unrestricted XML External Entity Resolution** setting.
        
        The application property `GEOSERVER_DATA_DIR_LOADER_ENABLED` is noted as an option if any deadlock occurs during startup due to an improvement in startup performance.
        
        A wide a range of *Content Security Policy* restrictions have been introduced, and very clear instructions noted to double check `proxy_base_url` is correct (as the GeoServer user interface will now detect and block a misconfigured system). The application property `org.geoserver.web.csp.strict=false` is available to temporarily disable this safety measure if you are locked out.
    
        Installation of Java 17 is required.
    
        Out of an abundance of caution testing raster layers is advisable due to wholesale change of the image processing engine.
        
        Changing the log file location is no longer available in global settings, and can be performed using the application property `GEOSERVER_LOG_LOCATION`.
        
        If your data directory used WorldImage or ArcGRID these formats are now available as extensions.
        You may choose to convert your data to GeoTIFF (recommended) or install the extensions to maintain compatibility.
        
        If your users depend on WCS 1.0 or WCS 1.1 this functionality is now provided as an extension.
        If you wish to continue making these services available you may install these extensions.
        
        NetCDF no longer requires as many files on disk (allowing the cleanup of unused hidden directories, `.idx` files, and `db` files).
    
    4.  Review the logs during startup, and test to ensure the application is working as expected.
    
        If you encounter problems consider planning a sequential update as in the next example.

!!! note
    Planning a sequential update from GeoServer 2.26.0 to GeoServer 3.0.
    
    1.  Check the [Download](https://geoserver.org/download) page and download the releases needed to make the transition:
        
        - GeoServer 2.26.4
        - GeoServer 2.27.3
        - GeoServer 2.28.0
        - GeoServer 3.0
        
        You may also make a note of the versions of Java to download:
        
        - OpenJDK 17
    
    2.  Perform each update in sequence, checking the [GeoServer 2](upgrade2.md) guidance for any work to perform.
    
        - Updating from GeoServer 2.26.0 to GeoServer 2.26.4
    
            The application property `ENTITY_RESOLUTION_UNRESTRICTED` is noted, if you are affected by a change in XML Parsing. This should only affect Application Schema that made use of the **Unrestricted XML External Entity Resolution** setting.
      
            *Review the logs during startup, and test to ensure the application is working as expected.*
     
        - Updating from GeoSerer 2.26.4 to GeoServer 2.27.3.
          
            The application property `GEOSERVER_DATA_DIR_LOADER_ENABLED` is noted as an option if any deadlock occurs during startup due to an improvement in startup performance.
            
            A wide a range of *Content Security Policy* restrictions have been introduced, and very clear instructions noted to double check `proxy_base_url` is correct (as the GeoServer user interface will now detect and block a misconfigured system). The application property `org.geoserver.web.csp.strict=false` is available to temporarily disable this safety measure if you are locked out.
            
            *Review the logs during startup, and test to ensure the application is working as expected.*
        
        - Updating from GeoServer 2.27.3 to GeoServer 2.28.0.
            
            Installation of Java 17 is required.
            
            Out of an abundance of caution testing raster layers is advisable due to wholesale change of the image processing engine.
            
            *Review the logs during startup, and test to ensure the application is working as expected.*
    
    3.  Perform each update in sequence, checking the GeoServer 3 guidance on this page for any work to perform.
    
        - Updating from GeoServer 2.28.0 to GeoServer 3.0
          
            Changing the log file location is no longer available in global settings, and can be performed using the application property `GEOSERVER_LOG_LOCATION`.
            
            If your data directory used WorldImage or ArcGRID these formats are now available as extensions.
            You may choose to convert your data to GeoTIFF (recommended) or install the extensions to maintain compatibility.
            
            If your users depend on WCS 1.0 or WCS 1.1 this functionality is now provided as an extension.
            If you wish to continue making these services available you may install these extensions.
            
            NetCDF no longer requires as many files on disk (allowing the cleanup of unused hidden directories, `.idx` files, and `db` files).
