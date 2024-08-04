/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import static org.geoserver.security.rest.WorkspaceAdminResourceFilter.ResourceAccess.READ;
import static org.geoserver.security.rest.WorkspaceAdminResourceFilter.ResourceAccess.WRITE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.TRACE;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.catalog.Catalog;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.filter.GeoServerAccessDecisionVoter;
import org.geoserver.security.filter.GeoServerSecurityInterceptorFilter;
import org.geoserver.security.rest.WorkspaceAdminResourceFilter.ResourceAccess;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.util.AntPathMatcher;

/**
 * {@link AccessDecisionVoter} assisting the {@link GeoServerSecurityInterceptorFilter} in granting
 * or denying access to REST API endpoints based on the authenticated user's administrative rights
 * to workspaces.
 *
 * <p>If the authenticated user has administrative rights to any workspace, as determined by the
 * {@link ResourceAccessManager}, this URL path decision voter will permit access to the following
 * end-points:
 *
 * <ul>
 *   <li>{@literal /rest/workspaces/**}
 *   <li>{@literal /rest/namespaces/**}
 *   <li>{@literal /rest/layers/**}
 *   <li>{@literal /rest/styles/**}
 *   <li>{@literal /rest/resource/workspaces/**}.
 * </ul>
 *
 * <p>This voter assumes the authenticated user is an administrator (i.e., does not have the role
 * {@literal ROLE_ADMINISTRATOR}). Such a check is presumed to be performed before invoking this
 * component's {@link #vote} method.
 *
 * <p>The information accessible to the user will be determined by what the {@link
 * SecureCatalogImpl} allows the user to see.
 *
 * <p>Special handling for the {@code /rest/resource/workspaces/**} end-point: Since there is no
 * "secured" {@link ResourceStore} decorator (unlike {@link SecureCatalogImpl} for the {@link
 * Catalog}), this component will deny access to {@code /rest/resource/workspaces/{workspace}/**}
 * where {@literal {workspace}} is not administratively accessible.
 *
 * <p>Note that this component serves the REST API in a similar capacity to how {@code
 * org.geoserver.web.WorkspaceAdminComponentAuthorizer} serves the web UI.
 *
 * <p>TODO: if the user is a workspace admin, just allow access to /workspaces/**, layers/**,
 * SecureCatalogImpl is in charge of filtering the visible cataloginfos. But for
 * /rest/resource/workspace/** do the filtering here, as there's no "secure" {@link ResourceStore}.
 * But force a 404 instead of a 403 for consistency
 */
public class WorkspaceAdminRestAuthorizer implements GeoServerAccessDecisionVoter {

    private WorkspaceAdminResourceFilter resourcePathFilter = new WorkspaceAdminResourceFilter();

    private AntPathMatcher restPathMatcher = new AntPathMatcher();

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true; // REVISIT: should be false?
    }

    /**
     * This implementation supports any type of class, because it does not query the presented
     * secure object.
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    private static final List<String> workspaceAntPatterns =
            List.of(
                    "/rest/workspaces/{workspace}.{extension}",
                    "/rest/workspaces/{workspace}",
                    "/rest/workspaces/{workspace}/**",
                    "/rest/namespaces/{workspace}.{extension}",
                    "/rest/namespaces/{workspace}",
                    "/rest/namespaces/{workspace}/**",
                    "/rest/layers/{workspace}:{layer}",
                    "/rest/resource/workspaces/{workspace}",
                    "/rest/resource/workspaces/{workspace}/**");

    @Override
    public int vote(
            Authentication authentication,
            FilterInvocation invocation,
            Collection<ConfigAttribute> attributes) {

        final String requestUri = invocation.getRequestUrl();
        // short-circuit if not a rest call
        if (!requestUri.startsWith("/rest")) {
            return ACCESS_ABSTAIN;
        }

        final HttpMethod httpMethod = HttpMethod.resolve(invocation.getRequest().getMethod());
        boolean canAccess = canAccess(authentication, requestUri, httpMethod);
        return canAccess ? ACCESS_GRANTED : ACCESS_ABSTAIN;
    }

    public boolean canAccess(
            Authentication authentication, String requestUri, HttpMethod httpMethod) {
        final String resourcePath = adaptToResourcePath(requestUri);

        if (null != resourcePath) {
            ResourceAccess grant = resourcePathFilter.getAccessLimits(authentication, resourcePath);
            if (grant == WRITE || (grant == READ && isReadOnly(httpMethod))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private String adaptToResourcePath(String rawUri) {
        final String uri = removeTrailingSlash(rawUri);

        if (matches(
                uri,
                "/rest.{extension}",
                "/rest",
                "/rest/layers.{extension}",
                "/rest/layers",
                "/rest/resource")) {
            return "";
        } else if (matches(
                uri,
                "/rest/workspaces.{extension}",
                "/rest/workspaces",
                "/rest/resource/workspaces")) {
            return "workspaces";
        } else if (matches(uri, "/rest/namespaces.{extension}", "/rest/namespaces")) {
            return "namespaces";
        } else if (matches(uri, "/rest/styles.{extension}", "/rest/styles")
                || matches(uri, "/rest/styles/**")) {
            return "styles";
        }

        String workspace =
                workspaceAntPatterns.stream()
                        .map(pattern -> extractWorkspace(pattern, uri))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
        if (workspace == null) {
            return null;
        }
        return "workspaces/" + workspace;
    }

    private boolean isReadOnly(HttpMethod method) {
        return method == GET || method == HEAD || method == OPTIONS || method == TRACE;
    }

    private boolean matches(String uri, String... patterns) {
        for (String pattern : patterns) {
            if (restPathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private String removeTrailingSlash(final String requestUri) {
        String path = URI.create(requestUri).normalize().getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    private String extractWorkspace(String pattern, String path) {
        if (restPathMatcher.match(pattern, path))
            return restPathMatcher.extractUriTemplateVariables(pattern, path).get("workspace");
        return null;
    }
}
