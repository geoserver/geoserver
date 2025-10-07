.. _community_oidc_config:

OAUTH2/OIDC configuration
=========================

#. Start GeoServer and login to the web admin interface as the ``admin`` user.
#. Click the ``Authentication`` link located under the ``Security`` section of
   the navigation sidebar.

   .. figure:: img/filter1.jpg
      :align: center

#. Scroll down to the ``Authentication Filters`` panel and click the ``Add new`` link.

   .. figure:: img/filter2.jpg
      :align: center

#. Click the ``OpenID Connect Login`` link.

   .. figure:: img/filter3.png
      :align: center

#. Select the IDP service that you want to use a login source with the checkbox.  You can only choose one.  Using `OpenID Connect Provider Login` is recommened. 

   .. figure:: img/logins.png
      :align: center

#. Fill in the information for your IDP

   * :ref:`Google <community_oidc_google>`
   * :ref:`GitHub <community_oidc_github>`
   * :ref:`Generic OpenID Connect recommended <community_oidc_oidc>` (also see :ref:`Keycloak <community_oidc_keycloak>`_)

#. Configure the :ref:`role source <community_oidc_role_source>` (access)

#. Configure the :ref:`Filter Chains <community_oidc_filter>`