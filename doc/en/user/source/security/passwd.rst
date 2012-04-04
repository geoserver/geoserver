.. _sec_passwd:

Passwords
=========

The subject of passwords is a central concept to any security system.  This section describes the way GeoServer handles passwords. 

.. _sec_passwd_encryption:

Password encryption
-------------------

A GeoServer configuration stores two types of passwords:

* Passwords for **user accounts** that are used to access GeoServer resources
* Passwords used internally for **accessing external services** such as databases and cascading OGC services

As these passwords are typically stored on disk it is strongly recommended that they be encrypted and not stored in human-readable text. GeoServer security provides three schemes for encrypting passwords: **plain text**, **Digest**, and **Password-based encryption (PBE)**.

The password encryption scheme is specified in the following places: a global setting that affects the encryption of passwords used for external resources, and for each :ref:`user/group service <sec_rolesystem_usergroupservices>`.  The encryption scheme for external resources should be :ref:`reversible <sec_passwd_reversible>`, while the user/group services can use any scheme.

Plain text
~~~~~~~~~~

.. note::  Prior to version 2.2.0, plain text encryption was the only available method used by GeoServer for storing passwords.

Plain text passwords provide no encryption at all.  In this case, passwords are human-readable by anyone who has access to the file system.  For obvious reasons, this is not recommended for any but the most basic test server.

Digest
~~~~~~

Digest encryption applies a SHA-256 `cryptographic hash function <http://en.wikipedia.org/wiki/Cryptographic_hash_function>`_ 
to passwords.  This scheme is "one-way" in that it is virtually impossible to reverse and obtain the original password from 
its hashed representation.  Please see the section on :ref:`sec_passwd_reversible` for more information on reversibility.

Password-based encryption
~~~~~~~~~~~~~~~~~~~~~~~~~

`Password-based encryption <http://www.javamex.com/tutorials/cryptography/password_based_encryption.shtml>`_ (PBE) is an encryption scheme that employs a user-supplied passphrase to generate an encryption key.  In order to protect from dictionary attacks, a random value called a `salt <http://en.wikipedia.org/wiki/Salt_%28cryptography%29>`_ is added to the passphrase when generating the key.

GeoServer comes with support for two forms of PBE.  **Weak PBE** (the GeoServer default) uses a basic encryption method that is relatively easy to crack. **Strong PBE** uses a much stronger encryption method based on an AES 256-bit algorithm.  It is highly recommended to use Strong PBE.

.. warning:: JUSTIN-TODO:  BE MORE SPECIFIC ABOUT ALGORITHM!

.. _sec_passwd_encryption_policies:

.. note::

   Strong PBE is not natively available on all Java virtual machines and may require the installation of some additional `JCE Unlimited Strength Jurisdiction <http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html>`_ policy files:

   * `Oracle JCE policy jars <http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html>`_ for Oracle JVM
   * `IBM JCE policy jars <https://www14.software.ibm.com/webapp/iwm/web/preLogin.do?source=jcesdk>`_ for IBM JVM

   .. warning:: THIS DIDN'T WORK FOR ME.


.. _sec_passwd_reversible:

Reversible encryption
~~~~~~~~~~~~~~~~~~~~~

Password encryption methods can be **reversible**, meaning that it is possible (and desired) to obtain the plain-text password from its encrypted version.  Reversible passwords are necessary for database connections or external OGC services such as :ref:`cascading WMS <data_external_wms>` and :ref:`cascading WFS <data_external_wfs>`, since GeoServer must be able to decode the encrypted password and pass it to the external service. Plain text and PBE passwords are reversible.  

Non-reversible passwords provide the highest level of security, and therefore should be used for user accounts and wherever else it is possible.  Digest is the only encryption scheme that is non-reversible.

.. _sec_passwd_keystore:

Secret keys and the keystore
----------------------------

For a reversible password to provide a meaningful level of security, access to the password must be restricted in some way.  In GeoServer, encrypting and decrypting passwords involves the generation of secret (private) keys, stored in a typical Java *keystore*.  GeoServer uses its own keystore for this purpose named ``geoserver.jceks`` which is located in the ``security`` directory in the GeoServer data directory. This file is stored in the `JCEKS format rather than the default JKS <http://www.itworld.com/nl/java_sec/07202001>`_.

The GeoServer keystore is password protected with a :ref:`sec_master_passwd`. It is possible to access the contents of the 
keystore with external tools such as `keytool <http://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html>`_. For example, this following command would prompt for the master password and list the contents of the keystore:

.. code-block:: bash

  $ keytools -list -keystore geoserver.jceks -storetype "JCEKS"

.. _sec_master_passwd:

Master password
---------------

GeoServer contains the ability to set a **master password** that serves two purposes:

* Protect access to the :ref:`keystore <sec_passwd_keystore>`
* Protect access to the GeoServer :ref:`sec_root`

By default, the master password is set to ``geoserver``, though for obvious reasons it is strongly recommended that the master password be 
changed **immediately** following any GeoServer installation.

.. warning:: JUSTIN-TODO: EXPLAIN MASTER PASSWORD PROVIDER

.. warning:: SHOULD ADD THIS INFO TO GS IN PROD!

.. _sec_passwd_policy:

Password policies
-----------------

A password policy defines constraints on passwords such as password length, case, and required mix of character classes. Password
policies are specified when adding :ref:`sec_rolesystem_usergroupservices` and used to constrain passwords when creating new users and when changing passwords of existing users.

Each user/group service uses a password policy to enforce these rules. The default GeoServer password policy allows the following optional constraints:

* Passwords must contain at least one number
* Passwords must contain at least one upper case letter
* Passwords must contain at least one lower case letter
* Password minimum length
* Password maximum length

