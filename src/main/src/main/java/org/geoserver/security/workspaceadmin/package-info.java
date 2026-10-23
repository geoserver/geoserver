/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Workspace administrator access control for the REST API.
 *
 * <p>Workspace administrators are users that, without being full administrators, can manage the contents of one or more
 * workspaces, as determined by {@link org.geoserver.security.ResourceAccessManager#isWorkspaceAdmin}. This package
 * grants them through the REST API the same privileges they already have in the web UI.
 *
 * <p>{@link org.geoserver.security.workspaceadmin.WorkspaceAdminRESTAccessRuleDAO} loads Ant-style URL pattern access
 * rules from {@code security/rest.workspaceadmin.properties}, created from a bundled template on first use.
 * {@link org.geoserver.security.workspaceadmin.WorkspaceAdminRestfulDefinitionSource} matches requests against those
 * rules, and {@link org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizationManager} grants access when a rule
 * matches and {@link org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer} confirms the user is a workspace
 * administrator, abstaining otherwise. Requests matching no rule fall back to the global REST security configuration
 * ({@code rest.properties}), which typically restricts access to full administrators.
 */
package org.geoserver.security.workspaceadmin;
