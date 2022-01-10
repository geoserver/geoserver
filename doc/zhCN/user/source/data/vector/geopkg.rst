.. _data_geopkg_vector:

GeoPackage
==========

`GeoPackage <http://www.opengeospatial.org/projects/groups/geopackageswg/>`_ 是一种基于SQLite的标准格式，能够在一个文件中包含多个矢量和栅格数据层。

GeoPackage文件既可以用作栅格数据存储，也可以用作矢量数据存储（以便可以发布两种图层）。


添加GeoPackage矢量数据存储
-------------------------------------

安装扩展程序后，创建新的数据存储区时， :guilabel:`GeoPackage` 将成为 :guilabel:`Vector Data Sources` 列表中的一个选项。

.. figure:: images/geopackagecreate.png
   :align: center

   *GeoPackage矢量数据存储列表*
.. figure:: images/geopackageconfigure.png
   :align: center

   *配置GeoPackage矢量数据存储*

.. list-table::
   :widths: 20 80

   * - **选项**
     - **描述**
   * - :guilabel:`database`
     - 指定 geopackage 文件的URI。
   * - :guilabel:`user`
     - 访问数据库的用户名
   * - :guilabel:`passwd`
     - 访问数据库的密码
   * - :guilabel:`namespace`
     - 与数据库关联的命名空间。通过更改工作区名称来更改此字段。
   * - :guilabel:`max connections`
     - 与数据库的最大打开连接数。
   * - :guilabel:`min connections`
     - 连接池连接的最小数量。
   * - :guilabel:`fetch size`
     - 每次与数据库交互时读取的记录数。
   * - :guilabel:`Connection timeout`
     - 连接池在超时之前将等待的时间（以秒为单位）。
   * - :guilabel:`validate connections`
     - 在使用前检查连接是否有效。

完成后，点击 :guilabel:`Save`.
