# Configuring with a Generic OIDC IDP

GeoServer allows connecting to almost any OIDC IDP. In fact, the more specific (i.e. google, keycloak, and azure) providers just make it easy to configure them.

This section will describe the steps to configure against almost any OIDC IDP.

**NOTE:** see [Keycloak](keycloak.md) for a detailed example of setting up an IDP and registering it with GeoServer using the "OpenID Connect Provider Login".

## Configure your OIDC IDP

In order to configure GeoServer, you will need the following:

- Your IDP's Client ID
- Your IDP's Client Secret
- Your IDP's ``openid-configuration`` endpoint. This should be at ``<idp base URL>/.well-known/openid-configuration``

If you want roles from your IDP, then you'll also have to configure the roles to be either in the ID Token, the ``userinfo`` endpoint, or in the JWT Access Token.

Take a look at the other configuration guides ([Google](google.md), [GitHub](github.md), [Keycloak](keycloak.md), [MS Azure and Entra](azure.md), [Generic OpenID Connect](generic.md)) - these should, indirectly, help you configure your IDP Server.

## Configuring GeoServer

See [Configuration](../configuring.md) for more details.

Also, the specific providers ([Google](google.md), [GitHub](github.md), [Keycloak](keycloak.md), [MS Azure and Entra](azure.md), [Generic OpenID Connect](generic.md)) provide more details.

1.  Create a new OIDC Filter

    > - Tick the "OpenID Connect Provider Login" checkbox
    > - Use your IDP's Client ID and Client Secret
    > - Use your IDP's ``openid-configuration`` endpoint and "Discover" button to configure most of the GeoServer OIDC Options
    > - Look in the "Advanced" configuration section to see if you need any of those options
    > - Set up the [roles source](../role-config.md) for the Filter (if needed)

2.  Add your new OIDC Filter to the "web" filter Chain

3.  Save

4.  In another browser (or incognito window), try to login using your new OIDC.

## Notes

See [troubleshooting](../advanced.md#community_oidc_troubleshooting).
