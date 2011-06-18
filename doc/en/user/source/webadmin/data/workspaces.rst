.. _webadmin_workspaces:

Workspaces
==========

This section is for viewing and configuring workspaces.  Analogous to a namespace, a workspace is a container which is used to organize other items.  In GeoServer, a workspace is often used to group similar layers together. Individual layers are often referred to by their workspace name, colon, then store. (Ex: topp:states) Two different layers having the same name can exist as long as they exist in different workspaces. (Ex: sf:states, topp:states).

.. figure:: ../images/data_workspaces.png
   :align: center
   
   *Workspaces page*

Edit Workspace
--------------

In order to view details and edit a workspace, click on a workspace name.

.. figure:: ../images/data_workspaces_URI.png
   :align: center
   
   *Workspace named "topp"*
   
A workspace consists of a name and a Namespace URI (Uniform Resource Identifier).  The workspace name has a maximum of ten characters and may not contain space.  A URI is similar to URLs, except URIs need not point to a location on the web, and only need to be a unique identifier.  For a Workspace URI, we recommend using a URL associated with your project, with perhaps a different trailing identifier, such as ``http://www.openplans.org/topp`` for the "topp" workspace.  
   
Add or Remove a Workspace
-------------------------

The buttons for adding and removing a workspace can be found at the top of the Workspaces view page. 

.. figure:: ../images/data_workspaces_add_remove.png
   :align: center
   
   *Buttons to add and remove*
   
To add a workspace, select the :guilabel:`Add new workspace` button.  You will be prompted to enter the the workspace name and URI.   
   
.. figure:: ../images/data_workspaces_medford.png
   :align: center
   
   *New Workspace page with example*
 
In order to remove a workspace, click on the workspace's corresponding check box.  As with the layer deletion process, multiple workspaces can be checked for removal on a single results page.  Click the :guilabel:`Remove selected workspaces(s)` button.  You will be asked to confirm or cancel the deletion.  Clicking :guilabel:`OK` will remove the workspace. 

.. figure:: ../images/data_workspaces_rename_confirm.png
   :align: center
   
   *Workspace removal confirmation*
      