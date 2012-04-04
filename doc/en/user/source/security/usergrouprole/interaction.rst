.. _sec_rolesystem_interaction:

Interaction between user/group and role services
================================================

The following information provides some details on the interaction between the :ref:`sec_rolesystem_usergroupservices` and the :ref:`sec_rolesystem_roleservices`.

Providing user information
--------------------------

The following diagram illustrates a user/group service and a role service interacts in order to provide user information. 

.. figure:: images/usergrouprole1.png
   :align: center

   *User/group and role service interacting to provide user information*

The user/group service provides the lookup interface for user information. Part of loading information for a user involves
delegating to the role service to determine what roles/authorities the specific user has associated with them. 

Authentication of user credentials
----------------------------------

A user/group service is primarily used during authentication. An authentication provider in the :ref:`sec_auth_chain` may utilize a user/group service
to perform authentication of user credentials. 

.. figure:: images/usergrouprole2.png
   :align: center

   *Using a a user/group service for authentication*

GeoServer defaults
------------------

The following figure illustrates the default user/group service, role service, and authentication provider in GeoServer:

.. figure:: images/usergrouprole3.png
   :align: center

   *Default GeoServer security configuration*

Two authentication providers are configured. The *Root* provider authenticates for the GeoServer :ref:`sec_root` and does not use a user/group service. The *Username/password* provider is the default provider that simply passes off username and password credentials to a user/group service.

A single user/group service that persists the user database as XML is present. The database contains a single user named ``admin`` and no groups. Similarly, the role server persists the role database as XML. By default this contains a single role named ``ROLE_ADMINISTRATOR``, with the ``admin`` user associated with it.
