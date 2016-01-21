.. _security_tutorials_activedirectory:

Authentication with LDAP against ActiveDirectory 
================================================

This tutorial explains how to use GeoServer LDAP support to connect to a Windows Domain using ActiveDirectory as an LDAP server. It is recommended that the 
:ref:`security_auth_provider_ldap` section be read before proceeding.

Windows Server and ActiveDirectory
----------------------------------
Active Directory is just another LDAP server implementation, but has some features that we must know to successfully use it with GeoServer LDAP authentication.
In this tutorial we will assume to have a Windows Server Domain Controller with ActiveDirectory named ``domain-controller`` for a domain named ``ad.local``.
If your environment uses different names (and it surely will) use your real names where needed.

We will also assume that:

    * a group named ``GISADMINGROUP`` exists.
    * a user named ``GISADMIN`` exists, has password ``secret``, and belongs to the ``GISADMINGROUP`` group.
    * a user named ``GISUSER`` exists, has password ``secret``, and does NOT belong to the ``GISADMINGROUP`` group.

.. note:: ADMINISTRATOR cannot be generally used as the admin group name with ActiveDirectory, because Administrator is the master user name in Windows environment.

Configure the LDAP authentication provider
------------------------------------------

#. Start GeoServer and login to the web admin interface as the ``admin`` user.
#. Click the ``Authentication`` link located under the ``Security`` section of
   the navigation sidebar.

    .. figure:: images/ldap1.jpg
       :align: center

#. Scroll down to the ``Authentication Providers`` panel and click the ``Add new`` link.

    .. figure:: images/ldap2.jpg
       :align: center

#. Click the ``LDAP`` link.

    .. figure:: images/ldap3.jpg
       :align: center

#. Fill in the fields of the settings form as follows:

   * Set ``Name`` to "ad-ldap"
   * Set ``Server URL``  to "ldap://domain-controller/dc=ad,dc=local
   * Set ``Filter used to lookup user`` to ``(|(userPrincipalName={0})(sAMAccountName={1}))``
   * Set ``Format used for user login name`` to "{0}@ad.local"
   * Check ``Use LDAP groups for authorization``
   * Check ``Bind user before searching for groups``
   * Set ``Group to use as ADMIN`` to "GISADMINGROUP"
   * Set ``Group search base`` to "cn=Users"
   * Set ``Group search filter`` to "member={0}"
   
#. Test the LDAP connection by entering the username "GISADMIN" and password "secret"
   in the connection test form located on the right and click the 
   ``Test Connection`` button. 

   .. figure:: images/ad1.jpg
      :align: center

   A successful connection should be reported at the top of the page.

#. Save.
#. Back on the authentication page scroll down to the ``Provider Chain`` panel 
   and move the ``ad-ldap`` provider from ``Available`` to ``Selected``.

   .. figure:: images/ad2.jpg
      :align: center

#. Save.

Test a LDAP login
-----------------

#. Navigate to the GeoServer home page and log out of the admin account. 
#. Login as the user "GISUSER" with the with the password "secret".

   .. figure:: images/ad3.jpg
      :align: center

Logging in as GISUSER doesn't yield any administrative functionality because the GISUSER account has not been mapped to the administrator role. In the next section 
GeoServer will be configured to map groups from the LDAP database to roles. 

Now we will login with a user having administrative rights.

#. Navigate to the GeoServer home page and log out of the account. 
#. Login as the user "GISADMIN" with the with the password "secret".

Once logged in full administrative functionality should be available.

Configure the LDAP role service
------------------------------------------
An additional step permits to configure a role service to get GeoServer roles
from the LDAP repository and allow access rights to be assigned to those roles.

#. Click the ``Users,Group,Roles`` link located under the ``Security`` section 
   of the navigation sidebar.
   
#. Click the ``Add new link`` under the  ``Role Services`` section.

#. Click the ``LDAP`` option under the  ``New Role Service`` section.

   .. figure:: images/ldap14.jpg
      :align: center
      
#. Enter ``ldapadrs`` in the  ``Name`` text field.

#. Enter ``ldap://domain-controller/dc=ad,dc=local`` in the  ``Server URL`` text field.

#. Enter ``CN=Users`` in the  ``Group search base`` text field.

#. Enter ``member={1},dc=ad,dc=local`` in the  ``Group user membership search filter`` text field.

#. Enter ``objectClass=group`` in the  ``All groups search filter`` text field.

#. Enter ``sAMAccountName={0}`` in the  ``Filter used to lookup user`` text field.

Then we need to a choose a user to authenticate on the server (many LDAP server don't allow anonymous data lookup).

#. Check the ``Authenticate to extract roles`` checkbox.

#. Enter ``GISADMIN@ad.local`` in the  ``Username`` text field.

#. Enter ``secret`` in the  ``Password`` text field.

#. Save.

#. Click the ``ldapadrs`` role service item under the  ``Role Services`` section.

#. Select ``ROLE_DOMAIN ADMINS`` from the ``Administrator role`` combobox.

#. Select ``ROLE_DOMAIN ADMINS`` from the ``Group administrator role`` combobox.

#. Save again.

You should now be able to see and assign the new ActiveDirectory roles wherever an ``Available Roles`` list is shown (for example in the ``Data`` and ``Services`` rules sections.