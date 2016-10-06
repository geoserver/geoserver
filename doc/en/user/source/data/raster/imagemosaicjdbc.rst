.. _data_imagemosaicjdbc:

Image Mosaic JDBC
=================

.. note:: GeoServer does not come built-in with support for Image Mosaic JDBC; it must be installed through an extension. Proceed to :ref:`imagemosaicjdbc_install` for installation details.

.. _imagemosaicjdbc_install:

Installing the JDBC Image Mosaic extension
------------------------------------------

#. Download the JDBC Image Mosaic extension from the `GeoServer download page 
   <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Adding an Image Mosaic JDBC data store
--------------------------------------

Once the extension is properly installed :guilabel:`Image Mosaic JDBC` will be an option in the :guilabel:`Raster Data Sources` list when creating a new data store.

.. figure:: images/imagemosaicjdbccreate.png
   :align: center

   *Image Mosaic JDBC in the list of vector data stores*

Configuring an Image Mosaic JDBC data store
-------------------------------------------

.. figure:: images/imagemosaicjdbcconfigure.png
   :align: center

   *Configuring an Image Mosaic JDBC data store*
 
For a detailed description, look at the :doc:`Tutorial</tutorials/imagemosaic-jdbc/imagemosaic-jdbc_tutorial>` 
