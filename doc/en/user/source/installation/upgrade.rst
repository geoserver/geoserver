.. _installation_upgrade:

Upgrading
=========

The general GeoServer upgrade process involves installing the new version on top
of the old and ensuring it points at the same data directory used by the
previous version. See :ref:`migrating_data_directory` for more details.

This section contains details about upgrading to specific GeoServer versions.

Upgrade to 2.2+
---------------

Security configuration
^^^^^^^^^^^^^^^^^^^^^^

GeoServer 2.2 comes with a significant retrofit of the :ref:`security` 
subsystem. The changes focus mostly on authentication and user management. On 
upgrade GeoServer will update configuration in the ``security`` directory. The 
specific changes are described :ref:`here <migrating_data_directory_22x>`.

Obtaining a master password
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Starting with Geoserver 2.2 a master password is needed. This password is used to log in as ``root`` user and to protect the Geoserver key store.

During the upgrade process, Geoserver tries to find a proper master password. The following rules apply

  * The default admin password ``geoserver`` is not allowed.
  * The minimal length of the password is 8 characters.

The algorithm for finding  a password:

#. Look for an existing user called ``admin``. If there is such a user and the password obeys the rules above, use it.

#. Look for a user having the role ``ROLE_ADMINISTRATOR``. If there is such a user and the password obeys the rules above, use it.

#. Generate a random password with 8 characters of length

The algorithm stores a file ``masterpw.info`` into the ``security`` directory. If an existing password of a user is used, the content of this file is like

::

	This file was created at 2012/08/11 15:57:52

	Master password is identical to the password of user: admin
	
	Test the master password by logging in as user "root"

	This file should be removed after reading !!!.


If a master password was generated, the content is like

::


	This file was created at 2012/08/11 15:57:52

	The generated master password is: pw?"9bWL

	Test the master password by logging in as user "root"

	This file should be removed after reading !!!.

After reading this file, remember the master password and remove this file. 


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



Upgrade to 2.6+
---------------

Before 2.6, the GeoJSON produced by the WFS service used a non-standard encoding for the CRS.  Setting ``GEOSERVER_GEOJSON_LEGACY_CRS=true`` as a system property, context parameter, or environment variable will enable the old behaviour.
