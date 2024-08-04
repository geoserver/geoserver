/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;

/**
 * A filter interface to determine which {@link Resource resources} an authenticated user can see
 * and/or write to, based on the {@link ResourceAccessManager} ability to determine whether an
 * {@link Authentication} belongs to a workspace administrator, and if so, which workspaces it's
 * allowed to see.
 *
 * <p>This can be used, for example, to secure the {@link ResourceStore}, or to determine which REST
 * API URIs to allow access to.
 *
 * <p>For {@link ResourceAccessManager#isWorkspaceAdmin(Authentication, Catalog) workspace
 * administrators}, {@link #canRead(Authentication, String) canRead} and {@link
 * #canWrite(Authentication, String) canWrite} will tell whether it has read or read-write access to
 * a resource based on its {@link Resource#path() path}.
 *
 * <p>For those users that are a workspace administrator, the following resources will be
 * accessible, using Ant-patterns:
 *
 * <ul>
 *   <li>{@literal ""}: the root folder, read-only.
 *   <li>{@literal "styles/**"}: the root styles folder, read-only.
 *   <li>{@literal "workspaces/**"}: workspaces folder, read-only.
 *   <li>{@literal "workspaces/{workspace}/**"}: read-write access limited to workspaces the user is
 *       an admin of
 *   <li>{@literal "namespaces/**"}: the namespaces folder, read-only.
 *   <li>{@literal "namespaces/{namespace-prefix}/**"}: read-write access limited to namespace
 *       prefixes matching workspace names the user is an admin of
 * </ul>
 *
 * @implNote {@link GeoServerExtensions} is used to get a hold on the {@link Catalog} and the {@link
 *     SecureCatalogImpl}, the former is expected to filter out which workspaces the {@code
 *     Authentication} can see, the later giving access to the {@link ResourceAccessManager}.
 */
public class WorkspaceAdminResourceFilter {

    public enum ResourceAccess {
        NONE,
        READ {
            @Override
            public boolean canRead() {
                return true;
            }
        },
        WRITE {
            @Override
            public boolean canRead() {
                return true;
            }

            @Override
            public boolean canWrite() {
                return true;
            }
        };

        public boolean canRead() {
            return false;
        }

        public boolean canWrite() {
            return false;
        }
    }

    /**
     * List of root resource names to allow access to a user that is a workspace administrator.
     * Usually as derived from REST calls to {@literal /rest/resource/**}
     */
    private static final List<String> collectionsAntPatterns =
            List.of("", "workspaces", "namespaces", "styles");

    private static final List<String> workspaceAntPatterns =
            List.of(
                    "workspaces/{workspace}",
                    "workspaces/{workspace}/**",
                    // namespaces path variable called workspace to reuse the extraction logic
                    "namespaces/{workspace}",
                    "namespaces/{workspace}/**");

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * Determines the {@link ResourceAccess access level} for the authenticated user on the {@link
     * Resource} denoted by {@code path}.
     */
    public ResourceAccess getAccessLimits(Authentication authentication, String path) {
        final String workspaceName = resolveWorkspace(path);

        if (workspaceName == null) {
            // it's not a workspace-related path
            boolean readable = isAllowedCollectionPath(path) && isAnyWorkspaceAdmin(authentication);
            return readable ? ResourceAccess.READ : ResourceAccess.NONE;
        }

        WorkspaceAccessLimits wsAccessLimits =
                getWorkspaceAccessLimits(authentication, workspaceName);
        boolean adminable = wsAccessLimits != null && wsAccessLimits.isAdminable();
        return adminable ? ResourceAccess.WRITE : ResourceAccess.NONE;
    }

    @Nullable
    private WorkspaceAccessLimits getWorkspaceAccessLimits(
            Authentication authentication, final String workspaceName) {
        WorkspaceAccessLimits wsAccessLimits = null;
        Catalog catalog = catalog();
        WorkspaceInfo workspace = catalog.getWorkspaceByName(workspaceName);
        if (workspace != null) {
            ResourceAccessManager accessManager = accessManager();
            wsAccessLimits = accessManager.getAccessLimits(authentication, workspace);
        }
        return wsAccessLimits;
    }

    private String pathWithNoTrailingSlash(final String path) {
        if (path.endsWith("/")) return path.substring(0, path.length() - 1);
        return path;
    }

    private String resolveWorkspace(String resource) {
        final String path = pathWithNoTrailingSlash(resource);
        // remove query params
        return workspaceAntPatterns.stream()
                .map(pattern -> extractWorkspace(pattern, path))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String extractWorkspace(String pattern, String path) {
        if (matcher.match(pattern, path))
            return matcher.extractUriTemplateVariables(pattern, path).get("workspace");
        return null;
    }

    private boolean isAllowedCollectionPath(String path) {
        return collectionsAntPatterns.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }

    private boolean isAnyWorkspaceAdmin(Authentication authentication) {
        return accessManager().isWorkspaceAdmin(authentication, catalog());
    }

    ResourceAccessManager accessManager() {
        SecureCatalogImpl secureCatalog = GeoServerExtensions.bean(SecureCatalogImpl.class);
        return secureCatalog.getResourceAccessManager();
    }

    private Catalog catalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }
}
