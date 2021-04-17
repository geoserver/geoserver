.. _installation_linux:

Linux binary
============

.. note:: 要在具有现有应用程序服务器（如Tomcat）的Linux上进行安装，请参阅 :ref:`installation_war` 部分.

与平台无关的二进制文件是捆绑 `Jetty <http://eclipse.org/jetty/>`_ （一种轻便的便携式应用程序服务器）的GeoServer Web应用程序。 它具有在所有操作系统上非常相似地工作的优点，并且设置非常简单。

Installation
------------

#. 确保在系统上安装了Java Runtime Environment（JRE）。GeoServer需要 **Java 8** 或者 **Java 11** 环境, 可从 `OpenJDK <http://openjdk.java.net>`__, `AdoptOpenJDK <https://adoptopenjdk.net>`__ 获得, 或由您的OS发行版提供.

   .. note:: 有关Java和GeoServer兼容性的更多信息，请参阅 `production_java`.

#. 选择您要下载的GeoServer版本。如果不确定，请选择 `Stable <http://geoserver.org/release/stable>`_ 。

#. 在下载页面上选择 :guilabel:`Platform Independent Binary` 。

#. 下载文件并将其解压缩到您要将该程序放置在的目录中。

   .. note:: 建议的位置为 :file:`/usr/share/geoserver`。

#. 过键入以下命令来添加环境变量以保存GeoServer的位置：

    .. code-block:: bash
    
       echo "export GEOSERVER_HOME=/usr/share/geoserver" >> ~/.profile
       . ~/.profile

#. 为使自己成为 ``geoserver`` 文件夹的拥有者。 在终端窗口中输入以下命令，用你的名字替换 ``USER_NAME`` :

    .. code-block:: bash

       sudo chown -R USER_NAME /usr/share/geoserver/

#. 切换到目录到 ``geoserver/bin`` 文件夹并且执行脚本 ``startup.sh`` 启动GeoServer:

    .. code-block:: bash
       
       cd geoserver/bin
       sh startup.sh

#. 在 web 浏览器, 导航到 ``http://localhost:8080/geoserver``.

如果看到GeoServer徽标，则表明已成功安装GeoServer。

   .. figure:: images/success.png

      GeoServer已安装并成功运行

要关闭GeoServer，请关闭永久命令行窗口， 或运行目录 :file:`bin` 中的 :file:`shutdown.sh` 文件.

卸载
--------------

#. 停止GeoServer（如果正在运行）。

#. 删除安装GeoServer的目录。
