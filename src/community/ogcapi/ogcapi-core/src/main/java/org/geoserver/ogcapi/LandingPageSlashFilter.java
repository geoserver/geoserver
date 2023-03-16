/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

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

    private List<APIService> services;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest
                && isLandingPage((HttpServletRequest) servletRequest)) {
            filterChain.doFilter(new SlashWrapper(servletRequest), servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean isLandingPage(HttpServletRequest servletRequest) {
        String path = "ogc" + servletRequest.getPathInfo();
        return services.stream().anyMatch(s -> (s.landingPage() + "/").equals(path));
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
