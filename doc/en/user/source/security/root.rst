.. _sec_root:

Root account
============

The highly configurable nature of GeoServer security allows for the possibility that an administrator may inadvertently cause normal authentication to cease functioning, essentially disabling all users including administrative accounts.  For this reason, the GeoServer security subsystem contains a **root account** that is always active regardless of the state of the security configuration. Much like its UNIX-style counterpart, this account provides "super user" status, and is meant to provide an alternative access method for fixing misconfiguration.

The username for the root account is ``root``.  Its name cannot be changed.  The password for the root account is the :ref:`sec_master_passwd`.
