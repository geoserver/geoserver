.. _installation:

安装
============
有多种方法可以在您的系统上安装GeoServer。本节将讨论可用的各种安装方式。
.. note::  要将GeoServer作为现有servlet容器（例如Tomcat）的一部分运行，请参阅 :ref:`installation_war` 部分。

.. warning:: GeoServer要求在您的系统上安装Java 8或Java 11（JRE）环境, 可以 `OpenJDK <http://openjdk.java.net>`__, `AdoptOpenJDK <https://adoptopenjdk.net>`__ 获取适用于Windows和macOS的安装包, 或从您的OS发行版中获得。
   
   这必须在安装之前完成。

.. toctree::
   :maxdepth: 1
   
   win_binary
   osx_binary
   linux
   war
   upgrade
.. note:: 目前，由于缺少可在其中构建和签名安装程序的安全Windows计算机，我们不再提供Windows Installer。