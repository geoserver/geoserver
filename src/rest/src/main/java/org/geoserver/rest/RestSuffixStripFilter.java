package org.geoserver.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.geoserver.filters.GeoServerFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RestSuffixStripFilter extends OncePerRequestFilter implements GeoServerFilter {

    // only extensions you want to treat as content-negotiation hints
    private final Set<String> knownExtensions = Set.of("html", "xml", "json", "sld", "xslt", "ftl");

    public static final String FORMAT_ATTRIBUTE = "geoserver.formatExtension";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String contextPath = request.getContextPath(); // e.g. "/geoserver"
        String uri = request.getRequestURI(); // e.g. "/geoserver/rest/styles/foo.html"

        // Restrict to REST only
        String restPrefix1 = contextPath + "/rest/";
        String restPrefix2 = contextPath + "/rest.";
        if (!uri.startsWith(restPrefix1) && !uri.startsWith(restPrefix2)) {
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
                request.setAttribute(FORMAT_ATTRIBUTE, ext);

                // forward to extension-less URI so handler mappings see /rest/styles/foo
                String forwardUri = uri.substring(0, dot);
                HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getRequestURI() {
                        return forwardUri;
                    }

                    @Override
                    public StringBuffer getRequestURL() {
                        StringBuffer requestURL = super.getRequestURL();
                        requestURL.setLength(requestURL.length() - (ext.length() + 1));
                        return requestURL;
                    }

                    @Override
                    public String getPathInfo() {
                        return super.getPathInfo()
                                .substring(0, super.getPathInfo().length() - (ext.length() + 1));
                    }
                };
                filterChain.doFilter(wrapper, response);
                return;
            }
        }

        // No usable extension → just continue normally
        filterChain.doFilter(request, response);
    }
}
