.. _data_featurepregen:

预用特性
=======================

.. note:: GeoServer并未内置支持预用功能。它必须通过扩展安装。

安装预用功能插件
------------------------------------------------

#.从 :website:`GeoServer download page <download>` . 下载预用功能插件。

   .. warning:: 确保将扩展版本与GeoServer实例的版本匹配!

#. 将存档内容提取到GeoServer安装目录 ``WEB-INF/lib`` 中。

添加预通用特征数据存储
-------------------------------------------

如果正确安装了扩展名，则在创建新的数据存储区时将列出, :guilabel:`Generalized Data Store` 作为选项。

.. figure:: images/featurepregencreate.png
   :align: center

   *向量数据存储列表中的通用数据存储*

配置预用特征数据存储
------------------------------------------------

.. figure:: images/featurepregenconfigure.png
   :align: center

   *配置预用特征数据存储*

有关详细说明，请参见 doc:`Tutorial</tutorials/feature-pregeneralized/feature-pregeneralized_tutorial>`
