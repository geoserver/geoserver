.. _sec_rolesystem_usergroups:

Users and Groups
================

A GeoServer **user** is defined similarly to most security systems.  The correct Java term is **principal**. A principal may be a human being, a software system, a computer and so on.  In this introduction, the term **user** is used. For each user the following information is maintained:

* User name
* :ref:`Password <sec_passwd>` (optionally stored :ref:`encrypted <sec_passwd_encryption>`)
* A flag indicating if the user is enabled (this is the default). A disabled user is not allowed to log in in the future. Existing log ins are not affected.
* Set of key/value pairs

Key/value pairs are implementation-specific and may be set by the :ref:`user/group service <sec_rolesystem_usergroupservices>` that the user or group 
originates from. For example, a user/group service that maintains information about a user such as Name, Email address, etc., may wish to associate those data values with the user object.

A GeoServer **group** is simply a set of users. For each group the following information is maintained:

* Group name
* A flag indicating if the group is enabled (this is the default). A disabled group does not contribute to the role calculation for all users contained in this group.
* List of users that are members of the group


