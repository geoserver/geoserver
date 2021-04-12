.. _postgis_quickstart:

发布 PostGIS 表
==========================

本教程涵盖了用 GeoServer 发布 PostGIS 表的步骤。

.. note:: 本教程假定 PostgreSQL/PostGIS 已经预先安装在系统上，并在 ``localhost`` 地址，端口号 ``5432`` 上提供服务并且 GeoServer 已在 ``http://localhost:8080/geoserver`` 运行。

数据准备
----------------

首先，我们收集将要发布的数据。

#. 下载 :download:`nyc_buildings.zip`. 它包含了一个纽约市建筑物的 PostGIS 数据表 dump.

#. 创建一个名叫 ``nyc`` 的 PostGIS 数据库。可以用以下命令实现:

   .. code-block:: console

      createdb nyc
      psql -d nyc -c 'CREATE EXTENSION postgis'

   .. note:: 要使用这些命令，你可能需要提供用户名和密码。

#. 解压 :file:`nyc_buildings.zip` 获取 :file:`nyc_buildings.sql`.

#. 导入 :file:`nyc_buildings.sql` 到 ``nyc`` 数据库:

   .. code-block:: console

      psql -f nyc_buildings.sql nyc

创建新 workspace
------------------------

下面的步骤用来为数据创建 workspace. 一个 workspace 是一种用于聚合相似图层的容器。

.. note:: 如果你希望使用一个已有的 workspace，可以跳过此步骤。通常，我们为每一个工程创建一个 workspace，它将包含相互之间有关联的 stores 和图层。

#. 在 Web 浏览器中打开 ``http://localhost:8080/geoserver``.

#. 像 :ref:`logging_in` 部分描述的那样登入 GeoServer. 

#. 导航到 :menuselection:`Data --> Workspaces`.

   .. figure:: ../../data/webadmin/img/data_workspaces.png

      Workspaces 页面

#. 点击 :guilabel:`Add new workspace` 按钮.

#. 你将会被要求输入 :guilabel:`Name` 和 :guilabel:`Namespace URI`.

   .. figure:: ../shapefile-quickstart/new_workspace.png

      配置新 workspace

#. 输入 ``nyc`` 作为 :guilabel:`Name` ， ``http://geoserver.org/nyc`` 作为:guilabel:`Namespace URI`.

   .. note:: 一个 workspace 名是用于描述项目的标识符。它必须不超过10个字符长，且不能含有空格。而一个 Namespace URI (Uniform Resource Identifier，统一资源标识符) 通常可以是一个和项目有关的、追加了一个尾部标识符来表明 workspace 的 URL.   Namespace URI 不必指向一个真实存在的 Web 地址。

#. 点击 :guilabel:`Submit` 按钮。 ``nyc`` workspace 将被添加到 :guilabel:`Workspaces` 列表。

创建一个 Store
----------------

创建完成 workspace 后，我们就准备好添加新 store 了。 Store 告诉 GeoServer 如何连接到数据源. 

#. 导航到 :menuselection:`Data-->Stores`.
    
#. 你应当能看到一个 store 的列表，它包含了 store 的类型和它隶属于的 workspace。

   .. figure:: datastores.png

      添加新数据源

#. 点击C ``PostGIS`` 链接来创建新数据源。

#. 输入 :guilabel:`Basic Store Info`:

   * 选择 ``nyc`` :guilabel:`Workspace`
   * 输入 ``nyc_buildings`` 作为数据源名称（:guilabel:`Data Source Name`）
   * 添加一段简短的描述（:guilabel:`Description`）

   .. figure:: basicStore.png

      Basic Store Info

#. 指定 PostGIS 数据库连接参数（:guilabel:`Connection Parameters`）:

   .. list-table::
      :header-rows: 1 

      * - Option
        - Value
      * - :guilabel:`dbtype`
        - :kbd:`postgis`
      * - :guilabel:`host`
        - :kbd:`localhost`
      * - :guilabel:`port`
        - :kbd:`5432`
      * - :guilabel:`database`
        - :kbd:`nyc`
      * - :guilabel:`schema`
        - :kbd:`public`
      * - :guilabel:`user`
        - :kbd:`postgres`
      * - :guilabel:`passwd`
        - (``postgres`` 用户的密码)
      * - :guilabel:`validate connections`
        - (勾选)

   .. note:: 其他字段保持默认值。
           
   .. figure:: connectionParameters.png
       
      连接参数

#. 点击 :guilabel:`Save` 来保存。 

创建图层
----------------

我们已经加载了 store ，现在我们可以发布图层了。

#. 导航到 :menuselection:`Data --> Layers`.

#. 点击 :guilabel:`Add a new resource`.

#. 从 :guilabel:`New Layer chooser` 菜单中选择 ``nyc:nyc_buidings``.

   .. figure:: newlayerchooser.png

      选择 store

#. 在图层结果行中选择名为 ``nyc_buildings`` 的图层。 

   .. figure:: layerrow.png

      选择新图层

#. :guilabel:`Edit Layer` 页面定义了图层的数据和发布参数信息。为 ``nyc_buildings`` 图层输入一个简短的标题（:guilabel:`Title`）和摘要（:guilabel:`Abstract`）。

   .. figure:: basicInfo.png

      基本资源信息

#. 依次点击 :guilabel:`Compute from data` > :guilabel:`Compute from native bounds` 来生成图层的限制框（bounding boxes）。

   .. figure:: boundingbox.png

      生成限制框

#. 点击页面顶部的 :guilabel:`Publishing` 选项卡。

#. 我们可以在这里设置图层样式。在 :guilabel:`WMS Settings` 下，确保默认样式（:guilabel:`Default Style`）被设置为了 :guilabel:`polygon`.

   .. figure:: style.png

      选择默认样式

#. 滚动到页面底部，点击 :guilabel:`Save` 来结束图层配置。

预览图层
--------------------

为了验证 ``nyc_buildings`` 图层已被正确发布，我们可以预览它。

#. 导航到 :guilabel:`Layer Preview` 屏幕，找到 ``nyc:nyc_buildings`` 图层。

#. 点击 :guilabel:`Common Formats` 栏中的 :guilabel:`OpenLayers` 链接。

#. 一个 OpenLayers 地图将在新标签中加载，并显示以默认多边形样式呈现的数据. 你可以使用这个预览地图缩放查看这个数据集，也可以浏览要素的属性。

   .. figure:: openlayers.png

      nyc_buildings 的预览地图

