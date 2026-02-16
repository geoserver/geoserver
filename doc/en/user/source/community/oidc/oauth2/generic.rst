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

Take a look at the other configuration guides (:ref:`Google <community_oidc_google>`, :ref:`GitHub <community_oidc_github>`, :ref:`Keycloak <community_oidc_keycloak>`, :ref:`MS Azure and Entra <community_oidc_azure>`) - these should, indirectly, help you configure your IDP Server.


Registering GeoServer with your IDP
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When registering GeoServer as a client or application with your IDP, you will need to provide:

* **Redirect URI (callback URL):** This is the read-only :guilabel:`Redirect URI` shown in the GeoServer filter configuration form. It has the form::

     https://<your-geoserver>/geoserver/web/login/oauth2/code/oidc

  Copy this value exactly into your IDP's "Valid redirect URIs" or "Callback URL" setting.

* **Post-Logout Redirect URI:** If your IDP supports single logout, also register the :guilabel:`After-Logout Redirect URI` shown in the GeoServer configuration. See :ref:`Logout Behavior <community_oidc_logout_behavior>`.

.. tip::

   Create the GeoServer OIDC filter first (even with placeholder values), so that the
   computed Redirect URI is visible. Then copy it into your IDP's configuration.


Configuring GeoServer
---------------------

See :ref:`Configuration <community_oidc_config>` for details about the common login settings (Redirect Base URI, logout behavior, etc.).

Also, the specific providers (:ref:`Google <community_oidc_google>`, :ref:`GitHub <community_oidc_github>`, :ref:`Keycloak <community_oidc_keycloak>`, :ref:`MS Azure and Entra <community_oidc_azure>`) provide more detailed walkthroughs.


Using the Discovery Document
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If your IDP exposes a ``.well-known/openid-configuration`` endpoint, this is the quickest way to configure GeoServer:

#. Create a new OIDC Filter

    * From the :guilabel:`Provider` dropdown, select :guilabel:`OpenID Connect Provider` (this is the default selection)
    * Enter your IDP's Client ID and Client Secret
    * In the :guilabel:`OpenID Discovery Document` field, type the URL to your IDP's discovery endpoint, e.g.::

         https://idp.example.com/.well-known/openid-configuration

    * Press :guilabel:`Discover` — this will download the OIDC metadata and fill in most fields automatically
    * Look in the :guilabel:`Advanced Settings` section to see if you need any of those options
    * Set up the :ref:`roles source <community_oidc_role_source>` for the Filter (if needed)

#. Add your new OIDC Filter to the "web" filter Chain
#. Save
#. In another browser (or incognito window), try to login using your new OIDC. 


.. _community_oidc_manual_config:

Manual Configuration (without Discovery)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If your OIDC provider does not expose a ``.well-known/openid-configuration`` discovery document (or it is behind a firewall), you can fill in the provider settings manually. You will need the following URIs from your IDP administrator:

.. list-table::
   :header-rows: 1
   :widths: 30 40 30

   * - Field
     - Description
     - Example
   * - Access Token URI
     - The token endpoint where GeoServer exchanges the authorization code for tokens.
     - ``https://idp.example.com/oauth2/token``
   * - User Authorization URI
     - The endpoint where users are redirected to authenticate with the IDP.
     - ``https://idp.example.com/oauth2/authorize``
   * - JSON Web Key Set URI
     - The endpoint providing the public keys used to verify token signatures.
     - ``https://idp.example.com/oauth2/jwks``
   * - User Info URI (optional)
     - The endpoint that returns user profile claims when presented with an access token.
     - ``https://idp.example.com/oauth2/userinfo``
   * - Logout URI (optional)
     - The IDP's logout endpoint for single logout support.
     - ``https://idp.example.com/oauth2/logout``
   * - Scopes
     - Space-separated list of OAuth2 scopes. At minimum, ``openid`` is required.
     - ``openid profile email``
   * - Token Introspection URI (optional)
     - Required only when using opaque (non-JWT) access tokens. The endpoint where GeoServer can validate opaque tokens.
     - ``https://idp.example.com/oauth2/introspect``

To configure manually:

#. Create a new OIDC Filter and select :guilabel:`OpenID Connect Provider` from the :guilabel:`Provider` dropdown
#. Enter your IDP's Client ID and Client Secret
#. Fill in each URI field listed above using values provided by your IDP administrator
#. Set the :guilabel:`Scopes` field (at minimum ``openid``)
#. Configure any :guilabel:`Advanced Settings` as needed (see below)
#. Set up the :ref:`roles source <community_oidc_role_source>` (if needed)
#. Add your new OIDC Filter to the "web" filter Chain
#. Save

.. tip::

   If you only have the issuer URL (e.g. ``https://idp.example.com``), try appending
   ``/.well-known/openid-configuration`` to it in your browser. Many providers support
   Discovery even if it is not prominently documented.


Common Issues with Generic Providers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**HTTPS enforcement in development**
   By default, GeoServer enforces HTTPS for the Token and Authorization URIs. If your development setup uses HTTP (e.g. a local Keycloak on ``http://localhost:7777``), uncheck :guilabel:`Force Access Token URI HTTPS Secured Protocol` and :guilabel:`Force User Authorization URI HTTPS Secured Protocol` in the :guilabel:`Advanced Settings` section.

**Scope format**
   Some providers are strict about scope formatting. The standard format is space-separated (e.g. ``openid profile email``). If authentication fails, verify that your IDP does not expect a different separator.

**Clock skew**
   Token validation can fail if the clocks on the GeoServer and IDP servers are not synchronized. Ensure both servers use NTP or a similar time synchronization mechanism.

**Redirect URI mismatch**
   The most common configuration error is a mismatch between the Redirect URI that GeoServer sends and the one registered with your IDP. Copy the exact value from the read-only :guilabel:`Redirect URI` field in the GeoServer filter form into your IDP's client configuration. See :ref:`Redirect Base URI <community_oidc_redirect_base_uri>` for how this value is calculated.

Notes
-----

See :ref:`troubleshooting <community_oidc_troubleshooting>`.
