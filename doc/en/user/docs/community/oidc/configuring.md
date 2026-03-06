# OAUTH2/OIDC configuration

The basic steps are:

1.  Configure your IDP
2.  In GeoServer, add the OIDC filter (configured for your IDP)
3.  In GeoServer, configure the "roles source" (if needed)
4.  In GeoServer, add your OIDC filter to the "web" filter Chain

For more details, here are detailed examples for different OIDC server types:

> - [Google](oauth2/google.md)
> - [GitHub](oauth2/github.md)
> - [Keycloak](oauth2/keycloak.md)
> - [MS Azure and Entra](oauth2/azure.md)
> - [Generic OpenID Connect](oauth2/generic.md)
