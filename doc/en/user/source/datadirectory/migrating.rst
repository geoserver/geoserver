.. _migrating_data_directory:

Migrating a Data Directory between different versions
=====================================================

Minor and major version numbers
-------------------------------

There should generally be no problems or issues migrating data directories between major and minor versions of GeoServer (i.e. from 2.0.0 to 2.0.1 and vice versa, or from 1.6.x to 1.7.x and vice versa).

Migrating between GeoServer 1.7.x and 2.0.x
-------------------------------------------

When using GeoServer 2.0.x with a data directory from the 1.7.x branch, modifications will occur to the directory **immediately** that will **make the data directory incompatible with 1.7.x**!  Below is a list of changes made to the data directory.

Files and directories added
```````````````````````````

::

  wfs.xml
  wcs.xml
  wms.xml
  logging.xml
  global.xml
  workspaces/*
  layergroups/*
  styles/*.xml

Files renamed
`````````````

* ``catalog.xml`` renamed to ``catalog.xml.old``
* ``services.xml`` renamed to ``services.xml.old``

.. note:: Reverting from GeoServer 2.0.x to 1.7.x

   In order to revert the directory to be compatible with 1.7.x again:

   #. Stop GeoServer.

   #. Delete the following files and directories::

         wfs.xml
         wcs.xml
         wms.xml
         logging.xml
         global.xml
         workspaces/*
         layergroups/*
         styles/*.xml

   #. Rename ``catalog.xml.old`` to ``catalog.xml``.

   #. Rename ``services.xml.old`` to ``services.xml``.

.. _migrating_data_directory_22x:

Migrating between GeoServer 2.1.x and 2.2.x
-------------------------------------------

The security changes that ship with GeoServer 2.2 require modifications to the ``security`` directory of the 
GeoServer data directory.

Files and directories added
```````````````````````````

::

  security/*.xml
  security/masterpw.*
  security/geoserver.jceks
  security/auth/*
  security/filter/*
  security/masterpw/*
  security/pwpolicy/*
  security/role/*
  security/usergroup/*
  
Files renamed
`````````````

  * ``security/users.properties`` renamed to ``security/users.properties.old``

.. note:: Reverting from GeoServer 2.2.x and 2.1.x

   In order to restore the GeoServer 2.1 configuration:

   #. Stop GeoServer.

   #. Rename ``users.properties.old`` to ``users.properties``.

   #. Additionally (although not mandatory) delete the following files and directories::

        security/
          config.xml
          geoserver.jceks
          masterpw.xml
          masterpw.digest
          masterpw.info
          auth/
          filter/
          masterpw/
          pwpolicy/
          role/
          usergroup/

Migrating between GeoServer 2.2.x and 2.3.x
-------------------------------------------

The security improvements that ship with GeoServer 2.3 require modifications to the ``security`` directory of the 
GeoServer data directory.

Files and directories added
```````````````````````````

::

  security/filter/roleFilter/config.xml
  
Files modified
``````````````
::

    security/filter/formLogout/config.xml
    security/config.xml
  
Backup files
````````````
::

    security/filter/formLogout/config.xml.2.2.x
    security/config.xml.2.2.x
  
.. note:: Reverting from GeoServer 2.3.x

   In order to restore the GeoServer 2.2 configuration:

   #. Stop GeoServer.

   #. Copy ``security/config.xml.2.2.x`` to ``security/config.xml``.

   #. Copy ``security/filter/formLogout/config.xml.2.2.x`` to ``security/filter/formLogout/config.xml``.

   #. Additionally (although not mandatory) delete the following files and directories::


        security/
          filter/
            roleFilter/
               config.xml
            formLogout/
               config.xml.2.2.x
          config.xml.2.2.x        

Migrating between GeoServer 2.5.x and 2.6.x
-------------------------------------------

The catalog naming conventions became more strict in 2.6, invalidating certain characters within names. This is because certain protocols will not work correctly with certain characters in the name. In 2.6.3 and forward, the naming restrictions are automatically set to relaxed through the STRICT_PATH java system property variable. In order to ensure your names will work with all protocols, set this variable to true. ::

    java -DSTRICT_PATH=true Start

This will invalidate all of the following characters:
  
* star (*)

* colon (:)

* comma (,)

* single quote (')

* ampersand (&)
  
* question mark (?)
  
* double quote (")
  
* less than (<)
  
* greater than (>)
  
* bar (|)

Be warned that some requests or protocols may behave unexpectedly when these characters are allowed. We recommend that you update your catalog and enable strict mode to ensure you follow appropriate naming conventions.