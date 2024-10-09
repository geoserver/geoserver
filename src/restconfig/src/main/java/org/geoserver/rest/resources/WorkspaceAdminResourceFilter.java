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
import org.geoserver.security.WorkspaceAdminAuthorizer;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;

/**
 * A filter interface to determine which {@link Resource resources} an authenticated user can see
 * and/or write to, based on the {@link WorkspaceAdminAuthorizer} ability to determine whether an
 * {@link Authentication} belongs to a workspace administrator, and if so, which workspaces it's
 * allowed to see.
 *
 * <p>This can be used, for example, to secure the {@link ResourceStore}, or to determine which REST
 * API URIs to allow access to.
 *
 * <p>For {@link WorkspaceAdminAuthorizer#isWorkspaceAdmin(Authentication) workspace
 * administrators}, {@link #canRead(Authentication, String) canRead} and {@link
 * #canWrite(Authentication, String) canWrite} will tell whether it has read or read-write access to
 * a resource based on its {@link Resource#path() path}.
 *
 * <p>For workspace administrator users, the following resources will be accessible, expressed as
 * Ant-patterns:
 *
 * <ul>
 *   <li>{@literal ""}: the root folder, read-only.
 *   <li>{@literal "workspaces/**"}: workspaces folder, read-only.
 *   <li>{@literal "workspaces/{workspace}/**"}: read-write access limited to workspaces the user is
 *       an admin of
 * </ul>
 *
 * @see WorkspaceAdminAuthorizer
 */
class WorkspaceAdminResourceFilter {

    public enum ResourceAccess {
        NONE(false, false),
        READ(true, false),
        WRITE(true, true);

        private final boolean canRead;
        private final boolean canWrite;

        private ResourceAccess(boolean r, boolean w) {
            this.canRead = r;
            this.canWrite = w;
        }

        public boolean canRead() {
            return canRead;
        }

        public boolean canWrite() {
            return canWrite;
        }
    }

    /**
     * List of root resource names to allow access to a user that is a workspace administrator.
     * Usually as derived from REST calls to {@literal /rest/resource/**}
     */
    private static final List<String> collectionsAntPatterns = List.of("", "workspaces");

    private static final List<String> workspaceAntPatterns =
            List.of("workspaces/{workspace}", "workspaces/{workspace}/**");

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final WorkspaceAdminAuthorizer authorizer;

    public WorkspaceAdminResourceFilter(WorkspaceAdminAuthorizer authorizer) {
        this.authorizer = Objects.requireNonNull(authorizer);
    }

    /**
     * Determines the {@link ResourceAccess access level} for the authenticated user on the {@link
     * Resource} denoted by {@code path}.
     */
    public ResourceAccess getAccessLimits(Authentication authentication, String path) {

        return extractWorkspace(path)
                .map(workspace -> workspaceResourceAccess(authentication, workspace))
                .orElseGet(() -> noWorkspaceResourceAccess(authentication, path));
    }

    private ResourceAccess noWorkspaceResourceAccess(Authentication authentication, String path) {
        boolean readable =
                isAllowedCollectionPath(path) && authorizer.isWorkspaceAdmin(authentication);
        return readable ? ResourceAccess.READ : ResourceAccess.NONE;
    }

    private ResourceAccess workspaceResourceAccess(
            Authentication authentication, final String workspace) {

        WorkspaceAccessLimits wsAccessLimits =
                authorizer.getWorkspaceAccessLimits(authentication, workspace);
        boolean adminable = wsAccessLimits != null && wsAccessLimits.isAdminable();
        return adminable ? ResourceAccess.WRITE : ResourceAccess.NONE;
    }

    private String pathWithNoTrailingSlash(final String path) {
        if (path.endsWith("/")) return path.substring(0, path.length() - 1);
        return path;
    }

    private Optional<String> extractWorkspace(String resource) {
        final String path = pathWithNoTrailingSlash(resource);
        return workspaceAntPatterns.stream()
                .map(pattern -> extractWorkspace(pattern, path))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private String extractWorkspace(String pattern, String path) {
        if (matcher.match(pattern, path))
            return matcher.extractUriTemplateVariables(pattern, path).get("workspace");
        return null;
    }

    private boolean isAllowedCollectionPath(String path) {
        return collectionsAntPatterns.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }
}
