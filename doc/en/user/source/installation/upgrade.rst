.. _installation_upgrade:

Upgrading
=========

The general GeoServer upgrade process involves installing the new version on top
of the old and ensuring it points at the same data directory used by the
previous version. See :ref:`migrating_data_directory` for more details.

This section contains details about upgrading to specific GeoServer versions.

Upgrade to 2.2
--------------

Security configuration
^^^^^^^^^^^^^^^^^^^^^^

GeoServer 2.2 comes with a significant retrofit of the :ref:`security` 
subsystem. The changes focus mostly on authentication and user management. On 
upgrade GeoServer will update configuration in the ``security`` directory. The 
specific changes are described :ref:`here <migrating_data_directory_22x>`.

RESTconfig security and administrative access
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The security changes also include a new type of access mode for layer level 
security that allows for controlling administrative access to workspaces. In 
this context administrative access includes access via the web admin ui, or 
the RESTconfig api. For more details see :ref:`sec_layer`.

A side effect of this change can have consequences for RESTconfig api users. Previously access via REST was controlled by specifying constraints on url patterns as described :ref:`here <sec_rest>`. Administrative
workspace/layer security now adds a second level of access control. Therefore in order for a user to access resources via REST that user must also have sufficient administrative privileges.

By default administrative access for workspaces/layers is granted to the ``ROLE_ADMINISTRATOR`` role. So if REST security defines url level constraints that involve roles with lesser privileges, access to resources will be denied. The most common case of this is when users make the REST api accessible anonymously. 

The solution to this problem is to reduce the administrative access role to that required by REST url security. In the case where access to the REST api is granted anonymously this is **not recommended**. Allowing a server to be administered anonymously is a huge security hole. 



