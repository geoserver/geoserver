/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Provides components for workspace administration security in GeoServer.
 *
 * <p>This package contains classes that implement a fine-grained security framework for managing workspace
 * administrator access rights across both the web UI and REST API. Workspace administrators in GeoServer have limited
 * administrative privileges, focused only on the workspaces they've been granted access to, in contrast to global
 * administrators who have unrestricted access to all GeoServer functionality.
 *
 * <h2>Key Components</h2>
 *
 * <h3>Core Authorization</h3>
 *
 * <ul>
 *   <li>{@link org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer} - Central component that determines
 *       whether a user is a workspace administrator and which workspaces they can access.
 *   <li>{@link org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizationManager} - Spring Security integration
 *       for evaluating workspace admin permissions during HTTP request processing.
 * </ul>
 *
 * <h3>REST API Security</h3>
 *
 * <ul>
 *   <li>{@link org.geoserver.security.workspaceadmin.WorkspaceAdminRestfulDefinitionSource} - Provides URL
 *       pattern-based security rules for REST API endpoints.
 *   <li>{@link org.geoserver.security.workspaceadmin.WorkspaceAdminRestAccessRule} - Represents individual REST API
 *       access rules for workspace administrators.
 *   <li>{@link org.geoserver.security.workspaceadmin.WorkspaceAdminRESTAccessRuleDAO} - Manages the loading and access
 *       to workspace admin REST API security rules from the security/rest.workspaceadmin.properties file.
 * </ul>
 *
 * <h2>Security Model</h2>
 *
 * <p>The workspace administrator security model is based on several key principles:
 *
 * <ol>
 *   <li><strong>Workspace Scoping</strong> - Administrators are restricted to managing only resources within workspaces
 *       they've been granted access to.
 *   <li><strong>Consistent Authorization</strong> - The same authorization logic applies to both the web UI and REST
 *       API interfaces.
 *   <li><strong>URL Pattern Matching</strong> - REST API access is controlled through Ant-style pattern matching for
 *       HTTP requests. Endpoints that don't match any pattern fall back to the global REST security configuration,
 *       which typically restricts access to administrators only.
 *   <li><strong>Read/Write Differentiation</strong> - Rules differentiate between read operations (GET, HEAD, OPTIONS,
 *       TRACE) and write operations (POST, PUT, PATCH, DELETE).
 *   <li><strong>Extensible Rules</strong> - Access rules are loaded from the security/rest.workspaceadmin.properties
 *       file and can be extended or customized. When the file doesn't exist, it's initialized from a template.
 * </ol>
 *
 * <h2>Integration Points</h2>
 *
 * <p>This package integrates with several other parts of GeoServer:
 *
 * <ul>
 *   <li>Spring Security Framework - For HTTP request authorization
 *   <li>GeoServer Web UI - For workspace-limited administrative interfaces
 *   <li>GeoServer REST API - For programmatic access to administrative functions
 *   <li>GeoServer Catalog - To determine workspace-level permissions
 *   <li>ResourceAccessManager - For extracting workspace administration privileges
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <p>To check if a user is a workspace administrator:
 *
 * <pre>
 * WorkspaceAdminAuthorizer authorizer = GeoServerExtensions.bean(WorkspaceAdminAuthorizer.class);
 * boolean isWorkspaceAdmin = authorizer.isWorkspaceAdmin(authentication);
 * </pre>
 *
 * <p>To determine if a workspace administrator can access a specific REST endpoint:
 *
 * <pre>
 * boolean canAccess = authorizer.canAccess(authentication, "/rest/workspaces/myworkspace", HttpMethod.GET);
 * </pre>
 *
 * @see org.geoserver.security.ResourceAccessManager
 * @see org.geoserver.security.GeoServerSecurityManager
 * @see org.springframework.security.authorization.AuthorizationManager
 */
package org.geoserver.security.workspaceadmin;
