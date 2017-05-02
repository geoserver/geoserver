.. _rest_security:

Security
========

The REST API allows you to adjust GeoServer security settings.

.. note:: Read the :api:`API reference for /security <security.yaml>`.

Listing the master password
---------------------------

**Retrieve the master password for the "root" account**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/security/masterpw.xml


Changing the master password
----------------------------

**Change to a new master password**

.. note:: Requires knowledge of the current master password.


Given a ``changes.xml`` file:

.. code-block:: xml

   <masterPassword>
      <oldMasterPassword>-"}3a^Kh</oldMasterPassword>
      <newMasterPassword>geoserver1</newMasterPassword>
   </masterPassword>

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @change.xml http://localhost:8080/geoserver/rest/security/masterpw.xml

*Response*

::

  200 OK


Listing the catalog mode
------------------------

**Fetch the current catalog mode**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET   http://localhost:8080/geoserver/rest/security/acl/catalog.xml

*Response*

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <catalog>
        <mode>HIDE</mode>
    </catalog>

Changing the catalog mode
-------------------------

**Set a new catalog mode** 

Given a ``newMode.xml`` file:

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <catalog>
        <mode>MIXED</mode>
    </catalog>

*Request*

.. admonition:: curl

   ::
   
       curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @newMode.xml http://localhost:8080/geoserver/rest/security/acl/catalog.xml


Listing access control rules
----------------------------

**Retrieve current list of access control rules**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/security/acl/layers.xml

*Response*

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <rules />

.. note:: The above response shows no rules specified.

Changing access control rules
-----------------------------

**Set a new list of access control rules**

Given a ``rules.xml`` file:

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <rules>
      <rule resource="topp.*.r">ROLE_AUTHORIZED</rule>
      <rule resource="topp.mylayer.w">ROLE_1,ROLE_2</rule>      
   </rules>

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d @rules.xml http://localhost:8080/geoserver/rest/security/acl/layers.xml 
   
*Response*

::

  201 Created



Deleting access control rules
-----------------------------

**Delete individual access control rule**

*Request*

.. admonition:: curl

   ::

     curl -v -u admin:geoserver -XDELETE  http://localhost:8080/geoserver/rest/security/acl/layers/topp.*.r

   
*Response*

::

  200 OK
