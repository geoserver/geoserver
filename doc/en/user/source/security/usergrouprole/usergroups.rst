.. _sec_rolesystem_usergroups:

Users and Groups
================

A GeoServer **user** is defined similarly to most security systems.  For each user the following information is maintained:

* User name
* :ref:`Password <sec_passwd>` (optionally stored :ref:`encrypted <sec_passwd_encryption>`)
* Set of key/value pairs

A GeoServer **group** is simply a set of users. For each group the following information is maintained:

* Group name
* List of users that are members of the group
* Set of key/value pairs

Key/value pairs are implementation-specific and may be set by the :ref:`user/group service <sec_rolesystem_usergroupservices>` that the user or group 
originates from. For example, a user/group service that maintains information about a user such as Name, Email address, etc., may wish to associate those data values with the user object.
