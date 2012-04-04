.. _webadmin_sec_passwd:

Passwords
=========

This page sets the various options related to :ref:`sec_passwd`, the :ref:`sec_master_passwd`, and :ref:`sec_passwd_policy`.

.. note:: Passwords for users are changed in the Users dialogs accessed from the :ref:`webadmin_sec_ugr` page.

Active master password provider
-------------------------------

.. warning:: JUSTIN-TODO: DEETS ABOUT MASTER PSWD PROVIDER STILL NEEDED

This option sets the active master password provider, via a drop-down list of all available master password providers.  

.. figure:: images/passwd_activemaster.png
   :align: center

   *Active master password provider*

Additionally, one can change the master password by clicking the :guilabel:`Change password` link.

.. figure:: images/passwd_changemaster.png
   :align: center

   *Changing the master password*

Master Password Providers
-------------------------

This section allows for the adding, removing, and editing of master password providers.

.. warning:: NEED LINK ABOUT MASTER PASSWORD PROVIDER

.. figure:: images/passwd_masterprovider.png
   :align: center

   *Master password provider list*


Password policies
-----------------

This section sets the various :ref:`sec_passwd_policy` available to users in GeoServer.  New password policies can be added or renamed, and existing policies edited or removed.

By default there are two password policies in effect, ``default`` and ``master``.  The ``default`` password policy, intended for most GeoServer users, does not have any active password constraints.  The ``master`` password policy, intended for the :ref:`sec_root`, specifies a **minimum password length of eight characters**.  Password policies are applied to users via the user/group service.

.. figure:: images/passwd_policies.png
   :align: center

   *List of password policies*

Clicking on an existing policy will open it up for editing, while clicking the :guilabel:`Add new` button will create a new password policy.

.. figure:: images/passwd_newpolicy.png
   :align: center

   *Creating a new password policy*
