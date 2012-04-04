.. _sec_rolesystem_roles:

Roles
=====

GeoServer **roles** are keys that are associated with the performing certain tasks or accessing particular resources.  Roles are assigned to users and groups in order to authorize them to perform the actions associated with the role. A role in GeoServer is made up of the following information:

* Role name
* Parent role
* Set of key/value pairs

GeoServer roles support inheritance; a child role inherits all the access granted to the parent role. For example, consider a role named ``ROLE_SECRET`` and a role that extends from it named ``ROLE_VERY_SECRET``. In this case, ``ROLE_VERY_SECRET`` encompasses all of what ``ROLE_SECRET`` can access, but not vice versa.

Key/value pairs are implementation-specific and may be set by the :ref:`role service <sec_rolesystem_roleservices>` that the user or group 
originates from. For example, a role service that assigns roles based on employee organization may wish to associate additional information with the role such as Department.

The default role in GeoServer is ``ROLE_ADMINISTRATOR``, which gives access to all operations and resources.

.. warning:: JUSTIN-TODO: LIST OTHER PSEUDOROLES?