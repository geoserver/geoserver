.. _data_featurepregen:

Pregeneralized Features
=======================

.. note:: GeoServer does not come built-in with support for Pregeneralized Features; it must be installed through an extension.

Installing the Pregeneralized Features extension
------------------------------------------------

#. Download the Pregeneralized Features extension from the `GeoServer download page 
   <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Adding a Pregeneralized Features data store
-------------------------------------------

If the extension is properly installed, :guilabel:`Generalized Data Store` will be listed as an option when creating a new data store.

.. figure:: images/featurepregencreate.png
   :align: center

   *Generalized Data Store in the list of vector data stores*

Configuring a Pregeneralized Features data store
------------------------------------------------

.. figure:: images/featurepregenconfigure.png
   :align: center

   *Configuring a Pregeneralized Features data store*

For a detailed description, look at the :doc:`Tutorial</tutorials/feature-pregeneralized/feature-pregeneralized_tutorial>`
