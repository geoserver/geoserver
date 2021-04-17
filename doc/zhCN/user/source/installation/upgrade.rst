.. _installation_upgrade:

升级现有版本
===========================

.. warning:: 请注意，某些升级是不可逆的，这意味着可以更改数据目录，使其不再与较旧版本的GeoServer兼容。 有关更多详细信息，请参阅 `datadir_migrating`。

通常GeoServer升级过程如下：

#. 备份当前数据目录。这可能涉及简单地将目录复制到其他位置。

#. 确保当前数据目录在应用程序外部（不在应用程序文件结构内部）。

#. 卸载旧版本，然后安装新版本。

   .. note:: 或者，您可以直接在旧版本之上安装新版本。

#. 确保新版本继续指向先前版本使用的相同数据目录。

升级特定版本的注意事项
------------------------------------

GeoJSON编码（GeoServer 2.6及更高版本）
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

从GeoServer 2.6开始，由WFS服务生成的GeoJSON不再对CRS使用非标准编码。要出于兼容性目的重新启用此行为，请将其设置GEOSERVER_GEOJSON_LEGACY_CRS=true为系统属性，上下文参数或环境变量。

JTS类型绑定（GeoServer 2.14及更高版本）
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

从GeoServer 2.14开始，由于升级到JTS（Java拓扑套件）1.16.0 ，由 `REST <rest>`要素类型和结构化coverage生成的输出请求使用不同的软件包名称 (``org.locationtech`` instead of ``com.vividsolutions``) 进行几何类型绑定。 例如：

以前::

    ...
    <attribute>
      <name>geom</name>
      <minOccurs>0</minOccurs>
      <maxOccurs>1</maxOccurs>
      <nillable>true</nillable>
      <binding>com.vividsolutions.jts.geom.Point</binding>
    </attribute>
    ...

之后::

    ...
    <attribute>
      <name>geom</name>
      <minOccurs>0</minOccurs>
      <maxOccurs>1</maxOccurs>
      <nillable>true</nillable>
      <binding>org.locationtech.jts.geom.Point</binding>
    </attribute>
    ...


任何依赖此绑定信息的REST客户端都应更新以支持新名称。
