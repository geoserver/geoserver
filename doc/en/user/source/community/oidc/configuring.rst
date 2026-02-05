.. _community_oidc_config:

OAUTH2/OIDC configuration
=========================

The basic steps are:

#. Configure your IDP
#. In GeoServer, add the OIDC filter and select your provider from the :guilabel:`Provider` dropdown (configured for your IDP)
#. In GeoServer, configure the "roles source" (if needed)
#. In GeoServer, add your OIDC filter to the "web" filter Chain

For more details, here are detailed examples for different OIDC server types:

   * :ref:`Google <community_oidc_google>`
   * :ref:`GitHub <community_oidc_github>`
   * :ref:`Keycloak <community_oidc_keycloak>`
   * :ref:`MS Azure and Entra <community_oidc_azure>`
   * :ref:`Generic OpenID Connect <community_oidc_generic>`

