.. _security_root:

Root account
============

The highly configurable nature of GeoServer security may result in an administrator inadvertently disrupting normal authentication, essentially disabling all users including administrative accounts.  For this reason, the GeoServer security subsystem contains a **root account**. Much like its UNIX-style counterpart, this account provides "super user" status, and is meant to provide an alternative access method for fixing configuration issues.

The username for the root account is ``root``.  Its name cannot be changed and the password for the root account is the :ref:`keystore password providers <security_webadmin_passwd_keystore>`.

Logging in as ``root`` is disabled by default:

* Enable using the web admin console following the instructions in :ref:`security_webadmin_passwd_keystore`.
  
  You may also enable manually by changing the Keystore Password Provider :file:`config.xml`, usually located in :file:`security/masterpw/default/config.xml`, by adding the following statement:
   
  .. code-block:: xml
  
     <loginEnabled>true</loginEnabled>

* Enable using application property ``GEOSERVER_ROOT_LOGIN_ENABLED``. With value of ``true``
  to enable, or value of ``false`` to disable, authentication of the ``root`` user. 
