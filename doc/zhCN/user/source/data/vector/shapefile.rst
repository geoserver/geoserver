.. _data_shapefile:

Shapefile
=========

shapefile 是一种流行的地理空间矢量数据格式。

.. note:: 虽然GeoServer对shapefile格式提供了强大的支持，但在生产环境中不建议选择该格式。诸如PostGIS之类的数据库更适合生产，并提供更好的性能和可伸缩性。有关更多信息，请参见 :ref:`production` 部分.

添加 shapefile
------------------

Shape文件实际上是文件的集合 (扩展名: ``.shp``, ``.dbf``, ``.shx``, ``.prj``, 或其他).  。所有这些文件都必须存在于同一目录中，以便GeoServer准确读取它们。与所有格式一样，将shapefile添加到GeoServer涉及通过 ref:`web_admin`将新存储添加到现有 ref:`data_webadmin_stores`。

.. warning::  ``.prj`` 尽管不是必需文件, ，但在与GeoServer一起使用时，强烈建议使用该文件，因为它包含有价值的投影信息。没有它，GeoServer可能无法加载您的shapefile！

首先，导航至:`存储 --> 添加新存储 --> Shapefile`.

.. figure:: images/shapefile.png
   :align: center

   *添加shapefile作为存储*

.. list-table::
   :widths: 20 80

   * - **选项**
     - **描述**
   * - :guilabel:`Workspace`
     - 包含存储的工作空间的名称。这也将是从存储创建的图层的前缀。
   * - :guilabel:`Data Source Name`
     - GeoServer已知的shapefile的名称。可以与文件名不同。工作空间名称和该名称的组合将是完整的图层名称 (例如: topp:states).
   * - :guilabel:`Description`
     - shapefile /存储的描述。 
   * - :guilabel:`Enabled`
     - 启用存储。如果未选中，则不会提供shapefile中的任何数据。
   * - :guilabel:`URL`
     - shapefile的位置。可以是绝对路径(例如 :file:`file:C:\\Data\\shapefile.shp`) 或相对于数据目录的路径(例如 :file:`file:data/shapefile.shp`.
   * - :guilabel:`namespace`
     - 与shapefile关联的命名空间。通过更改工作区名称来更改此字段。
   * - :guilabel:`create spatial index`
     - 启用自动创建空间索引的功能。
   * - :guilabel:`charset`
     - 用于从 ``.dbf`` 文件解码字符串的字符集。
   * - :guilabel:`memory mapped buffer`
       :guilabel:`Cache and reuse memory maps`
     - 启用使用内存映射的I / O，从而改善了文件在内存中的缓存。 **关闭Windows服务器**.

完成后，点击 :guilabel:`Save`.

配置shapefile图层
-----------------------------

Shapefile恰好包含一层，需要先将其添加为新层，然后才能由GeoServer提供服务。有关如何添加和编辑新图层,请参见 :ref:`data_webadmin_layers` 。
