.. _config_REST:

REST Configuration
==================

.. note:: For more information, please see the section on :ref:`rest`.

The RESTful API allows to create new stores and append new granules to mosaics via file uploads. By default the new stores and granules are saved with
the following directory structure::

	$GEOSERVER_DATA_DIR/data/<workspace>/<store>[/<file>]
	
For changing the `Root Directory` from `$GEOSERVER_DATA_DIR/data` to another directory, the user can define a parameter called `Root Directory path` 
inside :ref:`Global Settings Page <config_globalsettings>` and :ref:`Workspace Settings Page <data_webadmin_workspaces>`. 

In order to avoid cross workspace contamination, the final path will always be::

	${rootDirectory}/workspace/store[/<file>]
	
Path remapping is achieved by using the default implementation of the **RESTUploadPathMapper** interface. This interface gives the possibility to also 
map the file position inside the store, which could be useful for harvesting files into an existing mosaic DataStore.