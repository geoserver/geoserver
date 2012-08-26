.. _sec_rolesystem_roleservices:

Role services
=============

A **role service** is a source of information for roles. It provides the following:

* List of roles
* Calculation of role assignments for a given user
* Mapping of a role to the system role ``ROLE_ADMINISTRATOR``
* Mapping of a role to the system role ``ROLE_GROUP_ADMIN``

When a user/group service loads information about a user or a group, it delegates to the role service to determine which 
roles should be assigned to the user or group.  Unlike :ref:`sec_rolesystem_usergroupservices`, there is only a single role service active at any given time.

GeoServer comes by default with support for two types of role services:

* XML - *(Default)* role service persisted as XML
* JDBC - Role service persisted in a database via JDBC


.. _sec_rolesystem_mapping:

Mapping roles to system roles
-----------------------------

For assigning the system role ``ROLE_ADMINISTRATOR`` to a user or to a group, it is necessary to add a role with a different name and to map this role to ``ROLE_ADMINISTRATOR``. The same holds true for the system role ``ROLE_GROUP_ADMIN``. The mapping is stored in the ``config.xml`` file of the service.

.. code-block:: xml

	<roleService>
	  <id>471ed59f:13915c479bc:-7ffc</id>
	  <name>default</name>
	  <className>org.geoserver.security.xml.XMLRoleService</className>
	  <fileName>roles.xml</fileName>
	  <checkInterval>10000</checkInterval>
	  <validating>true</validating>
	  <adminRoleName>ADMIN</adminRoleName>
	  <groupAdminRoleName>GROUP_ADMIN</groupAdminRoleName>
	</roleService>

In this example, a user or a group having the role ``ADMIN`` has also the system role ``ROLE_ADMINISTRATOR``. Again, the same holds true for ``GROUP_ADMIN`` and ``ROLE_GROUP_ADMIN``.


.. _sec_rolesystem_rolexml:

XML role service
----------------

The XML role service persists the role database in an XML file.  This is the default role service in GeoServer.

This service represents the user database as XML corresponding to this :download:`XML schema <schemas/roles.xsd>`. The file is 
named :file:`roles.xml` and is located inside the GeoServer data directory at a path of ``security/role/<name>/roles.xml``, where
``<name>`` is the name of the role service.

The service is configured to map the local role ``ADMIN`` to the system role ``ROLE_ADMINISTRATOR``. Additionally, ``GROUP_ADMIN`` is mapped to ``ROLE_GROUP_ADMIN``. The mapping is stored the ``config.xml`` of each role service, 

The following is the contents of ``roles.xml`` that ships with the default GeoServer configuration:

.. code-block:: xml

   <roleRegistry version="1.0" xmlns="http://www.geoserver.org/security/roles">
     <roleList>
       <role id="ADMIN"/>
       <role id="GROUP_ADMIN"/>
     </roleList>
     <userList>
       <userRoles username="admin">
         <roleRef roleID="ADMIN"/>
       </userRoles>
     </userList>
     <groupList/>
   </roleRegistry>

This configuration contains two roles named ``ADMIN`` and ``GROUP_ADMIN``.  The role ``ADMIN`` is assigned to the ``admin`` user. Since the ``ADMIN`` role is mapped to the system role ``ROLE_ADMINISTRATOR`` the role calculation assigns both roles to the ``admin`` user.

Read more on :ref:`configuring a role service <webadmin_sec_roleservices>` in the :ref:`web_admin`.


.. _sec_rolesystem_rolejdbc:

JDBC role service
-----------------

The JDBC role service persists the role database via JDBC.  It represents the role database with multiple tables.  The following shows the database schema:

.. list-table:: Table: roles
   :widths: 15 15 15 15 
   :header-rows: 1

   * - Field
     - Type
     - Null
     - Key
   * - name
     - varchar(64)
     - NO
     - PRI
   * - parent
     - varchar(64)
     - YES
     - 

.. list-table:: Table: role_props
   :widths: 15 15 15 15 
   :header-rows: 1

   * - Field
     - Type
     - Null
     - Key
   * - rolename
     - varchar(64)
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

.. list-table:: Table: user_roles
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
   * - rolename
     - varchar(64)
     - NO
     - PRI

.. list-table:: Table: group_roles
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
   * - rolename
     - varchar(64) 
     - NO
     - PRI

The ``roles`` table is the primary table and contains the list of roles.  Roles in GeoServer support inheritance, so a role may optionally have a link to a parent role. The ``role_props`` table is a mapping table that maps additional properties to a role. (See the section on :ref:`sec_rolesystem_roles` for more details.)  The ``user_roles`` table maps users to the roles they are assigned.  Similarly the ``group_roles`` table does the same but for groups rather than users. 

The default GeoServer security configuration would be represented with the following database contents:

.. list-table:: Table: roles
   :widths: 15 15 
   :header-rows: 1

   * - name
     - parent
   * - *Empty*
     - *Empty*


.. list-table:: Table: role_props
   :widths: 15 15 15
   :header-rows: 1

   * - rolename
     - propname
     - propvalue
   * - *Empty*
     - *Empty*
     - *Empty*

.. list-table:: Table: user_roles
   :widths: 15 15 
   :header-rows: 1

   * - username
     - rolename
   * - *Empty*
     - *Empty*

.. list-table:: Table: group_roles
   :widths: 15 15 
   :header-rows: 1

   * - groupname
     - rolename
   * - *Empty*
     - *Empty*

Read more on :ref:`configuring a role service <webadmin_sec_roleservices>` in the :ref:`web_admin`.
