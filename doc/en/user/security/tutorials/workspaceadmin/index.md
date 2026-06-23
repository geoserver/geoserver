# Tutorial: Workspace Administration with the REST API

This tutorial walks through setting up a workspace administrator and exploring their REST API access using the default GeoServer data directory.

It is recommended to read the [Workspace Administration](../../workspaceadmin/index.md) section before proceeding.

## Prerequisites

- A running GeoServer instance with the default sample data (including the `ne` workspace with Natural Earth layers)
- `curl` or a similar HTTP client
- Admin access to GeoServer (default: `admin` / `geoserver`)

## Step 1: Create a role

Log in as administrator and navigate to **Security > Users, Groups, and Roles**. Select the **Roles** tab and click **Add new role**.

Create a role named `ROLE_NE_ADMIN`. This role will grant workspace administration privileges.

## Step 2: Create a user

Switch to the **Users/Groups** tab and click **Add new user**.

- Username: `neadmin`
- Password: `geo123`
- Assign the `ROLE_NE_ADMIN` role

## Step 3: Grant workspace admin access

Navigate to **Security > Data** and click **Add a new rule**.

- Workspace: `ne`
- Layer: `*`
- Access mode: `Admin`
- Grant to: `ROLE_NE_ADMIN`

This rule tells GeoServer that users with `ROLE_NE_ADMIN` are workspace administrators for the `ne` workspace. GeoServer will use this to restrict management access to only the `ne` workspace in both the web interface and the REST API.

## Step 4: Log in as the workspace admin

Log out and log back in as `neadmin`. The web interface shows a restricted view:

- Only the `ne` workspace appears in workspace listings
- Only layers, stores, and styles from the `ne` workspace are visible
- Global configuration pages are not accessible

## Step 5: Explore the REST API

Open a terminal and use `curl` to explore the REST API as the workspace admin. Use the `Accept` header for content negotiation rather than file extensions.

### REST API index

Start by requesting the REST API root. The index only shows endpoints the workspace admin is allowed to access — notice that admin-only endpoints like `settings`, `security/roles`, and `about` are absent:

    curl -u neadmin:geo123 "http://localhost:8080/geoserver/rest"

```html
<html>
<head><title> GeoServer Configuration API </title></head>
<body>
<h2>GeoServer Configuration API</h2>
<ul>
<li><a href=".../rest/crs">crs</a></li>
<li><a href=".../rest/fonts">fonts</a></li>
<li><a href=".../rest/index">index</a></li>
<li><a href=".../rest/layers">layers</a></li>
<li><a href=".../rest/namespaces">namespaces</a></li>
<li><a href=".../rest/resource">resource</a></li>
<li><a href=".../rest/security/self/password">security/self/password</a></li>
<li><a href=".../rest/services/wcs/settings">services/wcs/settings</a></li>
<li><a href=".../rest/services/wfs/settings">services/wfs/settings</a></li>
<li><a href=".../rest/services/wms/settings">services/wms/settings</a></li>
<li><a href=".../rest/services/wmts/settings">services/wmts/settings</a></li>
<li><a href=".../rest/styles">styles</a></li>
<li><a href=".../rest/templates">templates</a></li>
<li><a href=".../rest/workspaces">workspaces</a></li>
</ul>
</body>
</html>
```

### List workspaces

The workspace admin sees only the workspaces they can administer:

    curl -u neadmin:geo123 -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/workspaces"

```json
{
  "workspaces": {
    "workspace": [
      {
        "name": "ne",
        "href": "http://localhost:8080/geoserver/rest/workspaces/ne.json"
      }
    ]
  }
}
```

Compare with the full administrator who sees all 8 workspaces:

    curl -u admin:geoserver -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/workspaces"

### View workspace details

    curl -u neadmin:geo123 -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/workspaces/ne"

```json
{
  "workspace": {
    "name": "ne",
    "isolated": false,
    "dataStores": "http://localhost:8080/geoserver/rest/workspaces/ne/datastores.json",
    "coverageStores": "http://localhost:8080/geoserver/rest/workspaces/ne/coveragestores.json",
    "wmsStores": "http://localhost:8080/geoserver/rest/workspaces/ne/wmsstores.json",
    "wmtsStores": "http://localhost:8080/geoserver/rest/workspaces/ne/wmtsstores.json"
  }
}
```

### Other workspaces are hidden

Workspaces the user doesn't administer are not accessible — the server returns 404 as if they don't exist:

    curl -u neadmin:geo123 -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/workspaces/cite"

```
HTTP/1.1 404 Not Found
```

### List layers

The global `/rest/layers` endpoint returns only layers from workspaces the user administers — even though it's the global endpoint, the response is filtered:

    curl -u neadmin:geo123 -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/layers"

```json
{
  "layers": {
    "layer": [
      {"name": "ne:boundary_lines"},
      {"name": "ne:coastlines"},
      {"name": "ne:countries"},
      {"name": "ne:disputed_areas"},
      {"name": "ne:populated_places"}
    ]
  }
}
```

Only `ne` layers are returned. A full administrator would see layers from all workspaces.

Layers can also be listed through the workspace-specific endpoint:

    curl -u neadmin:geo123 -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/workspaces/ne/layers"

### List styles

The global `/rest/styles` endpoint returns all global styles (read-only). These are available so workspace administrators can reference them in their layers:

    curl -u neadmin:geo123 -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/styles"

```json
{
  "styles": {
    "style": [
      {"name": "burg"},
      {"name": "capitals"},
      {"name": "generic"},
      {"name": "line"},
      {"name": "point"},
      ...
    ]
  }
}
```

Workspace-specific styles are listed through the workspace endpoint:

    curl -u neadmin:geo123 -H "Accept: application/json" \
      "http://localhost:8080/geoserver/rest/workspaces/ne/styles"

```json
{
  "styles": {
    "style": [
      {"name": "boundary_lines"},
      {"name": "coastline"},
      {"name": "countries"},
      {"name": "populated_places"}
    ]
  }
}
```

### Global styles are read-only

While workspace administrators can read global styles, they cannot create, modify, or delete them:

    curl -u neadmin:geo123 -X POST -H "Content-Type: application/json" \
      -d '{"style":{"name":"test","filename":"test.sld"}}' \
      "http://localhost:8080/geoserver/rest/styles"

```
403 Forbidden
```

### Update workspace properties

Workspace administrators can update workspace properties (e.g. isolated mode) but cannot rename the workspace:

    curl -u neadmin:geo123 -X PUT -H "Content-Type: application/json" \
      -d '{"workspace":{"name":"ne"}}' \
      "http://localhost:8080/geoserver/rest/workspaces/ne"

```
200 OK
```

Attempting to rename the workspace is denied:

    curl -u neadmin:geo123 -X PUT -H "Content-Type: application/json" \
      -d '{"workspace":{"name":"renamed"}}' \
      "http://localhost:8080/geoserver/rest/workspaces/ne"

```
403 Forbidden
```

### Admin-only endpoints are denied

Global configuration endpoints are restricted to full administrators and return `403 Forbidden`:

    curl -u neadmin:geo123 "http://localhost:8080/geoserver/rest/settings"

```
HTTP/1.1 403 Forbidden
```

## What's next

- [Workspace Administration](../../workspaceadmin/index.md) -- conceptual overview
- [REST workspace admin security](../../workspaceadmin/rest.md) -- access rules reference
- [Filesystem sandboxing](../../sandbox.md) -- restricting file system access
