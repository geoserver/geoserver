/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;

/**
 * Maps {@link Resource#path() resource paths} to the {@link ResourceAccess access level} a workspace administrator has
 * on them, on behalf of {@link SecureResourceStore}.
 *
 * <p>Workspace administrators get read-write access to {@literal workspaces/{workspace}/**} for the workspaces they
 * administer, read-only access to the root folder, the {@literal workspaces} listing, and the global {@literal styles}
 * folder, and no access to anything else.
 *
 * @see WorkspaceAdminAuthorizer
 */
class WorkspaceAdminResourceFilter {

    /** Access level to a resource: {@code NONE}, {@code READ} (read-only), or {@code WRITE} (read-write). */
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

    /** Paths outside any workspace that workspace administrators can read. */
    private static final List<String> collectionsAntPatterns = List.of("", "styles", "styles/**", "workspaces");

    private static final List<String> workspaceAntPatterns =
            List.of("workspaces/{workspace}", "workspaces/{workspace}/**");

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final WorkspaceAdminAuthorizer authorizer;

    public WorkspaceAdminResourceFilter(WorkspaceAdminAuthorizer authorizer) {
        this.authorizer = Objects.requireNonNull(authorizer);
    }

    /** Determines the access level for the authenticated user on the resource denoted by {@code path}. */
    public ResourceAccess getAccessLimits(Authentication authentication, String path) {

        return extractWorkspace(path)
                .map(workspace -> workspaceResourceAccess(authentication, workspace))
                .orElseGet(() -> noWorkspaceResourceAccess(authentication, path));
    }

    private ResourceAccess noWorkspaceResourceAccess(Authentication authentication, String path) {
        boolean readable = isAllowedCollectionPath(path) && authorizer.isWorkspaceAdmin(authentication);
        return readable ? ResourceAccess.READ : ResourceAccess.NONE;
    }

    private ResourceAccess workspaceResourceAccess(Authentication authentication, final String workspace) {

        WorkspaceAccessLimits wsAccessLimits = authorizer.getWorkspaceAccessLimits(authentication, workspace);
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
