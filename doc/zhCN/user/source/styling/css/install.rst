.. _css_install:

安装 GeoServer CSS 扩展
======================================

CSS扩展在GeoServer下载页面的其他扩展下载中列出。

安装过程与其他GeoServer扩展类似:

#. 下载 :download_extension:`css`
   
   验证文件名中的版本号是否匹配你正在运行的 GeoServer 版本(例如上面的 |release| )。

#. 归档文件的内容提取到 GeoServer的WEB-INF/lib :file:`WEB-INF/lib` 目录中。
   确保在提取过程中没有创建任何子目录。

#. 重启 GeoServer。

如果安装成功，您将在 :ref:`styling_webadmin` 编辑器中看到一个新的CSS条目。 

.. figure:: images/css_style_format.png

   CSS format in the new style page

安装之后，您可能希望阅读本教程: :ref:`Styling data with CSS <css_tutorial>`。
