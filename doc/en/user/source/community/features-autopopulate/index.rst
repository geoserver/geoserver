.. _community_wfsautopopulate:

Features-Autopopulate Extension
===============================

The Features Autopopulate plug-in listens to transactions (so far only issued by WFS), and autopopulates the feature type attributes according to the values retrieved from the properties file.

The plugin uses a custom TransactionCallback that alters the insert/update WFS-T operations, forcing in specific values into them, based on configuration files.

To support configuration for multiple layers, the easiest thing is to place a configuration, file in the directories of the layers themselves, pretty much like the featureinfo templates.

A "transactionCustomizer.properties" file that contains a set of names and CQL expressions
 e.g.:

 ```
 UTENTE=env('GSUSER') # this will be replaced with the current user see @EnviromentInjectionCallback
 AGGIORNAMENTO=now()  # this will be replaced with the current date
 ```

To keep things simple, the expressions will just use environment variables, but not see the other values provided in the update/insert, and will not be differentiated by insert/update cases.
