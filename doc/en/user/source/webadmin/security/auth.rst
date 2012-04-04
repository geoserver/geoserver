.. _webadmin_sec_auth:

Authentication
==============

This page allows for setting details of authentication, including authentication providers and the authentication chain.

Anonymous authentication
------------------------

GeoServer will by default allow anonymous access to the :ref:`web_admin`.  Without authentication, users will still be able to view the :ref:`layerpreview`, capabilities documents, and basic GeoServer details.  Disabling anonymous access by unchecking the :guilabel:`Allow anonymous authentication` checkbox, will disallow this.  Anonymous users navigating to the GeoServer page will be met with a HTTP 401 status code, which typically results in a browser-based request for credentials.

.. note:: Read more about :ref:`sec_auth_webadmin`.

.. figure:: images/auth_anonymous.png
   :align: center

   *Anonymous authentication checkbox*

Authentication providers
------------------------

This section allows for the adding, removing, and editing of :ref:`sec_auth_providers`.  The default authentication provider uses basic :ref:`username/password authentication <sec_auth_provider_userpasswd>`.  :ref:`JDBC <sec_auth_provider_jdbc>` and :ref:`LDAP <sec_auth_provider_ldap>` can also be used.

Clicking on :guilabel:`Add new` will create a new provider.  Clicking on an existing provider will allow editing of that provider's parameters.

.. figure:: images/auth_providers.png
   :align: center

   *List of authentication providers*

Username/password provider
~~~~~~~~~~~~~~~~~~~~~~~~~~

The default new authentication provider is that which utilizes a user/group service for authentication.

.. figure:: images/auth_userpass.png
   :align: center

   *Creating a new authentication provider with a username and password*

.. list-table:: 
   :widths: 40 60 
   :header-rows: 1

   * - Option
     - Description
   * - Name
     - Name of the provider
   * - User Group Service
     - Name of the user/group service associated with this provider.  Can be any one of the active user/group services.

JDBC provider
~~~~~~~~~~~~~

The following shows the configuration options for the JDBC authentication provider.

.. figure:: images/auth_jdbc.png
   :align: center

   *Configuring the JDBC authentication provider*



.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - Option
     - Description
   * - Name
     - Name of the JDBC connection in GeoServer
   * - User Group Service
     - Name of the user/group service to use to load user information after the user is authenticated
   * - Driver class name
     - JDBC driver to use for the database connection
   * - Connection URL
     - JDBC URL to use when creating the database connection

LDAP provider
~~~~~~~~~~~~~

The following shows the configuration options for the LDAP authentication provider.  The default option is to use LDAP groups for role assignment, but there is an option to use a user/group service for role assignment.  Depending on whether this box is checked the menu itself will have different options.

.. figure:: images/auth_ldap1.png
   :align: center

   *Configuring the LDAP authentication provider using LDAP groups for role assignment*

.. figure:: images/auth_ldap2.png
   :align: center

   *Configuring the LDAP authentication provider using user/group service for role assignment*


.. list-table::
   :widths: 40 60
   :header-rows: 1

   * - Option
     - Description
   * - Name
     - Name of the LDAP connection in GeoServer
   * - Server URL
     - URL for the LDAP server connection. It must include the protocol, host, and port, as well as the "distinguished name" (DN) for the root of the LDAP tree.
   * - TLS
     - Enables a STARTTLS connection. (See the section on :ref:`sec_auth_provider_ldap_secure`.)
   * - User DN pattern
     - Search pattern to use to match the DN of the user in the LDAP database. The pattern should contain the placeholder ``{0}`` which is injected with the ``uid`` of the user.  Example: ``uid={0},ou=people``.  The root DN specified as port of the *Server URL* is automatically appended.
   * - Use LDAP groups for authorization
     - Specifies whether to use LDAP groups for role assignment. 
   * - Group search base
     - Relative name of the node in the tree to use as the base for LDAP groups. Example: ``ou=groups``.  The root DN specified as port of the *Server URL* is automatically appended.  Only applicable when the *Use LDAP groups for authorization( parameter is **checked**.
   * - Group search filter
     - Search pattern to use in order to locate the user's LDAP groups that the user is part of. The pattern may contain two placeholder values:
       * ``{0}``, is the full DN of the user.  Example: ``uid=bob,ou=people,dc=acme,dc=com``.
       * ``{1}``, is the ``uid`` portion of the full DN.  Example: ``bob``. 
       Only applicable when the *Use LDAP groups for authorization( parameter is **checked**.
   * - User Group Service
     - The user/group service to use for role assignment.  Only applicable when the *Use LDAP groups for authorization* parameter is **unchecked**.


Authentication chain
--------------------

This section sets the authentication chain.  Currently, only one default authentication chain is available.  (Read more about the default :ref:`sec_auth_chain`.)

.. figure:: images/auth_chain.png
   :align: center

   *Selecting the authentication chain*

