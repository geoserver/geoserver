/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Teams with {@link SuffixContentNegotiationStrategy} and {@link SuffixAwareHandlerMapping} to allow matching path
 * extensions to formats. This filter in particular matches requests to the /rest endpoint, checks for known extensions
 * and, if found, strips them from the request URI while storing them as a request attribute for later use.
 */
@Component
public class SuffixStripFilter extends OncePerRequestFilter implements GeoServerFilter {

    private static final Logger LOGGER = Logging.getLogger(SuffixStripFilter.class);

    // only extensions you want to treat as content-negotiation hints
    private final Set<String> knownExtensions = new HashSet<>(Set.of("html", "xml", "json", "sld", "xslt", "ftl"));

    /**
     * Key of the request attribute used to store the detected extension, if any. The extension is extracted from the
     * last path segment, and then removed from the URI, code in need of the extension will find it as a request
     * attribute under this key.
     */
    public static final String EXTENSION_ATTRIBUTE = "geoserver.formatExtension";

    public SuffixStripFilter(ApplicationContext applicationContext) {
        List<StyleHandler> styleHandlers = GeoServerExtensions.extensions(StyleHandler.class, applicationContext);
        styleHandlers.stream()
                .map(StyleHandler::getFileExtension)
                .filter(Objects::nonNull)
                .forEach(knownExtensions::add);
    }

    /**
     * Allows to programmatically register extensions too
     *
     * @param ext
     */
    public void addExtension(String ext) {
        knownExtensions.add(ext);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String contextPath = request.getContextPath(); // e.g. "/geoserver"
        String uri = request.getRequestURI(); // e.g. "/geoserver/rest/styles/foo.html"

        // General rules:
        // - only apply to /rest/* and /gwc/rest/*
        // - skip /rest/resource/* (static resources, they are files, the extension is not mime type matching)
        // - skip /gwc/rest/seed/* (the controller does its own content negotiation)
        String path = uri.substring(contextPath.length());
        if ((!path.startsWith("/rest/") && !(path.startsWith("/rest.")) && !path.startsWith("/gwc/rest"))
                || path.startsWith("/rest/resource/")
                || path.startsWith("/gwc/rest/seed/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract extension from the full URI
        int lastSlash = uri.lastIndexOf('/');
        int dot = uri.lastIndexOf('.');
        if (dot > lastSlash) {
            String ext = uri.substring(dot + 1);
            if (knownExtensions.contains(ext)) {
                // remember the extension for content negotiation
                request.setAttribute(EXTENSION_ATTRIBUTE, ext);

                // forward to extension-less URI so handler mappings see /rest/styles/foo
                String strippedUri = uri.substring(0, dot);
                HttpServletRequestWrapper wrapper = new StrippedPathRequestWrapper(request, strippedUri, ext);
                filterChain.doFilter(wrapper, response);
                return;
            } else {
                LOGGER.log(Level.WARNING, "Unrecognized extension: " + ext + " found in request URI");
            }
        }

        // No usable extension → just continue normally
        filterChain.doFilter(request, response);
    }

    private static class StrippedPathRequestWrapper extends HttpServletRequestWrapper {
        private final String strippedUri;
        private final String ext;

        public StrippedPathRequestWrapper(HttpServletRequest request, String strippedUri, String ext) {
            super(request);
            this.strippedUri = strippedUri;
            this.ext = ext;
        }

        @Override
        public String getRequestURI() {
            return strippedUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer requestURL = super.getRequestURL();
            requestURL.setLength(requestURL.length() - (ext.length() + 1));
            return requestURL;
        }

        @Override
        public String getPathInfo() {
            return super.getPathInfo().substring(0, super.getPathInfo().length() - (ext.length() + 1));
        }
    }
}
