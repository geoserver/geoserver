.. _shapefile_quickstart:

发布 Shapefile
======================

此教程涵盖了使用 GeoServer 发布 Shapefile 的步骤。

.. note:: 此教程假设 GeoServer 已经在 ``http://localhost:8080/geoserver`` 运行。

数据准备
----------------

首先，我们收集将要发布的数据。

#. 下载 :download:`nyc_roads.zip`。 这个压缩包包含一个此教程中将用到的纽约市道路的 shapefile.

#. 解压 :file:`nyc_roads.zip` 得到新文件夹： :file:`nyc_roads`。 它包含下面四个文件::

      nyc_roads.shp
      nyc_roads.shx
      nyc_roads.dbf
      nyc_roads.prj

#. 移动 ``nyc_roads`` 目录到 ``<GEOSERVER_DATA_DIR>/data``，其中 ``<GEOSERVER_DATA_DIR>`` 指的是 :ref:`GeoServer data directory <datadir>`. 如果 GeoServer 的文件结构没有被更改，那么路径是 ``geoserver/data_dir/data/nyc_roads``。 
 
创建新 Workspace
------------------------

以下步骤中我们为 shapefile 创建新 workspace. 一个 workspace 是一种用于聚合相似图层的容器。

.. note:: 如果你希望使用一个已有的 workspace，可以跳过此步骤。通常，我们为每一个工程创建一个 workspace，它将包含相互之间有关联的 stores 和图层。

#. 在 Web 浏览器中打开 ``http://localhost:8080/geoserver``.

#. 像 :ref:`logging_in` 部分描述的那样登入 GeoServer. 

#. 导航到 :menuselection:`Data --> Workspaces`.

   .. figure:: ../../data/webadmin/img/data_workspaces.png

      Workspaces 页面

#. 点击 :guilabel:`Add new workspace` 按钮.

#. 你将会被要求输入 :guilabel:`Name` 和 :guilabel:`Namespace URI`.

   .. figure:: new_workspace.png

      配置新 workspace

#. 输入 ``nyc`` 作为 :guilabel:`Name` ， ``http://geoserver.org/nyc`` 作为:guilabel:`Namespace URI`.

   .. note:: 一个 workspace 名是用于描述项目的标识符。它必须不超过10个字符长，且不能含有空格。而一个 Namespace URI (Uniform Resource Identifier，统一资源标识符) 通常可以是一个和项目有关的、追加了一个尾部标识符来表明 workspace 的 URL.   Namespace URI 不必指向一个真实存在的 Web 地址。

   .. figure:: workspace_nycroads.png

      nyc workspace

#. 点击 :guilabel:`Submit` 按钮。 ``nyc`` workspace 将被添加到 :guilabel:`Workspaces` 列表。

创建一个 Store
--------------

创建完成 workspace 后，我们就准备好添加新 store 了。 Store 告诉 GeoServer 如何连接到 shapefile. 

#. 导航到 :menuselection:`Data-->Stores`.
    
#. 你应当能看到一个 store 的列表，它包含了 store 的类型和它隶属于的 workspace。

#. 为了添加 shapefile，你需要创建新 store. 点击 :guilabel:`Add new Store` 按钮。你将被重定向到 GeoServer 所支持的数据源列表。请注意，数据源支持是可拓展的，所以你的列表可能和图里的有所不同。

   .. figure:: stores_nycroads.png

      Stores
  
#. 点击 :guilabel:`Shapefile`. 系统会展示 :guilabel:`New Vector Data Source` 页面。

#. 首先，设置 :guilabel:`Basic Store Info`.

   * 从下拉列表中选择 workspace ``nyc`` .
   * 输入 ``NYC Roads`` 作为 :guilabel:`Data Source Name` .
   * 输入一个简短的介绍（:guilabel:`Description`） (例如 "Roads in New York City")。

#. 在 :guilabel:`Connection Parameters` 下，找到 shapefile 的 :guilabel:`URL`。通常是 :file:`nyc_roads/nyc_roads.shp`.
  
   .. figure:: new_shapefile.png

      基础数据存储信息（Basic Store Info）和连接参数（Connection Parameters）

#. 点击 :guilabel:`Save` 来保存更改。你将被重定向到 :guilabel:`New Layer` 页面来配置 ``nyc_roads`` 图层。 

创建图层
----------------

我们已经创建了 store ，现在我们可以发布图层了。

#. 在 :guilabel:`New Layer` 页面上，点击 ``nyc_roads`` 图层名旁的 :guilabel:`Publish`. 

   .. figure:: new_layer.png

      新图层

#. :guilabel:`Edit Layer` 页面定义了图层的数据和发布参数信息。为 ``nyc_roads`` 图层输入一个简短的标题（:guilabel:`Title`）和摘要（:guilabel:`Abstract`）。

   .. figure:: new_data.png

      基本资源信息

#. 依次点击 :guilabel:`Compute from data` > :guilabel:`Compute from native bounds` 来生成图层的限制框（bounding boxes）。

   .. figure:: boundingbox.png

      生成限制框

#. 点击页面顶部的 :guilabel:`Publishing` 选项卡。

#. 我们可以在这里设置图层样式。在 :guilabel:`WMS Settings` 下，确保默认样式（:guilabel:`Default Style`）被设置为了 :guilabel:`line`.

   .. figure:: style.png

      选择默认样式
  
#. 滚动到页面底部，点击 :guilabel:`Save` 来结束图层配置。

预览图层
--------------------

为了验证 ``nyc_roads`` 图层已被正确发布，我们可以预览它。

#. 导航到 :guilabel:`Layer Preview` 屏幕，找到 ``nyc:nyc_roads`` 图层。

   .. figure:: layer_preview.png

      图层预览

#. 点击 :guilabel:`Common Formats` 栏中的 :guilabel:`OpenLayers` 链接。

#. 一个 OpenLayers 地图将在新标签中加载，并显示以默认线条样式呈现的 shapefile. 你可以使用这个预览地图缩放查看这个数据集，也可以浏览要素的属性。

   .. figure:: openlayers.png

      nyc_roads 的预览地图
