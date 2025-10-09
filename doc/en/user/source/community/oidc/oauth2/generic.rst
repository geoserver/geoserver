.. _community_oidc_generic:


Configuring with a Generic OIDC IDP
===================================

GeoServer allows connecting to almost any OIDC IDP. In fact, the more specific (i.e. google, keycloak, and azure) providers just make it easy to configure them.  

This section will describe the steps to configure against almost any OIDC IDP.

**NOTE:** see :ref:`Keycloak <community_oidc_keycloak>` for a detailed example of setting up an IDP and registering it with GeoServer using the "OpenID Connect Provider Login".


Configure your OIDC IDP
-----------------------

In order to configure GeoServer, you will need the following:

* Your IDP's Client ID
* Your IDP's Client Secret
* Your IDP's `openid-configuration` endpoint.  This should be at `<idp base URL>/.well-known/openid-configuration`

If you want roles from your IDP, then you'll also have to configure the roles to be either in the ID Token, the `userinfo` endpoint, or in the JWT Access Token. 

Take a look at the other configuration guides (:ref:`Google <community_oidc_google>`, :ref:`GitHub <community_oidc_github>`, :ref:`Keycloak <community_oidc_keycloak>`, :ref:`MS Azure and Entra <community_oidc_azure>`, :ref:`Generic OpenID Connect <community_oidc_generic>`) - these should, indirectly, help you configure your IDP Server.   

Configuring GeoServer
---------------------

See :ref:`Configuration <community_oidc_config>` for more details.  

Also, the specific providers (:ref:`Google <community_oidc_google>`, :ref:`GitHub <community_oidc_github>`, :ref:`Keycloak <community_oidc_keycloak>`, :ref:`MS Azure and Entra <community_oidc_azure>`, :ref:`Generic OpenID Connect <community_oidc_generic>`) provide more details.

#. Create a new OIDC Filter

    * Tick the "OpenID Connect Provider Login" checkbox
    * Use your IDP's Client ID and Client Secret
    * Use your IDP's `openid-configuration` endpoint and "Discover" button to configure most of the GeoServer OIDC Options
    * Look in the "Advanced" configuration section to see if you need any of those options
    * Set up the :ref:`roles source <community_oidc_role_source>` for the Filter (if needed)

#. Add your new OIDC Filter to the "web" filter Chain
#. Save
#. In another browser (or incognito window), try to login using your new OIDC. 

Notes
-----

See :ref:`troubleshooting <community_oidc_troubleshooting>`.