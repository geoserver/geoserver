Installing an OAuth2 Protocol
-----------------------------

This module allows GeoServer to authenticate against the `OAuth2 Protocol <https://tools.ietf.org/html/rfc6749>`_.

In order to let the module work, it is mandatory to setup and configure an ``oauth2-xxxx-extension``:

* :download_community:`sec-oauth2-google`
* :download_community:`sec-oauth2-geonode`
* :download_community:`sec-oauth2-github`
* :download_community:`sec-oauth2-openid-connect`

Each ZIP files contains the oauth2-core extension, and the  jars and the jars for the provider. 

The first one contains the necessary dependencies of the OAuth2 core module. This module contains the
GeoServer security filter, the base classes for the OAuth2 Token services and the GeoServer GUI panel.

The second one provides the OAuth2 implementation for each provider.  Since in almost all cases the only difference
between OAuth2 Providers are the endpoint URIs and the client connection information (not only the keys -
public and secret - but also the user profile representations).
In order to allow GeoServer to connect to a specific OAuth2 provider it is sufficient to install the OAuth2 Core module
plugin (and correctly configure the parameters through the GeoServer GUI - see next section for the details) and the
concrete implementation of the OAuth2 REST token template and resource details.

Currently this module is shipped with a sample extension for Google OAuth2 Provider. This is a particular case since the 
Google JWT response is not standard and therefore we had to define and inject also a ``GoogleUserAuthenticationConverter`` taking
the Google REST response against a valid ``access_token`` and converting it to an OAuth2 standard one.

Other than this the most interesting part is the implementation of the base class ``GeoServerOAuth2SecurityConfiguration``.

The latter contains the Google implementation of the ``OAuth2RestTemplate``.

In the next section we will see how to install and configure the OAuth2 security filter on GeoServer authenticating against 
Google OAuth2 Provider.