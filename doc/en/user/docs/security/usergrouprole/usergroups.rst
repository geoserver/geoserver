.. _security_rolesystem_usergroups:

Users and Groups
================

The definition of a GeoServer **user** is similar to most security systems. Although the correct Java term is **principal**—a principal being a human being, computer, software system, and so on—the term **user** is adopted throughout the GeoServer documentation. For each user the following information is maintained:

* User name
* :ref:`Password <security_passwd>` (optionally stored :ref:`encrypted <security_passwd_encryption>`)
* A flag indicating if the user is enabled (this is the default). A disabled user is prevented from logging on. Existing user sessions are not affected.
* Set of key/value pairs

Key/value pairs are implementation-specific and may be configured by the :ref:`user/group service <security_rolesystem_usergroupservices>` the user or group belongs to. For example, a user/group service that maintains information about a user such as Name, Email address, and so on, may wish to associate those attributes with the user object.

A GeoServer **group** is simply a set of users. For each group the following information is maintained:

* Group name
* A flag indicating if the group is enabled (this is the default). A disabled group does not contribute to the role calculation for all users contained in this group.
* List of users who belong to the group


