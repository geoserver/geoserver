.. _data_customjdbcaccess:

Custom JDBC Access for image data
=================================

.. note:: GeoServer does not come built-in with support for Custom JDBC Access; it must be installed through an extension. Proceed to :doc:`Image Mosaic JDBC<imagemosaicjdbc>` for installation details. This extension includes the support for Custom JDBC Access.


Adding a coverage based on Custom JDBC Access
---------------------------------------------

This extension  is targeted to users having a special database layout for storing their image data or use a special data base extension concerning raster data.

Read the geotools documentation for Custom JDBC Access: `<http://docs.geotools.org/latest/userguide/library/coverage/jdbc/customized.html>`_.

After developing the custom plugin, package the classes into a jar file and copy it into the :file:`WEB-INF/lib`  directory of the geoserver installation.

Create  the xml config file and proceed to the section `Configuring GeoServer` in the  :doc:`Image Mosaic JDBC Tutorial</tutorials/imagemosaic-jdbc/imagemosaic-jdbc_tutorial>`
