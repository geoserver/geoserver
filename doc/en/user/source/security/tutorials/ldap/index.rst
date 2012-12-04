.. _sec_tutorials_ldap:

Authentication with LDAP
========================

This tutorial introduces GeoServer LDAP support and walks through the process of setting up authentication against an LDAP server.

.. note:: Read more about the :ref:`LDAP authentication provider <sec_auth_provider_ldap>`.

LDAP server setup
-----------------

A mock LDAP server will be used for this tutorial. Download and run the `acme-ldap <http://files.opengeo.org/geoserver/acme-ldap.jar>`_ jar:: 

  java -jar acme-ldap.jar

The output of which should look like the following::

  Directory contents:
    ou=people,dc=acme,dc=org
      uid=bob,ou=people,dc=acme,dc=org
      uid=alice,ou=people,dc=acme,dc=org
      uid=bill,ou=people,dc=acme,dc=org
    ou=groups,dc=acme,dc=org
    cn=user,ou=groups,dc=acme,dc=org
      member: uid=bob,ou=people,dc=acme,dc=org
      member: uid=alice,ou=people,dc=acme,dc=org
    cn=admin,ou=groups,dc=acme,dc=org
      member: uid=bill,ou=people,dc=acme,dc=org

    Server running on port 10389

The following diagram illustrates the hierarchy of this LDAP datatabse:

  .. figure:: images/acme_ldap.png

     *Diagram of custom LDAP*

The LDAP tree consists of:

* The root domain component, ``dc=acme,dc=org``
* Two organizational units named ``people`` and ``groups``
* Two groups named ``user`` and ``admin``
* Two users named ``bob`` and ``alice`` who are members of the ``user`` group
* One user named ``bill`` who is a member of the ``admin`` group


Configure the LDAP authentication provider
------------------------------------------

#. Start GeoServer and login to the web admin interface as the admin user. 

#. Click the :guilabel:`Authentication` link located under the :guilabel:`Security` section of the navigation sidebar.

   .. figure:: images/authlink.png
      :align: center

      *Click to bring up the Authentication settings page*        
 
#. Scroll down to the :guilabel:`Authentication Providers` panel and click the :guilabel:`Add new` link.

   .. figure:: images/addnewauthprovider.png
      :align: center

      *Click to create a new authentication provider*
 
#. Click the :guilabel:`LDAP` link.

   .. figure:: images/newldapauthprovider.png
      :align: center

      *Selecting a new LDAP authentication provider*
 
#. Fill in the form as follows:

   .. list-table::
      :header-rows: 1

      * - Field
        - Description
      * - :guilabel:`Name`
        - ``acme-ldap``
      * - :guilabel:`Server URL`
        - ``ldap://localhost:10389/dc=acme,dc=org``
      * - :guilabel:`User lookup pattern`
        - ``uid={0},ou=people``

   .. figure:: images/ldapsettings.png
      :align: center

      *Configuring an LDAP authentication provider*
   
#. Test the LDAP connection by entering the user name :guilabel:`bob` and password :guilabel:`secret` in the connection test form located on the right side of the page and clicking the :guilabel:`Test Connection` button. A successful connection should be reported at the top of the page.

   .. figure:: images/testconnection.png
      :align: center

      *Testing the connection to the LDAP connection*

#. Click :guilabel:`Save`.

#. Back on the authentication page, scroll down to the :guilabel:`Provider Chain` panel and click the right-arrow button to move the :guilabel:`acme-ldap` provider from :guilabel:`Available` to :guilabel:`Selected`.

   .. figure:: images/selectedprovider.png
      :align: center

      *Activating the LDAP authentication provider*

#. Click :guilabel:`Save`.

Test the LDAP login
-------------------

#. Log out of the admin account.

#. Login as the user :guilabel:`bob` with the with the password :guilabel:`secret`.

   .. figure:: images/boblogin.png
      :align: center

      *Logging in as user "bob"*

   .. figure:: images/bobloggedin.png
      :align: center

      *Successfully logged in*

