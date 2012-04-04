.. _sec_rolesystem_roleservices:

Role services
=============

A **role service** is a source of information for roles. It provides the following:

* List of roles
* Calculation of role assignments for a given user

When a user/group service loads information about a user or a group, it delegates to the role service to determine which 
roles should be assigned to the user or group.  Unlike :ref:`sec_rolesystem_usergroupservices`, there is only a single role service active at any given time.

GeoServer comes by default with support for two types of role services:

* XML - *(Default)* role service persisted as XML
* JDBC - Role service persisted in a database via JDBC


.. _sec_rolesystem_rolexml:

XML role service
----------------

The XML role service persists the role database in an XML file.  This is the default role service in GeoServer.

This service represents the user database as XML corresponding to this :download:`XML schema <schemas/roles.xsd>`. The file is 
named :file:`roles.xml` and is located inside the GeoServer data directory at a path of ``security/role/<name>/roles.xml``, where
``<name>`` is the name of the role service.

The following is the contents of ``roles.xml`` that ships with the default GeoServer configuration:

.. code-block:: xml

   <roleRegistry version="1.0" xmlns="http://www.geoserver.org/security/roles">
     <roleList>
       <role id="ROLE_ADMINISTRATOR"/>
     </roleList>
     <userList>
       <userRoles username="admin">
         <roleRef roleID="ROLE_ADMINISTRATOR"/>
       </userRoles>
     </userList>
     <groupList/>
   </roleRegistry>

This configuration contains a single role named ``ROLE_ADMINISTRATOR`` and assigns the role to the ``admin`` user.

Read more on :ref:`configuring a role service <webadmin_sec_roleservices>` in the :ref:`web_admin`.


.. _sec_rolesystem_rolejdbc:

JDBC role service
-----------------

The JDBC role service persists the role database via JDBC.  It represents the role database with multiple tables.  The following shows the database schema:

.. list-table:: Table: users
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
   * - ``ROLE_ADMINISTRATOR``
     - ``NULL``


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
   * - ``admin``
     - ``ROLE_ADMINISTRATOR``

.. list-table:: Table: group_roles
   :widths: 15 15 
   :header-rows: 1

   * - groupname
     - rolename
   * - *Empty*
     - *Empty*

Read more on :ref:`configuring a role service <webadmin_sec_roleservices>` in the :ref:`web_admin`.
