.. _sec_roles:

Users and roles
===============

Security in GeoServer is a **role-based system**.  Roles are created to serve particular functions (Examples: access WFS, administer UI, read certain layers), and users are linked to those roles.

Setting users and roles
-----------------------

Linking users and roles is done via the file ``users.properties``.  This file is in the GeoServer data directory in the ``security`` directory.  Be default, this file contains one line::

   admin=geoserver,ROLE_ADMINISTRATOR

There is only one predefined role: ``ROLE_ADMINISTRATOR``.  This role provides full access to all systems inside GeoServer.  This file links the user **admin** (with password **geoserver**) to this role.

.. note::

   It should go without saying that if you are using GeoServer in a production environment, this default behavior should be immediately changed.

Other users and roles can be created by adding to the ``users.properties`` file.  The syntax is::

   user=password,role[,role2,...]

where:

   * **user** is the user name
   * **password** is the password associated with that user
   * **role[,role2,...]** is the name of the role(s) associated with this user   
   
Although the default administrator role is ``ROLE_ADMINISTRATOR``, the naming convention is not mandatory.    Multiple users can be linked with the same role.  Users and passwords are case-sensitive.