# Workspace Administration {: #security_workspace_admin }

A workspace administrator is a user with limited administrative privileges scoped to one or more specific workspaces. Workspace administrators can manage data stores, layers, styles, layer groups, templates, and service configuration within their assigned workspaces, but cannot modify global settings, access other workspaces, or change system configuration.

This model supports multi-tenant environments where different teams or organizations manage their own data independently, without requiring full GeoServer administrator access.

## How workspace administration works

Workspace administration is controlled by three configuration layers that work together:

### Data security rules (`layers.properties`)

The foundation of workspace administration. These rules define **who** is a workspace administrator by granting the admin access mode (`a`) on a workspace to a role.

For example, to make `ROLE_NE_ADMIN` a workspace admin for the `ne` workspace:

    ne.*.a=ROLE_NE_ADMIN

This can also be configured through the web interface under **Security > Data**. See [Layer security](../layer.md) for the full syntax and [Data security settings](../webadmin/data.md) for the web interface.

GeoServer's catalog security layer uses these rules to determine which workspaces a user can *manage*. In the admin interfaces (web UI and REST API), a workspace administrator will only see workspaces, layers, stores, and styles within their assigned workspaces — other workspaces are hidden entirely from management views.

!!! note

    Admin access is independent of service access. A workspace administrator for `ne` can only
    manage the `ne` workspace, but may still be able to *view* layers from other workspaces via
    OWS services (WMS, WFS, etc.) depending on the read/write rules (`r`, `w`) in `layers.properties`.
    Conversely, a user may have read access to all layers but no admin access to any workspace.

### Global REST security rules (`rest.properties`)

The [REST security](../rest.md) configuration maps URL patterns and HTTP methods to **roles**. By default, all REST API endpoints are restricted to users with `ROLE_ADMINISTRATOR`:

    /**;GET,HEAD=ROLE_ADMINISTRATOR
    /**;POST,DELETE,PUT=ROLE_ADMINISTRATOR

While `rest.properties` could be edited to grant any user role access to specific endpoints, it has no concept of workspace scoping — any role granted access gets it for all workspaces equally. It is also not aware of which users are workspace administrators.

### Workspace admin REST access rules (`rest.workspaceadmin.properties`)

This file complements `rest.properties` by defining **which REST API endpoints** workspace administrators can reach, without requiring changes to `rest.properties`. It uses Ant-style URL patterns to open specific endpoints for any user that is a workspace administrator (as determined by `layers.properties`).

Both files are evaluated together during request authorization. A workspace admin request is granted if it matches a `rest.workspaceadmin.properties` rule, even if `rest.properties` would otherwise restrict it to `ROLE_ADMINISTRATOR`.

Critically, this file only controls URL-level access — it does **not** determine which workspaces or resources are visible. GeoServer handles that based on the admin access rules from `layers.properties`.

In other words: `rest.workspaceadmin.properties` opens the door to endpoints, `layers.properties` determines what's behind the door.

The file is created automatically from a built-in template on first startup. Custom rules can be added to extend or restrict the default permissions. See [REST workspace admin security](rest.md) for the full reference.

## Web interface

When a workspace administrator logs into the GeoServer web interface, they see a restricted version of the admin console:

- Only their administrable workspaces appear in workspace listings
- Layer, store, and style listings are filtered to their workspaces
- Global configuration pages (Settings, Security, etc.) are not accessible
- Workspace-specific settings and service configuration can be managed

## REST API

Workspace administrators can access the REST API with the same scope as the web interface. The default access rules provide:

- **Read-only** access to workspace and namespace listings, global styles, global templates, fonts, and the REST API index
- **Read/write** access to all resources within their workspaces (datastores, layers, styles, layer groups, templates)
- **Read/write** access to per-workspace service settings (WMS, WFS, etc.)
- **No access** to admin-only endpoints (global settings, security configuration, etc.)

Requests to endpoints outside the configured patterns fall through to `rest.properties`, which typically restricts access to full administrators only.

See [REST workspace admin security](rest.md) for the full access rules reference.

## Filesystem sandboxing

Workspace administrators can be further restricted with [filesystem sandboxing](../sandbox.md), which limits file system access to `<sandbox>/<workspace>`. This ensures workspace administrators can only access files within their assigned workspace directories, both through the web interface and the REST API resource browser.

## See also

- [REST workspace admin security](rest.md) -- access rules reference
- [Layer security](../layer.md) -- admin access mode
- [Filesystem sandboxing](../sandbox.md) -- file system restrictions
- [Workspace admin guidance](../../production/config.md#production_config_workspace_admin) -- production recommendations
- [Tutorial: Workspace administration with the REST API](../tutorials/workspaceadmin/index.md) -- hands-on setup guide
