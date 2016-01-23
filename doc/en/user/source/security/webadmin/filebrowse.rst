.. _security_webadmin_filebrowse:

File Browsing
=============

The GeoServer web admin employs a file browser dialog that will expose locations of the 
file system other than the GeoServer directory. These locations include the root of the 
file system and the users home directory. In highly secure and multi-tenant environments 
disabling this feature may be desired. 

The property ``GEOSERVER_FILEBROWSER_HIDEFS`` can be used to disable this functionality.
When set to ``true`` only the GeoServer data directory will be exposed through the file
browser.

The property is set through one of the standard means:

* ``web.xml`` ::

    <context-param>
      <param-name>GEOSERVER_FILEBROWSER_HIDEFS</param-name>
      <param-value>true</param-value>
    </context-param>
  
* System property ::

    -DGEOSERVER_FILEBROWSER_HIDEFS=true

* Environment variable ::

    export GEOSERVER_FILEBROWSER_HIDEFS=true