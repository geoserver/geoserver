# REST workspace admin security {: #security_rest_workspace_admin }

The `rest.workspaceadmin.properties` file controls which REST API endpoints workspace administrators can access. These rules determine URL-level access only: they do not control which workspaces or resources are visible, which is determined by the data security rules in `layers.properties` (see [Workspace Administration](index.md) for the full picture).

The file lives in the `security` directory of the GeoServer data directory, and is created from a built-in template on first startup.

## Syntax

Each rule maps an Ant-style URL pattern to the HTTP methods workspace administrators may use on it:

    /url/pattern=METHOD1,METHOD2,...

Methods can be explicit HTTP method names (GET, POST, PUT, ...) or one of the shorthands:

- `r` = Read operations (GET, HEAD, OPTIONS, TRACE)
- `w` = Write operations (POST, PUT, PATCH, DELETE)
- `rw` = All operations (read + write)

Rules are evaluated in order and the first match wins, so place more specific patterns before more general ones. Requests that match no rule fall back to [rest.properties](../rest.md), which by default restricts access to full administrators.

## Default access rules

The default rules give workspace administrators read-write access to the contents of their workspaces, and read-only access to the global resources needed to work with them:

```properties
# Workspace and catalog endpoints. GeoServer filters the actual contents
# based on the user's manageable workspaces; these rules only open the endpoints.
/rest/workspaces.{ext}=r
/rest/workspaces=r

# read and update; a workspace rename is rejected
/rest/workspaces/{workspace}.{ext}=r,PUT
/rest/workspaces/{workspace}=r,PUT

# full access to sub-resources (datastores, coveragestores, styles, layergroups, etc.)
/rest/workspaces/{workspace}/**=rw

# namespaces mirror the workspace rules
/rest/namespaces.{ext}=r
/rest/namespaces=r
/rest/namespaces/{namespace}.{ext}=r,PUT
/rest/namespaces/{namespace}=r,PUT
/rest/namespaces/{namespace}/**=rw

# layers, filtered to manageable workspaces
/rest/layers/**=rw

# read-only global styles and templates
# (workspace ones are under /rest/workspaces/{workspace}/**)
/rest/styles.{ext}=r
/rest/styles/**=r
/rest/templates.{ext}=r
/rest/templates/**=r

# resource browser, contents filtered to manageable workspaces
/rest/resource/workspaces=r
/rest/resource/workspaces/{workspace}/**=rw
/rest/resource/styles=r
/rest/resource/styles/**=r
/rest/resource/**=r

# self-service account management
/rest/security/self/**=rw

# per-workspace OWS service settings
# (global settings, /rest/services/*/settings, fall through to rest.properties)
/rest/services/*/workspaces/{workspace}/**=rw

# read-only API root, index, fonts and CRS lookups
/rest/fonts.{ext}=r
/rest/fonts/**=r
/rest/crs=r
/rest/crs/**=r
/rest=r
/rest/=r
/rest.{ext}=r
/rest/index=r
/rest/index.{ext}=r
```

With these rules, workspace administrators:

- can update workspaces and namespaces they administer, but cannot rename them
- cannot change the default workspace or namespace
- have read-only access to global styles and templates, and no access to global layer groups
- can manage per-workspace service settings (WMS, WFS, etc.), but not the global ones

## Customization

Edit `security/rest.workspaceadmin.properties` in the data directory to extend or restrict the default permissions. For example, to also give workspace administrators read access to the GeoServer logging configuration:

    /rest/logging=GET

## Filesystem sandboxing

When a [filesystem sandbox](../sandbox.md) is configured, workspace administrators accessing `/rest/resource` are restricted to the subdirectory matching their workspace name within the sandbox.

## Troubleshooting

When a workspace administrator gets an unexpected `403` or `404` response, check that:

1. The user's role is granted admin access (`a`) to the workspace by the data security rules in `layers.properties`
2. The request URL matches one of the patterns, with an allowed HTTP method
3. The resource belongs to a workspace the user administers; resources in other workspaces are hidden and return `404`
4. For PUT operations on a workspace or namespace, the name is not being modified

For a hands-on walkthrough, see the [Workspace Administration tutorial](../tutorials/workspaceadmin/index.md).
