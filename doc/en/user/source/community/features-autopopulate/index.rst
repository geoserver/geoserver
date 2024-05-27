.. _community_wfsautopopulate:

Features-Autopopulate Extension
===============================

The Features Autopopulate plug-in listens to transactions (so far only issued by WFS), and autopopulates the feature type attributes according to the values retrieved from the properties file.

The plugin uses a custom TransactionCallback that alters the insert/update WFS-T operations, forcing in specific values into them, based on configuration files.

To support configuration for multiple layers, the easiest thing is to place a configuration, file in the directories of the layers themselves, pretty much like the :code:`featureinfo templates`.

Assume that we want to allow GeoServer updating automatically two attributes of the :code:`topp:states` feature type every time we are going to perform an insert or update transaction.

In particular what we want to do is:

 * Update/insert into the :code:`EDITOR` the current GeoServer user.
 * Update/insert into the :code:`LAST_UPDATE` the current timestamp

We can leverage on two GeoServer :code:`CQL Expressions` that allow us to get thos values at runtime

.. code-block:: ini

   EDITOR=env('GSUSER')
   LAST_UPDATE=now()

The function :code:`env('GSUSER')` reads the system environment for a variable named :code:`GSUSER`, resolves the value and returns back the current logged-in username.

This one is a special variable automatically created by GeoServer at :code:`login` time and injected into the local environment (see :code:`#EnviromentInjectionCallback` for more details)

The function :code:`now()` gets the current timestamp and returns back the date-time.

What we need to do then is to create a file named :code:`transactionCustomizer.properties` inside the folder :code:`$GEOSERVER_DATADIR/workspaces/topp/states` with the current key-value pairs

.. code-block:: ini
  
   EDITOR=env('GSUSER')
   LAST_UPDATE=now()

*NOTE*: The extension will look for the existing properties into the feature type. If no field matches the ones specified into the :code:`transactionCustomizer.properties`, the value will be ignored. The matches are *case-sensitive*.