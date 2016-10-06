.. _security_rolesystem_usergroupservices:

User/group services
===================

A **user/group service** provides the following information for users and groups:

* Listing of users
* Listing of groups, including users affiliated with each group
* User passwords

Many authentication providers will make use of a user/group service to perform authentication. In this case, the user/group service would be the database against which users and passwords are authenticated. Depending on how the :ref:`security_auth_chain` is configured, there may be zero, one, or multiple user/group services active at any given time.

A user/group service may be read-only, providing access to user information but not allowing new users and groups to be added or altered. This may occur if a user/group service was configured to delegate to an external service for the users and groups database. An example of this would be an external LDAP server.

By default, GeoServer support two types of user/group services:

* XML—*(Default)* User/group service persisted as XML
* JDBC—User/group service persisted in database via JDBC


.. _security_rolesystem_usergroupxml:

XML user/group service
----------------------

The XML user/group service persists the user/group database in an XML file. This is the default behavior in GeoServer. This service represents the user database as XML, and corresponds to this :download:`XML schema <schemas/users.xsd>`. 

.. note:: 

   The XML user/group file, :file:`users.xml`, is located in the GeoServer data directory, ``security/usergroup/<name>/users.xml``, where ``<name>`` is the name of the user/group service.

The following is the contents of ``users.xml`` that ships with the default GeoServer configuration:

.. code-block:: xml

   <userRegistry version="1.0" xmlns="http://www.geoserver.org/security/users">
     <users>
       <user enabled="true" name="admin" password="crypt1:5WK8hBrtrte9wtImg5i5fjnd8VeqCjDB"/>
     </users>
     <groups/>
   </userRegistry>
  
This particular configuration defines a single user, ``admin``, and no groups. By default, stored user passwords are encrypted using the 
:ref:`weak PBE <security_passwd_encryption>` method.

For further information, please refer to :ref:`configuring a user/group service <security_webadmin_usergroupservices>` in the :ref:`web_admin`.


.. _security_rolesystem_usergroupjdbc:

JDBC user/group service
-----------------------

The JDBC user/group service persists the user/group database via JDBC, managing the user information in  multiple tables. The user/group database schema is as follows:

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
     - varchar(254)
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

The ``users`` table is the primary table and contains the list of users with associated passwords. The ``user_props`` table maps additional properties to a user. (See :ref:`security_rolesystem_usergroups` for more details.)  The ``groups`` table lists all available groups, and the ``group_members`` table maps which users belong to which groups.

The default GeoServer security configuration is:

.. list-table:: Table: users
   :widths: 15 15 15 
   :header-rows: 1

   * - name
     - password
     - enabled
   * - *Empty*
     - *Empty*
     - *Empty*

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

For further information, please refer to :ref:`configuring a user/group service <security_webadmin_usergroupservices>` in the :ref:`web_admin`.

.. _security_rolesystem_usergroupldap:

LDAP user/group service
------------------------

The LDAP user/group service is a read only user/group service that maps users and groups from an LDAP repository to GeoServer users and groups.

Users are extracted from a specific LDAP node, configured as the ``Users search base``. Groups are extracted from a specific LDAP node, configured as the ``Groups search base``. A user is mapped for every matching user and a group is mapped for every matching group. 

It is possible to specify the attributes which contain the name of the group (such as ``cn``), the user (such as ``uid``) as well as the membership relationship between the two (such as ``member``). However, it is also possible to specify specific filters to search for all users/groups (for example ``cn=*``), find a user/group by name  (for example ``cn={0}``) and map users to groups (such as ``member={0}``). These filters can also be automatically derived from the attribute names. Alternatively, the attribute names may be left empty if the filters are provided.

For users, additional properties (key/value pairs, see :ref:`security_rolesystem_usergroups`) may be populated from the LDAP Server by providing a comma separated list of property names.

Retrieving the user/group information can be done anonymously or using a given username/password if the LDAP repository requires it.

An example of configuration file (config.xml) for this type of role service is the following:

   .. code-block:: xml

	<org.geoserver.security.ldap.LDAPUserGroupServiceConfig>
	  <id>2c3e0e8d:154853796a3:-8000</id>
	  <name>myldapservice</name>
	  <className>org.geoserver.security.ldap.LDAPUserGroupService</className>
	  <serverURL>ldap://127.0.0.1:10389/dc=acme,dc=org</serverURL>
	  <groupSearchBase>ou=groups</groupSearchBase>
	  <groupFilter>cn={0}</groupFilter>
	  <groupNameAttribute>cn</groupNameAttribute>
	  <allGroupsSearchFilter>cn=*</allGroupsSearchFilter>
	  <groupSearchFilter>member={0}</groupSearchFilter>
	  <groupMembershipAttribute>member</groupMembershipAttribute>
	  <userSearchBase>ou=people</userSearchBase>
	  <userFilter>uid</userFilter>
	  <userNameAttribute>uid={0}</userNameAttribute>
	  <allUsersSearchFilter>uid=*</allUsersSearchFilter>
	  <useTLS>false</useTLS>
	  <bindBeforeGroupSearch>true</bindBeforeGroupSearch>
	  <user>admin</user>
	  <password>admin</password>
	  <passwordEncoderName>emptyPasswordEncoder</passwordEncoderName>
	  <passwordPolicyName>default</passwordPolicyName>
	  <populatedAttributes>email, telephone</populatedAttributes>
	</org.geoserver.security.ldap.LDAPUserGroupServiceConfig>

For further information, please refer to :ref:`configuring a user/group service <security_webadmin_usergroupservices>` in the :ref:`web_admin`.

