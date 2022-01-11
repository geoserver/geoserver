.. _data_shapefiles_directory:

空间文件目录
==========================

目录存储自动执行将多个shapefile加载到GeoServer中的过程。加载包含多个shapefile的目录将自动将每个shapefile添加到GeoServer。

.. note:: 虽然GeoServer对shapefile格式提供了强大的支持，但在生产环境中不建议选择该格式。诸如PostGIS之类的数据库更适合生产，并提供更好的性能和可伸缩性。有关更多信息，请参见 :ref:`production` 。

添加目录
------------------

首先，导航至 :menuselection:`Stores --> Add a new store --> Directory of spatial files`.

.. figure:: images/directory.png
   :align: center

   *添加空间文件目录作为存储库*

.. list-table::
   :widths: 20 80

   * - **选项**
     - **描述**
   * - :guilabel:`Workspace`
     - 包含存储的工作空间的名称。这也将是从存储中的shapefile创建的所有图层名称的前缀。
   * - :guilabel:`Data Source Name`
     - GeoServer已知的存储名称。
   * - :guilabel:`Description`
     - 目录存储的描述。
   * - :guilabel:`Enabled`
     - 启用存储。如果禁用，则不会提供任何shapefile中的数据。
   * - :guilabel:`URL`
     - 目录的位置。可以是绝对路径 (例如 :file:`file:C:\\Data\\shapefile_directory`) 或相对于数据目录的路径 (例如 :file:`file:data/shapefile_directory`。
   * - :guilabel:`namespace`
     - 与存储关联的命名空间。通过更改工作区名称来更改此字段。

完成后，点击 :guilabel:`Save`.

配置 shapefiles
----------------------

目录存储中包含的所有shapefile将作为目录存储的一部分加载，但是需要将它们分别配置为可以由GeoServer服务的新图层。有关如何添加和编辑新 :ref:`data_webadmin_layers` 。