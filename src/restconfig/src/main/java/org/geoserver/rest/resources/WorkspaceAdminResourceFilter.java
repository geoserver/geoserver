/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;

/**
 * A filter interface to determine which {@link Resource resources} an authenticated user can see and/or write to, based
 * on the {@link WorkspaceAdminAuthorizer} ability to determine whether an {@link Authentication} belongs to a workspace
 * administrator, and if so, which workspaces it's allowed to see.
 *
 * <p>This can be used, for example, to secure the {@link ResourceStore}, or to determine which REST API URIs to allow
 * access to.
 *
 * <p>For {@link WorkspaceAdminAuthorizer#isWorkspaceAdmin(Authentication) workspace administrators},
 * {@link #canRead(Authentication, String) canRead} and {@link #canWrite(Authentication, String) canWrite} will tell
 * whether it has read or read-write access to a resource based on its {@link Resource#path() path}.
 *
 * <p>For workspace administrator users, the following resources will be accessible, expressed as Ant-patterns:
 *
 * <ul>
 *   <li>{@literal ""}: the root folder, read-only.
 *   <li>{@literal "workspaces/**"}: workspaces folder, read-only.
 *   <li>{@literal "workspaces/{workspace}/**"}: read-write access limited to workspaces the user is an admin of
 * </ul>
 *
 * @see WorkspaceAdminAuthorizer
 */
class WorkspaceAdminResourceFilter {

    /** Represents the access level to a resource. */
    public enum ResourceAccess {
        /** No access to the resource */
        NONE(false, false),
        /** Read-only access to the resource */
        READ(true, false),
        /** Read-write access to the resource */
        WRITE(true, true);

        private final boolean canRead;
        private final boolean canWrite;

        /**
         * Creates a new access level with the specified read and write permissions.
         *
         * @param r whether reading is permitted
         * @param w whether writing is permitted
         */
        private ResourceAccess(boolean r, boolean w) {
            this.canRead = r;
            this.canWrite = w;
        }

        /**
         * Checks if reading is permitted for this access level.
         *
         * @return true if reading is permitted, false otherwise
         */
        public boolean canRead() {
            return canRead;
        }

        /**
         * Checks if writing is permitted for this access level.
         *
         * @return true if writing is permitted, false otherwise
         */
        public boolean canWrite() {
            return canWrite;
        }
    }

    /**
     * List of root resource names to allow access to a user that is a workspace administrator. Usually as derived from
     * REST calls to {@literal /rest/resource/**}
     */
    private static final List<String> collectionsAntPatterns = List.of("", "workspaces");

    private static final List<String> workspaceAntPatterns =
            List.of("workspaces/{workspace}", "workspaces/{workspace}/**");

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final WorkspaceAdminAuthorizer authorizer;

    /**
     * Creates a new resource filter with the given workspace admin authorizer.
     *
     * @param authorizer the authorizer to use for checking workspace admin permissions
     * @throws NullPointerException if authorizer is null
     */
    public WorkspaceAdminResourceFilter(WorkspaceAdminAuthorizer authorizer) {
        this.authorizer = Objects.requireNonNull(authorizer);
    }

    /**
     * Determines the {@link ResourceAccess access level} for the authenticated user on the {@link Resource} denoted by
     * {@code path}.
     *
     * @param authentication the authentication to check permissions for
     * @param path the resource path to check access for
     * @return the access level (NONE, READ, or WRITE) for the given user and resource
     */
    public ResourceAccess getAccessLimits(Authentication authentication, String path) {

        return extractWorkspace(path)
                .map(workspace -> workspaceResourceAccess(authentication, workspace))
                .orElseGet(() -> noWorkspaceResourceAccess(authentication, path));
    }

    /**
     * Determines access for resources that are not within a specific workspace. These are typically collection
     * resources like the root or workspaces directory.
     *
     * @param authentication the authentication to check permissions for
     * @param path the resource path to check access for
     * @return READ access if the path is an allowed collection and the user is a workspace admin, NONE otherwise
     */
    private ResourceAccess noWorkspaceResourceAccess(Authentication authentication, String path) {
        boolean readable = isAllowedCollectionPath(path) && authorizer.isWorkspaceAdmin(authentication);
        return readable ? ResourceAccess.READ : ResourceAccess.NONE;
    }

    /**
     * Determines access for resources within a specific workspace.
     *
     * @param authentication the authentication to check permissions for
     * @param workspace the workspace name
     * @return WRITE access if the user has admin rights to the workspace, NONE otherwise
     */
    private ResourceAccess workspaceResourceAccess(Authentication authentication, final String workspace) {

        WorkspaceAccessLimits wsAccessLimits = authorizer.getWorkspaceAccessLimits(authentication, workspace);
        boolean adminable = wsAccessLimits != null && wsAccessLimits.isAdminable();
        return adminable ? ResourceAccess.WRITE : ResourceAccess.NONE;
    }

    /**
     * Removes a trailing slash from a path if present.
     *
     * @param path the path to normalize
     * @return the path without a trailing slash
     */
    private String pathWithNoTrailingSlash(final String path) {
        if (path.endsWith("/")) return path.substring(0, path.length() - 1);
        return path;
    }

    /**
     * Extracts the workspace name from a resource path if it matches any workspace pattern.
     *
     * @param resource the resource path to check
     * @return an Optional containing the workspace name if found, empty otherwise
     */
    private Optional<String> extractWorkspace(String resource) {
        final String path = pathWithNoTrailingSlash(resource);
        return workspaceAntPatterns.stream()
                .map(pattern -> extractWorkspace(pattern, path))
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * Extracts the workspace name from a path using an Ant pattern.
     *
     * @param pattern the Ant pattern with a {workspace} variable
     * @param path the path to extract from
     * @return the workspace name if the path matches the pattern, null otherwise
     */
    private String extractWorkspace(String pattern, String path) {
        if (matcher.match(pattern, path))
            return matcher.extractUriTemplateVariables(pattern, path).get("workspace");
        return null;
    }

    /**
     * Checks if the path is one of the allowed collection paths that workspace admins can access.
     *
     * @param path the path to check
     * @return true if the path matches an allowed collection pattern, false otherwise
     */
    private boolean isAllowedCollectionPath(String path) {
        return collectionsAntPatterns.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }
}