#. While the connection was successful, logging in as "bob" doesn't yield any administrative functionality because the account has not been mapped to the administrator role. In the next section GeoServer will be configured to map groups from the LDAP database to roles.

Map LDAP groups to GeoServer roles
----------------------------------

When using LDAP for authentication, GeoServer maps LDAP groups to GeoServer roles by prefixing the group name with ``ROLE_`` and converting the result to uppercase. For example "bob" and "alice" are members of the **user** group, so they would be assigned a role named ``ROLE_USER``. Similarly "bill" is a member of the **admin** group, so he would be assigned a role named ``ROLE_ADMIN``.

#. Log out and log back in as the admin user.

#. Navigate to the :guilabel:`Authentication` page as before.

#. Scroll to the :guilabel:`Authentication Providers` panel and click the :guilabel:`acme-ldap` link.

   .. figure:: images/acmeproviderlink.png
      :align: center

      *Click to access the LDAP connection settings*

#. Fill in the following form fields:

   .. list-table::
      :header-rows: 1

      * - Field
        - Description
      * - :guilabel:`Group search base`
        - ``ou=groups``
      * - :guilabel:`Group search filter`
        - ``member={0}``

   The first field specifies the node of the LDAP directory tree at which groups are located. In this case the organizational unit named **groups**. The second field specifies the LDAP query filter to use in order to locate those groups that a specific user is a member of. The ``{0}`` is a placeholder which is replaced with the UID of the user.

   .. figure:: images/acmegroups.png
      :align: center

      *Mapping LDAP groups to GeoServer roles*

#. Click :guilabel:`Save`.

Map a GeoServer role to the administrator role
----------------------------------------------

At this point the LDAP provider will populate an authenticated user with roles based on the groups the user is a member of. However, the GeoServer administrative role is named ``ROLE_ADMINISTRATOR``, not ``ROLE_ADMIN``, so "bill" who is assigned the role ``ROLE_ADMIN`` will not be granted administrative rights. To remedy this, the GeoServer role service will be reconfigured to treat ``ROLE_ADMIN`` as an administrative role. 

.. todo:: Perhaps it makes more sense to use an admin group.

#. Click the :guilabel:`Users, Groups, Roles` link located under the :guilabel:`Security` section of the navigation sidebar.

   .. figure:: images/ugrlink.png
      :align: center

      *Click to bring up the Users, Groups, and Roles settings page*

#. Click the :guilabel:`default` role service in the :guilabel:`Role Services`` panel.

   .. figure:: images/defaultroleservice.png
      :align: center

      *Click to edit the settings of the default role service*

#. Click the :guilabel:`Roles` tab.

#. Click :guilabel:`Add new role`.

   .. figure:: images/addnewrolelink.png
      :align: center

      *Click to add a new role*

#. Enter :guilabel:`ROLE_ADMIN` in the :guilabel:`Name field`. Leave the other fields as-is.

   .. figure:: images/roleadmin.png
      :align: center

      *Creating a new role*

#. Click :guilabel:`Save`.

   .. figure:: images/allroles.png
      :align: center

      *New role successfully created*

   .. todo:: Won't this make the admin account no longer an admin?

#. Click the :guilabel:`Settings` tab.

#. Select :guilabel:`ROLE_ADMIN` in the :guilabel:`Administrator role` box.
   
   .. figure:: images/settingadminrole.png
      :align: center

      *Setting the admin role*

#. Click :guilabel:`Save`.

At this point members of the ``admin`` LDAP group (in this case, the user "bill") should be given full administrative privileges once authenticated. Log out of the admin account and log in as "bill" with the password "hello". Once logged in full administrative functionality should be available.

.. todo:: This doesn't work. Boo.

.. todo:: Add a screenshot that shows bill with admin access (when fixed)