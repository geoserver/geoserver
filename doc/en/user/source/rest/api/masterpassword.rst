.. _rest_api_masterpassword:

Root Password
===============

The ``root password`` is used to encrypt the GeoServer key store and for an emergency login using
the user ``root``.


.. warning::

   The use of HTTPS is recommended, otherwise all password are sent in plain text.

``/security/masterpw[.<format>]``
---------------------------------

Fetches the root password and allows to change the root password

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Fetch the root password
     - 200,403
     - XML, JSON
     - 
   * - PUT
     - Changes the root password
     - 200,405,422
     - XML, JSON
     -

Formats for PUT (root password change).

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
   * - PUT with the wrong current root password
     - 422
   * - PUT with a new root password rejected by the password policy
     - 422

