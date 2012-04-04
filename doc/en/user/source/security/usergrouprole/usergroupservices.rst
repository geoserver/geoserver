.. _sec_rolesystem_usergroupservices:

User/group services
===================

A **user/group service** is a source of information for users and groups. It provides the following:

* Listing of users
* Listing of groups, including users affiliated with each group
* User passwords

Many types of authentication providers will make use of a user/group service to perform authentication.  In this case, the user/group service would be the database for looking up users and performing password authentication.  Depending on how the :ref:`sec_auth_chain` is configured there can be zero or more user/group services active at any given time.

A user/group service can be read-only, in that it only provides a source of user information and does not allow for the adding/changing of new users and groups. Such a case would occur if a user/group service was set up to delegate to some external service for the database of users and groups. An example of this would be an external LDAP server.

GeoServer comes by default with support for two types of user/group services:

* XML - *(Default)* User/group service persisted as XML
* JDBC - User/group service persisted in database via JDBC


.. _sec_rolesystem_usergroupxml:

XML user/group service
----------------------

The XML user/group service persists the user/group database in an XML file.  This is the default behavior in GeoServer.

This service represents the user database as XML corresponding to this :download:`XML schema <schemas/users.xsd>`. The file is 
named :file:`users.xml` and is located inside the GeoServer data directory at a path of ``security/usergroup/<name>/users.xml``, where
``<name>`` is the name of the user/group service.

The following is the contents of ``users.xml`` that ships with the default GeoServer configuration:

.. code-block:: xml

   <userRegistry version="1.0" xmlns="http://www.geoserver.org/security/users">
     <users>
       <user enabled="true" name="admin" password="crypt1:5WK8hBrtrte9wtImg5i5fjnd8VeqCjDB"/>
     </users>
     <groups/>
   </userRegistry>
  
This configuration contains a single user named ``admin`` and no groups. User passwords are stored encrypted by default using the 
:ref:`weak PBE <sec_passwd_encryption>` method.

Read more on :ref:`configuring a user/group service <webadmin_sec_usergroupservices>` in the :ref:`web_admin`.


.. _sec_rolesystem_usergroupjdbc:

JDBC user/group service
-----------------------

The JDBC user/group service persists the user/group database via JDBC.  It represents the user database with multiple tables.  The following shows the database schema:

.. list-table:: Table: users
   :widths: 15 15 15 15 
   :header-rows: 1

   * - Field
     - Type
     - Null
     - Key
   * - name
     - varchar(128)
     - NO
     - PRI
   * - password
     - varchar(64)
     - YES
     - 
   * - enabled
     - char(1)
     - NO
     - 

.. list-table:: Table: user_props
   :widths: 15 15 15 15 
   :header-rows: 1

   * - Field
     - Type
     - Null
     - Key
   * - username
     - varchar(128)
     - NO
     - PRI
   * - propname
     - varchar(64)
     - NO
     - PRI
   * - propvalue
     - varchar(2048)
     - YES
     - 

.. list-table:: Table: groups
   :widths: 15 15 15 15 
   :header-rows: 1

   * - Field
     - Type
     - Null
     - Key
   * - name
     - varchar(128)
     - NO
     - PRI
   * - enabled
     - char(1)
     - NO
     - 

.. list-table:: Table: group_members
   :widths: 15 15 15 15 
   :header-rows: 1

   * - Field
     - Type
     - Null
     - Key
   * - groupname
     - varchar(128)
     - NO
     - PRI
   * - username
     - varchar(128) 
     - NO
     - PRI

The ``users`` table is the primary table and contains the list of users with associated passwords. The ``user_props`` table is 
a mapping table that maps additional properties to a user. (See :ref:`sec_rolesystem_usergroups` for more details.)  The ``groups`` table lists all available groups, and the ``group_members`` table contains the mapping of users to the groups they are associated with.

The default GeoServer security configuration would be represented with the following database contents:

.. list-table:: Table: users
   :widths: 15 15 15 
   :header-rows: 1

   * - name
     - password
     - enabled
   * - ``admin``
     - ``digest1:UTb...``
     - ``Y``

.. list-table:: Table: user_props
   :widths: 15 15 15 
   :header-rows: 1

   * - username
     - propname
     - propvalue
   * - *Empty*
     - *Empty*
     - *Empty*

.. list-table:: Table: groups
   :widths: 15 15
   :header-rows: 1

   * - name
     - enabled
   * - *Empty*
     - *Empty*

.. list-table:: Table: group_members
   :widths: 15 15
   :header-rows: 1

   * - groupname
     - username
   * - *Empty*
     - *Empty*

Read more on :ref:`configuring a user/group service <webadmin_sec_usergroupservices>` in the :ref:`web_admin`.

