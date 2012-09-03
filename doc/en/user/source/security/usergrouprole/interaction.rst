.. _sec_rolesystem_interaction:

Interaction between user/group and role services
================================================

The following information provides some details on the interaction between the :ref:`sec_rolesystem_usergroupservices` and the :ref:`sec_rolesystem_roleservices`.

Calculating the roles of a user
-------------------------------

The following diagram illustrates how a user/group service and a role service interact in order to calculate the roles of a user

.. figure:: images/usergrouprole1.png
   :align: center

   *User/group and role service interacting for role calculation*

On fetching an enabled user from a user/group service, his roles have to be calculated. The detailed procedure is described below.

#. Fetch all enabled groups of the user. If a group is disabled, it is not of interest.
#. Fetch all roles associated to the user and put them into the result set
#. For each enabled group the user is a member, fetch all roles associated with the group and add them to the result set.
#. For each role in the result set, fetch all ancestor roles and add them to the result set.
#. Personalize each role in the result set if necessary
#. If the result set contains the local admin role, add the role ``ROLE_ADMINISTRATOR``
#. If the result set contains the local group admin role, add the role ``ROLE_GROUP_ADMIN``

Personalization of role looks for role parameters (key value pairs) of each role and checks if the user properties (key value pairs) contain and identical key. If this is the case, the value of the role parameter is replaced by the value of the user property.


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

A single user/group service that persists the user database as XML is present. The database contains a single user named ``admin`` and no groups. Similarly, the role server persists the role database as XML. By default this contains a single role named ``ADMIN``, with the ``admin`` user associated with it. The ``ADMIN`` role is mapped to ``ROLE_ADMINISTRATOR`` role and thus, the ``admin`` user is associated with system administrator role during role calculation.
