.. _installation_windows_bin:

Windows binary
==============

.. note:: 要在具有现有应用程序服务器（如Tomcat）的Windows上进行安装，请参阅 :ref:`installation_war` 部分。

在Windows上安装GeoServer的另一种方法是使用平台无关的二进制文件。这个版本是将一种便携式应用程序服务器 `Jetty <http://eclipse.org/jetty/>`__ 捆绑在内部的GeoServer Web应用程序。 它具有在所有操作系统上非常相似地工作的优点，并且设置非常简单。

安装
------------

#. 确保在系统上安装了Java Runtime Environment（JRE）。 GeoServer 要求 **Java 8** 或者 **Java 11** 环境, 由Windows 安装程序`AdoptOpenJDK <https://adoptopenjdk.net>`__ 提供.


   .. note:: 有关Java和GeoServer的更多信息， 请参阅章节 。

#. 导航到 `GeoServer下载页面 <http://geoserver.org/download>`_.

#. 选择您要下载的GeoServer版本。如果不确定，选择 `Stable <http://geoserver.org/release/stable>`_.  

#. 在下载页面上选择 :guilabel:`Platform Independent Binary` 。

#. 下载文件并将其解压缩到您要将该程序放置在的目录中。

   .. note:: 建议的位置为 :file:`C:\\Program Files\\GeoServer`.

设置环境变量
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

环境变量``JAVA_HOME``如果尚未设置,则需要对其进行设置。这是通往您的JRE的路径 :file:`%JAVA_HOME%\\bin\\java.exe` .

#. 导航至 :menuselection:`控制面板 --> 系统 --> 高级系统设置 --> 环境变量`.

#. 在 :guilabel:`系统变量` 下点击 :guilabel:`新建`. 

#. 在 :guilabel:`变量名` 输入 ``JAVA_HOME``.  在 :guilabel:`变量值` 输入你的 JDK/JRE路径。

#.单击确定三次。

.. note:: 您可能还需要设置 ``GEOSERVER_HOME`` 变量,即GeoServer的安装目录, 和 ``GEOSERVER_DATA_DIR`` 变量, 即GeoServer数据目录的位置 (默认为 :file:`%GEOSERVER_HOME\\data_dir`). 如果您希望使用默认位置以外的数据目录，则后者是必需的。 设置这些变量的过程与设置 ``JAVA_HOME`` 变量相同。

运行
-------

.. note:: 可以通过Windows资源管理器或命令行来完成。

#. 导航到安装GeoServer的位置内的目录 :file:`bin`。

#. 运行 :file:`startup.bat`。 命令行窗口将出现并保持不变。此窗口包含诊断和故障排除信息。该窗口必须保持打开状态，否则GeoServer将关闭。

#. 导航至 ``http://localhost:8080/geoserver`` (或在任何位置安装GeoServer) 访问GeoServer  。

如果看到GeoServer徽标，则表明已成功安装GeoServer。

   .. figure:: images/success.png

    GeoServer已安装并成功运行

停止
--------

要关闭GeoServer，请永久关闭命令行窗口，或运行 :file:`bin` 目录中的 :file:`shutdown.bat` 文件。

卸载
--------------

#. 停止GeoServer（如果正在运行）。

#. 删除安装GeoServer的目录。
