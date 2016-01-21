.. _security_root:

Root account
============

The highly configurable nature of GeoServer security may result in an administrator inadvertently disrupting normal authentication, essentially disabling all users including administrative accounts.  For this reason, the GeoServer security subsystem contains a **root account** that is always active, regardless of the state of the security configuration. Much like its UNIX-style counterpart, this account provides "super user" status, and is meant to provide an alternative access method for fixing configuration issues.

The user name for the root account is ``root``.  Its name cannot be changed and the password for the root account is the :ref:`security_master_passwd`.
