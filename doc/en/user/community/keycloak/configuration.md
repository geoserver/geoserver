# Configuring the Keycloak Role Service

The role service queries the Keycloak Admin REST API using a confidential client and its service-account credentials. Two steps are required: configure a client in Keycloak, then create the role service in GeoServer.

## Step 1 — Configure a Keycloak client

The role service authenticates to Keycloak using the OAuth2 *client credentials* grant. You can reuse an existing confidential client (for example the one used for the OIDC login filter) or create a dedicated one.

For the client, ensure:

1. **Client authentication** is enabled (Keycloak 18+: toggle *Client authentication* on; older Keycloak: set *Access Type* to **confidential**).
2. **Service accounts roles** is enabled (allows the client to call admin APIs under its own identity).
3. Under the **Service account roles** tab, assign the **`realm-admin`** role from the `realm-management` client to the service account. This grants read access to the realm's user and role data.

!!! tip
    If you only want the role service to read a subset of roles, you can use finer-grained roles such as `view-realm`, `view-users`, and `query-users` from `realm-management` instead of `realm-admin`.

Once saved, note the following from the **Credentials** tab:

- **Client ID** — the client name (e.g. `geoserver`)
- **Client secret** — the secret shown on the Credentials tab

### Client roles (optional)

By default, the role service fetches only realm-level roles. To include roles defined on specific clients, you need each client's internal **ID** (a UUID, not the client name). This can be found in the URL when viewing the client configuration page:

```
/auth/admin/master/console/#/realms/{realm}/clients/{ID}
```

Collect one ID per client whose roles you want to expose, separated by commas.

## Step 2 — Create the role service in GeoServer

1. Navigate to **Security → Users, Groups and Roles** and click **Add new Role Service**.
2. Select **Keycloak** from the list of providers.
3. Fill in the connection fields:

| Field | Description |
|---|---|
| **Name** | An identifier for this role service within GeoServer (e.g. `keycloak`) |
| **Keycloak URL** | The Keycloak server root, e.g. `http://localhost:8080`. Do not include a trailing slash. |
| **Realm name (human-readable)** | The Keycloak realm to query, e.g. `master`. Use the realm *name*, not its ID. |
| **Client ID (human-readable)** | The client ID from the Credentials tab. |
| **Client Secret** | The client secret. |
| **Comma-separated IDs of clients (UUIDs)** | *(Optional)* Internal UUIDs of Keycloak clients whose roles should be included alongside realm roles. |

4. Click **Save**. The service will connect to Keycloak and load all roles.
5. The **Administrator Role** and **Group Administrator Role** dropdowns are now populated with the roles fetched from Keycloak. Select the Keycloak roles you want mapped to GeoServer's `ADMIN` and `GROUP_ADMIN` authorities.

!!! note "Role naming convention"
    Keycloak role names used with GeoServer should begin with `ROLE_` (e.g. `ROLE_ADMIN`, `ROLE_EDITOR`). GeoServer's access control rules expect this prefix. Roles without it will still be visible but may not match standard GeoServer security expressions.

## Activating the role service

The role service can be used in two ways:

**As the active role service** — navigate to **Security → Settings** and set the **Active Role Service** to the Keycloak role service. GeoServer will look up every authenticated user's roles from Keycloak on each request.

**As a role source for the OIDC filter** — if you are using the [OAuth2/OIDC module](../../extensions/oidc/index.md) for login, you can configure the OIDC filter to use this role service as its role source. This separates authentication (handled by the OIDC filter using token claims) from authorization (looked up live from Keycloak via the Admin API).

## Keycloak 17+ URL paths

The role service appends paths directly to the **Base URL** you provide:

```
{Base URL}/admin/realms/{realm}/...
{Base URL}/realms/{realm}/protocol/openid-connect/token
```

This means the Base URL controls whether the `/auth` context path is included:

| Keycloak distribution | Example Base URL |
|---|---|
| Keycloak 17+ (Quarkus, default — no `/auth` prefix) | `http://keycloak:8080` |
| Keycloak ≤16 (WildFly) or Quarkus with `/auth` re-enabled | `http://keycloak:8080/auth` |

## Roles cache

Role lookups are cached for **60 minutes** to avoid repeated Admin API calls on every request. The cache is per role service instance and is invalidated when GeoServer is restarted or the role service is reconfigured.
