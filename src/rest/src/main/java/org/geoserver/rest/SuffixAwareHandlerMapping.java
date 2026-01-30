/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Teams with {@link SuffixContentNegotiationStrategy} and {@link SuffixStripFilter} to allow matching path extensions
 * to formats. In particular, this mapping attempts to match both the stripped path (e.g. /styles/foo) and the original
 * path with extension (e.g. /styles/foo.json) to allow for patterns that include a format-like path variable at the end
 * of the URL.
 */
public class SuffixAwareHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

        // First, try with the already-stripped path (from filter)
        HandlerMethod handler = super.lookupHandlerMethod(lookupPath, request);

        // If not found, check if there was an extension and try with it
        if (handler == null) {
            String extension = (String) request.getAttribute(SuffixStripFilter.EXTENSION_ATTRIBUTE);
            if (extension != null) {
                // Try with extension added back for patterns like /{method}.{format}
                String pathWithExtension = lookupPath + "." + extension;
                // Create a wrapped request with modified path
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getRequestURI() {
                        return request.getContextPath() + pathWithExtension;
                    }

                    @Override
                    public String getServletPath() {
                        return pathWithExtension;
                    }

                    @Override
                    public Object getAttribute(String name) {
                        if (UrlPathHelper.PATH_ATTRIBUTE.equals(name)) return pathWithExtension;
                        return super.getAttribute(name);
                    }
                };
                handler = super.lookupHandlerMethod(pathWithExtension, wrappedRequest);
            }
        }

        return handler;
    }
}
