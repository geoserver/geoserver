.. _tool_resource_examples:

Resource Browser Examples
=========================

Uploading an icon to a styles folder
------------------------------------

To upload a file to the global styles folder:

#. Use :guilabel:`Resource Browser` to select :menuselection:`/ --> styles` in the resource tree.
   
   .. figure:: img/upload_select_styles.png
      
      Resource Browser styles folder
      
#. Click :guilabel:`Upload` button to open :guilabel:`Upload a Resource` dialog.

#. Use :guilabel:`Browse` to select a file from the local file system.
   
   .. figure:: img/upload_icon.png
      
      Upload icon to styles folder

#. Press :guilabel:`OK` to upload the file.

   .. figure:: img/upload_icon_complete.png
      
      Resource Browser icon

Creating a control-flow configuration file
------------------------------------------

Many extensions, such as :ref:`control_flow`, are managed using a configuration file.

To create a :file:`controlflow.properties` file:

#. Use :guilabel:`Resource Browser` to select the root :menuselection:`/` folder in the resource tree.
   
   This can be tricky as the label is not very long.
   
   .. figure:: img/control_root.png
      
      Resource Browser root folder
      
#. Click :guilabel:`New resource` button to open :guilabel:`Edit a Resource` dialog.
   
   * :guilabel:`Resource`: :kbd:`controlflow.properties`
   * :guilabel:`Content`: file contents
   
      .. literalinclude:: /extensions/controlflow/controlflow.properties
         :language: properties
   
#. Press :guilabel:`OK` to create the resource.
   
   .. figure:: img/control_edit.png
      
      Edit a Resource controlflow.properties
