.. _rest_api_accesscontrol:

Access Control
==============


``/security/acl/catalog.<format>``
----------------------------------

Fetches the catalog mode and allows to change the catalog mode. The mode must be one of 

   * HIDE
   * MIXED
   * CHALLENGE

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Fetch the catalog mode
     - 200,403
     - XML, JSON
     - 
   * - PUT
     - Set the catalog mode
     - 200,403,404,422
     - XML, JSON
     -

Formats:

**XML**

.. code-block:: xml
 
   <catalog>
     <mode>HIDE</mode>
   </catalog>
 

**JSON**

.. code-block:: json

   {"mode":"HIDE" }



Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - No administrative privileges
     - 403
   * - Malformed request
     - 404     
   * - Invalid catalog mode
     - 422

``/security/acl/layers.<format>``
---------------------------------
``/security/acl/services.<format>``
-----------------------------------
``/security/acl/rest.<format>``
-------------------------------

API for administering access control for 

   * Layers
   * Services
   * The REST API 
   
.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Fetch all rules
     - 200,403
     - XML, JSON
     - 
   * - POST
     - Add a set of rules
     - 200,403,409
     - XML, JSON
     -
   * - PUT
     - Modify a set of rules
     - 200,403,409
     - XML, JSON
     -
   * - DELETE
     - Delete a specific rule
     - 200,404,409
     - XML, JSON
     -
   
   
Format for DELETE:

The specified rule has to be the last part in the URI::

   /security/acl/layers/*.*.r

.. note::
   
   Slashes ("/") in a rule name must be encoded with **%2F**. The REST rule **/\*\*;GET** must be encoded
   to /security/acl/rest/**%2F\*\*;GET**           
   
     
Formats for GET,POST and PUT:

**XML**

.. code-block:: xml
 
   <?xml version="1.0" encoding="UTF-8"?>
   <rules>
      <rule resource="*.*.r">*</rule>
      <rule resource="myworkspace.*.w">ROLE_1,ROLE_2</rule>
   </rules> 


**JSON** ::

   {
   "*.*.r": "*",
   "myworkspace".*.w": "ROLE_1,ROLE_2"
   }
   
      
The resource attribute specifies a rule. There are three different formats.
 
   * For layers: <workspace>.<layer>.<access>. The asterisk is a wild card for <workspace>
     and <layer>. <access> is one of **r** (read), **w** (write) or **a** (administer).
     
   * For services: <service>.<method>. The asterisk is a wild card wild card for <service>
     and <method>. Examples:
     
     *  wfs.GetFeature
     *  wfs.GetTransaction
     *  wfs.*
     
   * For REST: <URL Ant pattern>;<comma separated list of HTTP methods>. Examples:
   
     *  /\*\*;GET
     *  /\*\*;POST,DELETE,PUT
     
The content of a rule element is a comma separated list of roles or the asterisk.

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - No administrative privileges
     - 403
   * - POST, adding an already existing rule
     - 409     
   * - PUT, modifying a non existing rule
     - 409
   * - DELETE, Deleting a non existing rule
     - 409                         
   * - Invalid rule specification   
     - 422
     
.. note::

   When adding a set of rules and only one role does already exist, the whole request is aborted.
   When modifying a set of rules and only one role does not exist, the whole request is aborted too.
   