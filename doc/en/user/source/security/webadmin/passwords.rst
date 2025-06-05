.. _security_webadmin_passwd:

Passwords
=========

This page configures the various options related to :ref:`security_passwd`, the :ref:`security_master_passwd`, and :ref:`security_passwd_policy`.

.. note:: User passwords may be changed in the Users dialog box accessed from the :ref:`security_webadmin_ugr` page.

.. _security_webadmin_masterpasswordprovider:

Keystore passwords
------------------

In GeoServer, encrypting and decrypting passwords involves the generation of secret shared keys, stored in a Java *keystore*. For more information see :ref:`security_passwd_keystore`.

Active keystore password provider
'''''''''''''''''''''''''''''''''

This option sets the active keystore password provider, via a list of all available keystore password providers.

.. figure:: images/passwd_activemaster.png

   Active keystore password provider

To change the keystore password click the :guilabel:`Change password` link.

.. figure:: images/passwd_changemaster.png

   Changing the keystore password

.. warning:: It is advisable for an Administrator of the System to read :file:`security/masterpw.info`, record the password for your own use, and delete :file:`security/masterpw.info`.

.. _security_webadmin_passwd_keystore:

Keystore Password Providers
'''''''''''''''''''''''''''

This section provides the options for adding, removing, and editing keystore password providers.

.. figure:: images/passwd_masterprovider.png

   Keystore password provider list

.. note:: By default the login to Admin GUI and REST APIs with Keystore Password is disabled. In order to enable it you will need to manually change the Keystore Password Provider ``config.xml``, usually located in :file:`security/masterpw/default/config.xml`, by adding the following statement:

   .. code-block: xml

      <loginEnabled>true</loginEnabled>
   
   For more information see :ref:`security_root`.

Password policies
-----------------

This section configures the various :ref:`security_passwd_policy` available to users in GeoServer.  New password policies can be added or renamed, and existing policies edited or removed.

By default there are two password policies in effect, ``default`` and ``root``.  The ``default`` password policy, intended for most GeoServer users, does not have any active password constraints.  The ``keystore`` password policy, intended for the :ref:`security_root`, specifies a **minimum password length of eight characters**.  Password policies are applied to users via the user/group service.

.. figure:: images/passwd_policies.png

   List of password policies

Clicking an existing policy enables editing, while clicking the :guilabel:`Add new` button will create a new password policy.

.. figure:: images/passwd_newpolicy.png

   Creating a new password policy
