/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.geoserver.rest.util.RESTUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * When doing a POST or PUT to endpoints accepting some file, the URL will contain the extension of the POSTed file,
 * while the response should be a standard XML or JSON response, depending on the accepts header.
 *
 * <p>The default ContentNegotiationStrategies will favor the extension when determining the response type. For example,
 * when posting an sld file, "Ponds.sld", to "/rest/styles/Ponds.sld", the default content negotiation assumes this to
 * mean you expect an .sld response.
 *
 * <p>This strategy overrides this behavior for specified paths
 */
public class PutIgnoringExtensionContentNegotiationStrategy implements ContentNegotiationStrategy {

    private final List<PathPattern> patterns;

    List<MediaType> mediaTypes;

    /**
     * Construct a new strategy. This should be instantiated as a bean for it to get picked up by the
     * {@link RestConfiguration}
     *
     * @param patternStrings The path patterns used to determine if the request path matches
     * @param mediaTypes The list of {@link MediaType}s to return when the path matches
     */
    public PutIgnoringExtensionContentNegotiationStrategy(List<String> patternStrings, List<MediaType> mediaTypes) {
        PathPatternParser parser = new PathPatternParser();
        this.patterns = patternStrings.stream().map(parser::parse).toList();
        this.mediaTypes = mediaTypes;
    }

    /**
     * Determine the list of supported media types
     *
     * @return {@link #mediaTypes}, as long as the request is a PUT or POST, and the path provided by webRequest
     *     matches. Otherwise returns an empty list (never null).
     */
    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) return List.of();

        // Check for POST or PUT
        String method = request.getMethod();
        if (!"PUT".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            return List.of();
        }

        // Resolve the request path for PathPatternParser
        PathContainer lookupPath = RESTUtils.pathWithinApplication(request);

        // Match using PathPattern instead of PatternsRequestCondition
        for (PathPattern pattern : patterns) {
            if (pattern.matches(lookupPath)) {
                return mediaTypes;
            }
        }

        return List.of();
    }
}
