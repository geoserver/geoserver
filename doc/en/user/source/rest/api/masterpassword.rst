.. _rest_api_masterpassword:

Keystore Password
=================

The ``keystore password`` is used to encrypt the GeoServer key store and for an emergency login using
the user ``root``.


.. warning::

   The use of HTTPS is recommended, otherwise all password are sent in plain text.

``/security/masterpw[.<format>]``
---------------------------------

Fetches the keystore password and allows to change the keystore password

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Fetch the keystore password
     - 200,403
     - XML, JSON
     - 
   * - PUT
     - Changes the keystore password
     - 200,405,422
     - XML, JSON
     -

Formats for PUT (keystore password change).

**XML**

.. code-block:: xml
 
   <masterPassword>
      <oldMasterPassword>oldPassword</oldMasterPassword>
      <newMasterPassword>newPassword</newMasterPassword>
   </masterPassword>

**JSON**

.. code-block:: json

   { "oldMasterPassword":"oldPassword",
     "newMasterPassword":"newPassword" }


Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET without administrative privileges
     - 403
   * - PUT without administrative privileges
     - 405
   * - PUT with the wrong current keystore password
     - 422
   * - PUT with a new keystore password rejected by the password policy
     - 422

