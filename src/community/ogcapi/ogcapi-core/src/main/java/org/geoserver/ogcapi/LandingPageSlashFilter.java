/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.catalog.Catalog;
import org.geoserver.filters.GeoServerFilter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Hack for CITE tests, they force a trailing "/" for the landing page even if they did not specify
 * it in the request, and the landing page would not work if trailing slashes are disabled. This
 * filter removes the trailing slash from the request path.
 */
@Component
public class LandingPageSlashFilter implements GeoServerFilter, ApplicationContextAware {

    private final Catalog catalog;
    private List<APIService> services;

    public LandingPageSlashFilter(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest
                && isLandingPageWithSlash((HttpServletRequest) servletRequest)) {
            filterChain.doFilter(new SlashWrapper(servletRequest), servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean isLandingPageWithSlash(HttpServletRequest servletRequest) {
        String requestURI = servletRequest.getRequestURI();
        int contextPathLength = servletRequest.getContextPath().length();
        if (requestURI.length() <= contextPathLength) return false;
        String path = requestURI.substring(contextPathLength + 1);
        // no point checking services if it does not end with a slash anyway
        if (!path.endsWith("/")) return false;

        return services.stream().anyMatch(s -> matchesServiceLandingPage(path, s));
    }

    /**
     * The path can contain a local workspace and a local layer name, so we need to remove them to
     * get to the actual path in the service
     */
    private boolean matchesServiceLandingPage(String path, APIService s) {
        // global service
        String landingPageSlash = s.landingPage() + "/";
        if (path.equals(landingPageSlash)) return true;

        // workspace local service then?
        String[] components = path.split("/");
        if (components.length <= 3) return false; // ogc/features/v1/... no workspace

        // check if the first component is a workspace or a layer group
        String token1 = components[0];
        if (catalog.getWorkspaceByName(token1) == null
                && catalog.getLayerGroupByName(token1) == null) return false;

        // is it a match now?
        if (landingPageSlash.equals(toLandingPath(components, 1))) return true;

        // maybe layer specific too?
        String token2 = components[1];
        if (catalog.getLayerByName(token1 + ":" + token2) == null) return false;

        // remove both workspace and layer name then
        return landingPageSlash.equals(toLandingPath(components, 2));
    }

    private static String toLandingPath(String[] components, long skip) {
        return stream(components).skip(skip).collect(joining("/")) + "/";
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        services =
                applicationContext.getBeansWithAnnotation(APIService.class).values().stream()
                        .map(o -> APIDispatcher.getApiServiceAnnotation(o.getClass()))
                        .collect(Collectors.toList());
    }

    /**
     * Removes the trailing slash on the path info, so that the landing page is properly served even
     * when trailing slashes are present by their match is disabled
     */
    private class SlashWrapper extends HttpServletRequestWrapper {

        public SlashWrapper(ServletRequest request) {
            super((HttpServletRequest) request);
        }

        @Override
        public String getPathInfo() {
            String path = super.getPathInfo();
            return removeTrailingSlash(path);
        }

        @Override
        public String getRequestURI() {
            String uri = super.getRequestURI();
            return removeTrailingSlash(uri);
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = super.getRequestURL();
            url.setLength(url.length() - 1);
            return url;
        }

        private String removeTrailingSlash(String uri) {
            return uri.substring(0, uri.length() - 1);
        }
    }
}
