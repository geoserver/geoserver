.. _security_rest_workspace_admin:

REST Workspace Administrator Security
=====================================

GeoServer supports fine-grained access control for workspace administrators when using the REST API. This functionality complements the existing workspace administration capabilities available in the web interface, ensuring that workspace administrators have consistent permissions across both the web UI and programmatic REST API access.

How It Works
------------

The workspace administrator REST API security follows these principles:

1. Workspace administrators can only access resources within their assigned workspaces
2. Access rules are defined using Ant-style pattern matching for REST API URL paths
3. Different HTTP methods (GET, POST, PUT, DELETE) can be controlled separately to allow read or write access
4. Endpoints that don't match any pattern fall back to the global REST security configuration (typically restricted to administrators)
5. Default rules are provided but can be extended with custom rules

Default Access Rules
--------------------

The default workspace administrator access rules are defined in a template that is used to initialize the ``rest.workspaceadmin.properties`` file. By default, workspace administrators are granted the following access through the REST API:

**Workspace and Catalog Endpoints:**

* ``/rest/workspaces.{ext}=r`` - Read-only access to the workspaces listing (JSON/XML)
* ``/rest/workspaces=r`` - Read-only access to the workspaces listing
* ``/rest/workspaces/{workspace}.{ext}=r,PUT`` - Read and update access to specific workspace (JSON/XML), but cannot rename the workspace
* ``/rest/workspaces/{workspace}=r,PUT`` - Read and update access to specific workspace, but cannot rename the workspace
* ``/rest/workspaces/{workspace}/**=rw`` - Full access to all resources under the workspace
* ``/rest/namespaces.{ext}=r`` - Read-only access to namespaces listing (JSON/XML)
* ``/rest/namespaces=r`` - Read-only access to namespaces listing
* ``/rest/namespaces/{namespace}.{ext}=r,PUT`` - Read and update access to specific namespace (JSON/XML), but cannot rename the namespace
* ``/rest/namespaces/{namespace}=r,PUT`` - Read and update access to specific namespace, but cannot rename the namespace
* ``/rest/namespaces/{namespace}/**=rw`` - Full access to all resources under the namespace
* ``/rest/layers/**=rw`` - Full access to layers (filtered by secure catalog to only show layers in managed workspaces)
* ``/rest/styles.{ext}=r`` - Read-only access to global styles listing (JSON/XML)
* ``/rest/styles/**=r`` - Read-only access to all global styles (for use in layers within the workspace)
* ``/rest/templates.{ext}=r`` - Read-only access to global templates listing (JSON/XML)
* ``/rest/templates/**=r`` - Read-only access to all global templates (for reference when creating workspace-specific templates)

.. note::
   Workspace-specific resources are accessed through the workspace endpoint, for example:
   ``/rest/workspaces/{workspace}/styles/**``, ``/rest/workspaces/{workspace}/templates/**``, 
   and ``/rest/workspaces/{workspace}/layergroups/**``. Workspace administrators have full access (read/write) 
   to styles, templates, and layer groups within their assigned workspaces, but only read access to global 
   styles and templates. Workspace administrators have no access to global layer groups. Layers and layer groups 
   in workspace-administered workspaces can reference global styles.

**Resource Access:**

* ``/rest/resource/workspaces=r`` - Read-only access to workspace resources listing
* ``/rest/resource/workspaces/{workspace}/**=rw`` - Full access to resources within workspace
* ``/rest/resource/**=r`` - Read-only access to other resources

**User Account Management:**

* ``/rest/security/self/**=rw`` - Full access to self-service account management

**General Service Endpoints:**

* ``/rest/fonts.{ext}=r`` - Read-only access to fonts listing (JSON/XML)
* ``/rest/fonts/**=r`` - Read-only access to fonts
* ``/rest=r`` - Read-only access to REST API root
* ``/rest/=r`` - Read-only access to REST API root (alternate)
* ``/rest.{ext}=r`` - Read-only access to REST API root (JSON/XML)
* ``/rest/index=r`` - Read-only access to REST API index
* ``/rest/index.{ext}=r`` - Read-only access to REST API index (JSON/XML)

Configuration
-------------

The workspace administrator REST access rules are defined in the ``rest.workspaceadmin.properties`` file located in the ``security`` directory of the GeoServer data directory. When GeoServer is first started or the file doesn't exist, it's automatically created from a template containing the default workspace administrator access rules.

Custom rules can be added to this file to extend or modify the default permissions using the syntax::

  /url/pattern=METHOD1,METHOD2,...

Where methods can be specific HTTP methods (GET, POST, PUT, DELETE) or shorthand values:

* ``r`` = Read operations (GET, HEAD, OPTIONS, TRACE)
* ``w`` = Write operations (POST, PUT, PATCH, DELETE)
* ``rw`` = All operations (read + write)

For example, to allow workspace administrators to access styling information across all workspaces (read-only)::

  /rest/styles/**=r

Filesystem Sandboxing
----------------------

Workspace administrators are also subject to filesystem sandboxing when accessing resources through the REST API. This ensures that they can only access files within their assigned workspace directories.

When a filesystem sandbox is configured as described in :ref:`security_sandbox`, workspace administrators will be restricted to the subdirectory matching their workspace name within the sandbox.

Examples
--------

The following examples demonstrate typical REST API operations available to workspace administrators:

**Accessing workspace information**::

  GET /rest/workspaces/myworkspace

**Adding a new datastore to their workspace**::

  POST /rest/workspaces/myworkspace/datastores

**Modifying layer settings within their workspace**::

  PUT /rest/workspaces/myworkspace/layers/mylayer

**Creating a workspace-specific style**::

  POST /rest/workspaces/myworkspace/styles

**Reading a global style (read-only access)**::

  GET /rest/styles/default_point

**Accessing workspace resources**::

  GET /rest/resource/workspaces/myworkspace/styles/mystyle.sld

Security Considerations
-----------------------

1. Workspace administrators cannot access resources outside of their assigned workspaces
2. Workspace administrators cannot rename workspaces they administer (can update other properties)
3. Workspace administrators cannot change the default workspace or namespace settings
4. Workspace administrators have read-only access to global styles (but can create/modify workspace-specific styles)
5. REST endpoints not matching any patterns fall back to global REST security rules (typically administrators-only)
6. URL pattern matching is case-sensitive by default
7. Nested paths under workspace resources are automatically secured
8. Appropriate HTTP status codes (401/403) are returned for unauthorized access attempts

When troubleshooting access issues, check:

1. The user has the ROLE_WORKSPACE_ADMIN role assigned
2. The user has the correct workspace(s) assigned
3. The requested resource falls under the permitted URL patterns
4. The HTTP method being used is allowed for that resource pattern
5. For PUT operations on workspaces or namespaces, ensure the name field is not being modified
