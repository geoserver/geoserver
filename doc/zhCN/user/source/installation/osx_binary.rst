.. _installation_osx_bin:

Mac OS X binary
=====================

.. note::  要在OS X上使用现有的应用程序服务器（如Tomcat）进行安装, 请参阅 :ref:`installation_war` 部分。

在OS X上安装GeoServer的另一种方法是使用平台无关的二进制文件。 这个版本是将一种便携式应用程序服务器 `Jetty <http://eclipse.org/jetty/>`__ 捆绑在内部的GeoServer Web应用程序。 它具有在所有操作系统上非常相似地工作的优点，并且设置非常简单。

安装
------------

#. 确保在系统上安装了Java Runtime Environment（JRE）。  GeoServer 要求 **Java 8** 或者 **Java 11** 环境, 由macOS安装程序 `AdoptOpenJDK <https://adoptopenjdk.net>`__ 提供。

   .. note:: 有关Java和GeoServer兼容性的更多信息，请参阅 production_java.

#. 导航到 `GeoServer下载页面 <http://geoserver.org/download>`_.

#. 选择您要下载的GeoServer版本。如果不确定，选择 `Stable <http://geoserver.org/release/stable>`_.

#. 在下载页面上选择 :guilabel:`Platform Independent Binary` 。

#. 下载文件并将其解压缩到您要将该程序放置在的目录中。

   .. note:: 建议的位置为 :file:`/usr/local/geoserver`.

#. 通过键入以下命令来添加环境变量以保存GeoServer的位置：

   .. code-block:: bash
    
      echo "export GEOSERVER_HOME=/usr/local/geoserver" >> ~/.profile
      . ~/.profile

#. 输入如下命令,使自己成为 ``geoserver`` 文件夹的拥有者:

    .. code-block:: bash

       sudo chown -R <USERNAME> /usr/local/geoserver/

   ``USER_NAME`` 是你的用户名

#. 切换到目录 ``geoserver/bin`` 并执行 ``startup.sh`` 脚本启动GeoServer:

    .. code-block:: bash
       
       cd geoserver/bin
       sh startup.sh

    .. include:: ./osx_jaierror.txt

#. 在 web 浏览器, 导航到 ``http://localhost:8080/geoserver``.

如果看到GeoServer徽标，则表明已成功安装GeoServer。

   .. figure:: images/success.png

      GeoServer已安装并成功运行

要关闭GeoServer，请永久关闭命令行窗口，或者运行文件夹 :file:`bin` 下的 :file:`shutdown.bat` 文件。

卸载
--------------

#. 停止GeoServer（如果正在运行）。

#. 删除安装GeoServer的目录。
