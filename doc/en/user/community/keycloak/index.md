# Keycloak Role Service

The `keycloak` module provides a [GeoServer role service](../../security/usergrouprole/roleservices.md) backed by the [Keycloak](https://www.keycloak.org/) Admin REST API.

Unlike token-based role extraction (where roles are read from the JWT claims of an incoming request), this module queries Keycloak directly. This means:

- The full list of realm and client roles is visible in GeoServer's **Security → Users, Groups and Roles** administration pages.
- Roles can be assigned to layers and services using GeoServer's standard role-based access control, without requiring every user to log in first.
- It can be combined with the [OAuth2/OIDC login module](../oidc/index.md) — the OIDC filter handles authentication while this service supplies the authoritative role list.

!!! note
    This module only provides a **role service**. It does not add a Keycloak login button or authentication filter. For browser-based SSO login via Keycloak, install the [OAuth2/OIDC module](../oidc/index.md) alongside this one.

<div class="grid cards" markdown>

- [Installing the Keycloak Role Service module](installing.md)
- [Configuring the Keycloak Role Service](configuration.md)

</div>
