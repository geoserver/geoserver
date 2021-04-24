.. _web_admin:

网络管理界面
============================

网络管理界面时基于web的用来管理geoserver的工具, 从添加数据到更改服务设置。 在默认的GeoServer安装中， 可以通过Web浏览器访问该界面 ``http://localhost:8080/geoserver/web``。 但是，URL可能会有所不同，具体取决于您的本地安装。

.. figure:: images/web-admin.png

   网络管理界面

   以下各节详细介绍了GeoServer中可用的菜单选项。 **除非另有说明，否则您将需要使用管理凭据登录才能查看这些选项。**

关于 & 状态
--------------

The :guilabel:`About & Status` 部分提供对GeoServer诊断和配置工具的访问权限，对于调试特别有用。

* The ref:`config_serverstatus` 页面显示服务器配置参数和运行时状态的摘要。

* The ref:`GeoServer Logs <logging>` 页面显示的GeoServer日志输出。这对于无需退出浏览器即可确定错误很有用。

* The ref:`config_contact` 页面设置WMS服务器的“功能”文档中可用的公共联系信息。

* The :guilabel:`About GeoServer` 部分提供了指向GeoServer文档，主页和错误跟踪器的链接。 **您无需登录GeoServer即可访问此页面。**


数据
----

 ref:`Data` 部分包含所有不同数据相关的设置配置选项。

* ref:`Layer Preview <layerpreview>` 页面提供了各种输出格式的链接，包括常见的OpenLayers和KML格式。该页面有助于直观地验证和探索特定层的配置。 **您无需登录到GeoServer即可访问图层预览。**

* ref:`Workspaces <data_webadmin_workspaces>` 页面显示一个工作区列表，并具有添加，编辑和删除的功能。还显示哪个工作空间是服务器的默认工作空间。

* ref:`Stores <data_webadmin_stores>` 页面显示存储列表，并具有添加，编辑和删除的功能。详细信息包括与图层关联的工作空间，存储的类型（数据格式）以及是否启用了图层。

* ref:`Layers <data_webadmin_layers>` 页面显示图层列表，并具有添加，编辑和删除的功能。详细信息包括与该图层关联的工作空间和存储，是否启用了该图层以及该图层的空间参考系统（SRS）。

* ref:`Layer Groups <data_webadmin_layergroups>` 页面显示一个图层组列表，并具有添加，编辑和删除的功能。详细信息包括关联的工作空间（如果有）。

* ref:`Styles <styling_webadmin>` 页面显示样式列表，并具有添加，编辑和删除的功能。详细信息包括关联的工作空间（如果有）。

在每个包含表的页面中，都有三种不同的方法来定位对象：排序，搜索和分页。要按字母顺序对数据类型进行排序，请单击列标题。为了进行简单搜索，请在搜索框中输入搜索条件，然后按Enter。要浏览条目（一页25个），请使用表格底部和顶部的箭头按钮。

服务
--------

 ref:`services` 部分是用于配置的GeoServer发布的各种服务。

* ref:`Web Coverage Service (WCS) <services_webadmin_wcs>` 页面管理元数据，资源限制和SRS可用性WCS。

* ref:`Web Feature Service (WFS) <services_webadmin_wfs>` 页面管理元数据，功能发布，服务水平选择，以及数据专用输出WFS。

* ref:`Web Map Service (WMS) <services_webadmin_wms>` 页面管理元数据，资源限制，SRS可用性和其他数据特定输出WMS。

设置
--------

 :guilabel:`Settings` 分包含适用于整个服务器的配置设置。


* ref:`Global Settings <config_globalsettings>` 页面为整个服务器配置消息传递，日志记录，字符和代理设置。

* ref:`JAI` 页面配置了WMS和WCS操作都使用的几个JAI参数。

* ref:`Coverage Access <config_converageaccess>` 页面配置相关的加载和发布覆盖范围设置。

切片缓存
------------

 :guilabel:`Tile Caching` 部分配置嵌入式 :ref:`GeoWebCache <gwc>`.

* ref:`Tile Layers <gwc_webadmin_layers>` 页面显示了GeoServer中的哪些图层也可以作为平铺（缓存）图层使用，并具有添加，编辑和删除的功能。

* ref:`Caching Defaults <gwc_webadmin_defaults>` 页面设置缓存服务的全局选项。

* ref:`Gridsets <gwc_webadmin_gridsets>` 页面显示了切片缓存的所有可用网格集，并具有添加，编辑和删除的功能。

* ref:`Disk Quota <gwc_webadmin_diskquotas>` 面设置磁盘上切片缓存管理的选项，包括在必要时减小文件大小的策略。

* ref:`BlobStores <gwc_webadmin_blobstores>` 页面管理已知嵌入式GeoWebCache不同blobstores（平铺缓存源）。

安全
--------

ref:`Security <security_webadmin>` 部分配置内置 :ref:`security subsystem <security>`.

*  ref:`Settings <security_webadmin_settings>` 页面管理的安全子系统的高级选项。

*  ref:`Authentication <security_webadmin_auth>` 页面管理认证过滤器，过滤器链和供应商。

*  ref:`Passwords <security_webadmin_passwd>` 页面管理用户和root帐户的密码策略。

*  ref:`Users, Groups, Roles <security_webadmin_ugr>` 页面管理用户，组和角色，以及它们如何相互关联。用户帐户的密码可以在此处更改。

*  ref:`Data <security_webadmin_data>` 页面管理数据级安全性选项，从而允许工作空间和图层受角色限制。

*  ref:`Services <security_webadmin_services>` 页面管理服务级别安全选项，让服务和运营由角色的限制。

演示
-----

 ref:`demos` 部分包含指向GeoServer的示例WMS，WCS和WFS请求的链接，并列出了GeoServer已知的所有SRS信息。此外，还有一个用于在空间参考系统之间转换坐标的重新投影控制台，以及一个用于WCS请求的请求构建器。 **您无需登录GeoServer即可访问这些页面。**

工具
------

 ref:`tools` 部分包含管理工具。

*  ref:`Web Resource <tool_resource>` 工具提供的数据目录图标，字体和配置文件管理。
*  ref:`Catalog Bulk Load Tool <tool_bulk>` 的测试可以批量复制配置。

扩展
----------

ref:`GeoServer extensions <extensions>` 可以向Web界面添加功能和其他选项。可以在每个扩展名的部分中找到详细信息。
