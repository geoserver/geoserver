# OAuth2 OpenID Connect

The `oidc` module is a security module that accepts users (and roles) from external [OIDC](https://openid.net/developers/how-connect-works/) identity providers. This allows GeoServer to be compatible with an organization's [Single Sign On](https://en.wikipedia.org/wiki/Single_sign-on).

<div class="grid cards" markdown>

- [Installing the OAUTH2/OIDC module](installing.md)
- [OAUTH2/OIDC configuration](configuring.md)
- [Configure the Google authentication provider](oauth2/google.md)
- [Configure the GitHub authentication provider](oauth2/github.md)
- [Configure the Microsoft Azure authentication provider](oauth2/azure.md)
- [Configuring with Keycloak](oauth2/keycloak.md)
- [Configuring with a Generic OIDC IDP](oauth2/generic.md)
- [Configuring the roles source](role-config.md)
- [Advanced Information](advanced.md)

</div>

!!! note
    The OAuth2 OpenID Connect modules is marked pending, meaning you'll find them in the releases, but are not yet officially supported extensions. In order for this module to graduate to supported extensions, we need more installations, and public experience migrating from the previous keycloak and oauth2 modules it replaces. Please contact <alessio.fabiani@geo-solutions.it> with your experience and feedback, and the [user forum](https://discourse.osgeo.org/c/geoserver/user/51) with enthusiasm and success.
