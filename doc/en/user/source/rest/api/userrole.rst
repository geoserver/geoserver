.. _rest_api_user_roles:

Users/Groups and Roles
======================

Security
--------

The Users/Groups and Roles Rest API is only accessible to users with the role ROLE_ADMIN.

Input/Output
------------

Data Object Transfer
~~~~~~~~~~~~~~~~~~~~
Both XML and JSON are supported for transfer of data objects. The default is XML. Alternatively, JSON may be used by setting the 'content-type' (POST) and 'accept' (GET) http headers to 'application/json' in your requests.

Encoding of a user in XML::

	<user>
		<userName>..</userName>
		<password>..</password>
		<enabled>true/false</enabled>
	</user>

Encoding of a user in JSON::

	{"userName": "..", "password": "..", enabled: true/false}

Passwords are left out in results of reading requests.

Encoding of a list of users in XML::

	<users>
		<user> ... </user>
		<user> ... </user>
		...		
	</users>

Encoding of a list of users in JSON::

	{"users":[ {..}, {..}, .. ]}

Encoding of a list of groups in XML::

	<groups>
		<group> agroupname </group>
		<group> bgroupname </group>
		...		
	</groups>

Encoding of a list of groups in JSON::

	{"groups":[ {..}, {..}, .. ]}


Encoding of a list of roles::

	<roles>
		<role> arolename </role>
		<role> brolename </role>
		...		
	</roles>

Encoding of a list of roles in JSON::

	{"roles":[ {..}, {..}, .. ]}

Configuration
-------------

The default user/group service is by default the service named "default". This can be 
altered in the following manner: 

    #. Start geoserver with the following java system property present::

          org.geoserver.rest.DefaultUserGroupServiceName=<name_of_usergroupservice>

Requests
--------

``/rest/usergroup/[service/<serviceName>/]users/``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query all users or add a new user in a particular or the default user/group service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - GET
     - List all users in service.
     - 200 OK. List of users in XML.
   * - POST
     - Add a new user
     - 201 Inserted. Created ``ID`` header.


``/rest/usergroup/[service/<serviceName>/]user/<user>``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query, modify or delete a specific user in a particular or the default user/group service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - GET
     - Read user information
     - 200 OK. User in XML.
   * - POST
     - Modify the user, unspecified fields remain unchanged.
     - 200 OK.
   * - DELETE
     - Delete the user
     - 200 OK.

``/rest/usergroup/[service/<serviceName>/]groups/``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query all groups in a particular user/group or the default service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - GET
     - List all groups in service.
     - 200 OK. List of groups in XML.


``/rest/usergroup/[service/<serviceName>/]group/<group>``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Add or delete a specific group in a particular or the default user/group service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - POST
     - Add the group.
     - 200 OK.
   * - DELETE
     - Delete the group.
     - 200 OK.


``/rest/usergroup/[service/<serviceName>/]user/<user>/groups``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query all groups associated with a user in a particular or the default user/group service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - GET
     - List all groups associated with user.
     - 200 OK. List of groups in XML.

``/rest/usergroup/[service/<serviceName>/]group/<group>/users``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query all users associated with a group in a particular or the default user/group service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - GET
     - List all users associated with group.
     - 200 OK. List of groups in XML.

``/rest/usergroup/[service/<serviceName>/]<user>/group/<group>``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Associate or disassociate a specific user with a specific group in a particular or the default user/group service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - POST
     - Associate the user with the group.
     - 200 OK.
   * - DELETE
     - Disassociate the user from the group.
     - 200 OK.



``rest/roles/[service/{serviceName}/]``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query all roles in a particular role service or the active role service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - GET
     - List all roles in service.
     - 200 OK. List of roles in XML.


``/rest/roles/[service/<serviceName>/]role/<role>``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Add or delete a specific role in a particular role service or the active role service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - POST
     - Add the role.
     - 200 OK.
   * - DELETE
     - Delete the role.
     - 200 OK.


``/rest/roles/[service/<serviceName>/]<serviceName>/user/<user>/roles``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Query all roles associated with a user in a particular role service or the active role service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - GET
     - List all roles associated with user.
     - 200 OK. List of roles in XML.


``/rest/roles/[service/<serviceName>/]role/<role>/user/<user>/``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Associate or disassociate a specific user with a specific role in a particular role service or the active role service.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20

   * - Method
     - Action
     - Response
   * - POST
     - Associate the user with the role.
     - 200 OK.
   * - DELETE
     - Disassociate the user from the role.
     - 200 OK.

