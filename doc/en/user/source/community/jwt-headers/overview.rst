JWT Header Overview
-------------------

This module allows  `JSON-based  <https://en.wikipedia.org/wiki/JSON>`_ headers (for username and roles) as well as `JWT-based  <https://en.wikipedia.org/wiki/JSON_Web_Token>`_ headers (for username and roles).  It also allows for validating JWT-Based AccessTokens (i.e. via `OAUTH2  <https://en.wikipedia.org/wiki/OAuth>`_/`OpenID Connect  <https://en.wikipedia.org/wiki/OpenID#OpenID_Connect_(OIDC)>`_).


If you are using something like `Apache's mod_auth_openidc  <https://github.com/OpenIDC/mod_auth_openidc>`_, then this module will allow you to;

#. Get the username from an Apache-provided `OIDC_*` header (either as simple-strings or as a component of a JSON object).
#. Get the user's roles from an Apache-provided `OIDC_*` header (as a component of a JSON object).
#. The user's roles can also be from any of the standard GeoServer providers (i.e. User Group Service, Role Service, or Request Header).

If you are using `OAUTH2/OIDC Access Tokens  <https://www.oauth.com/oauth2-servers/access-tokens/>`_:

#. Get the username from the attached JWT Access Token (via a path into the `Access Token's JSON Claims  <https://auth0.com/docs/authenticate/login/oidc-conformant-authentication/oidc-adoption-access-tokens/>`_).
#. Get the user's roles from the JWT Access Token (via a path into the Token's JSON Claims).
#. Validate the Access Token

   * Validate its Signature
   * Validate that it hasn't expired
   * Validate the token against a token verifier URL ("userinfo_endpoint") and check that subjects match
   * Validate components of the Access Token (like `aud (audience)  <https://auth0.com/docs/secure/tokens/json-web-tokens/json-web-token-claims>`_) 
   
#. The user's roles can also be from any of the standard GeoServer providers (i.e. User Group Service, Role Service, or Request Header).
#. You can also extract roles from the JWT Access Token (via a JSON path).
