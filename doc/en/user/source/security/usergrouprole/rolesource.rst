.. _security_rolesystem_rolesource:

Role source and role calculation
================================

Different authentication mechanisms provide different possibilities where to look for the roles of a principal/user. The  role source is the base for the calculation of the roles assigned to the authenticated principal.

Using a user/group Service
--------------------------

During configuration of an authentication mechanism, the name of a user group service has to be specified. The used role service is always the role service configured as active role service. The role calculation itself is described here :ref:`security_rolesystem_interaction`

Using a role service directly
-----------------------------

During configuration of an authentication mechanism, the name of a role service has to be specified. The calculation of the roles works as follows:

#. Fetch all roles for the user.
#. For each role in the result set, fetch all ancestor roles and add those roles to the result set.
#. If the result set contains the local admin role, add the role ``ROLE_ADMINISTRATOR``.
#. If the result set contains the local group admin role, add the role ``ROLE_GROUP_ADMIN``.

This algorithm does not offer the possibility to have personalized roles and it does not consider group memberships.

Using an HTTP header attribute
------------------------------

The roles for a principal are sent by the client in an HTTP header attribute (Proxy authentication). GeoServer itself does no role calculation and extracts the roles from the header attribute. During configuration, the name of the header attribute must be specified. An example with a header attribute named "roles"::

 	roles: role_a;role_b;role_c

An example for roles with role parameters::

	roles: role_a;role_b(pnr=123,nick=max);role_c

The default syntax is

* roles are delimited by **;**
* a role parameter list starts with **(** and ends with **)** 
* a role parameter is a key value pair delimited by **=**
* role parameters are delimited by **,**
