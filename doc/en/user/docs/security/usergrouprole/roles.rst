.. _security_rolesystem_roles:

Roles
=====

GeoServer **roles** are keys associated with performing certain tasks or accessing particular resources. Roles are assigned to users and groups, authorizing them to perform the actions associated with the role. A GeoServer role includes the following:

* Role name
* Parent role
* Set of key/value pairs

GeoServer roles support inheritance—a child role inherits all the access granted to the parent role. For example, suppose you have one role named ``ROLE_SECRET`` and another role, ``ROLE_VERY_SECRET``, that extends ``ROLE_SECRET``. ``ROLE_VERY_SECRET`` can access everything ``ROLE_SECRET`` can access, but not vice versa.

Key/value pairs are implementation-specific and may be configured by the :ref:`role service <security_rolesystem_roleservices>` the user or group belongs to. For example, a role service that assigns roles based on employee organization may wish to associate additional information with the role such as Department Name.

GeoServer has a number of system roles, the names of which are reserved. Adding a new GeoServer role with reserved name is not permitted.

* ``ROLE_ADMINISTRATOR``—Provides access to all operations and resources
* ``ROLE_GROUP_ADMIN``—Special role for administrating user groups
* ``ROLE_AUTHENTICATED``—Assigned to every user authenticating successfully
* ``ROLE_ANONYMOUS``—Assigned if anonymous authentication is enabled and user does not log on


