.. _installation_war:

Web archive
===========

GeoServer打包为独立的servlet， 可与现有的应用程序服务器如 `Apache Tomcat <http://tomcat.apache.org/>`_ 和 `Jetty <http://eclipse.org/jetty/>`_ 一起使用。

.. note:: GeoServer大部分已经使用Tomcat进行了测试，推荐的应用程序服务器也是如此。GeoServer需要实现Servlet 3和注释处理的Tomcat的较新版本（7.0.65或更高版本）。已知其他应用程序服务器可以工作，但不能保证。
 
安装
------------

#. 确保在系统上安装了Java Runtime Environment（JRE）。GeoServer需要 **Java 8** 或者 **Java 11** 环境, 可从 `OpenJDK <http://openjdk.java.net>`__, `AdoptOpenJDK <https://adoptopenjdk.net>`__ 获取Windows和macOS的安装程序, 或由您的OS发行版提供。


   .. note:: 有关Java和GeoServer的更多信息，请参阅 `production_java`.

#. 导航到 `GeoServer 下载页面 <http://geoserver.org/download>`_.

#. 在下载页面上选择 :guilabel:`Web Archive` .

#. 下载存档文件并解压。

#. 照常部署Web存档。通常，所需要做的只是将 :file:`geoserver.war` 文件复制到应用程序服务器的 ``webapps`` 目录,然后部署该应用程序。

   .. note:: 可能需要重新启动应用程序服务器。

运行
-------

使用容器应用程序的启动和停止Web应用程序的方法来运行GeoServer。

要访问 `web_admin`, 打开浏览器并导航到 ``http://SERVER/geoserver`` 。例如，如果Tomcat在localhost的8080端口上运行，则URL为 ``http://localhost:8080/geoserver``。

卸载
--------------

#. 停止容器应用程序。

#. 从容器应用程序的 ``webapps`` 目录中删除GeoServer webapp 。这通常包括 :file: `geoserver.war` 文件和 ``geoserver`` 目录.
