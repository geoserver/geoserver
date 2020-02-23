/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;

/**
 * When doing a POST or PUT to endpoints accepting some file, the URL will contain the extension of
 * the POSTed file, while the response should be a standard XML or JSON response, depending on the
 * accepts header.
 *
 * <p>The default ContentNegotiationStrategies will favor the extension when determining the
 * response type. For example, when posting an sld file, "Ponds.sld", to "/rest/styles/Ponds.sld",
 * the default content negotiation assumes this to mean you expect an .sld response.
 *
 * <p>This strategy overrides this behavior for specified paths
 */
public class PutIgnoringExtensionContentNegotiationStrategy implements ContentNegotiationStrategy {

    PatternsRequestCondition pathMatcher;
    List<MediaType> mediaTypes;

    /**
     * Construct a new strategy. This should be instantiated as a bean for it to get picked up by
     * the {@link RestConfiguration}
     *
     * @param pathMatcher The {@link PatternsRequestCondition} used to determine if the request path
     *     matches
     * @param mediaTypes The list of {@link MediaType}s to return when the path matches
     */
    public PutIgnoringExtensionContentNegotiationStrategy(
            PatternsRequestCondition pathMatcher, List<MediaType> mediaTypes) {
        this.pathMatcher = pathMatcher;
        this.mediaTypes = mediaTypes;
    }

    /**
     * Determine the list of supported media types
     *
     * @return {@link #mediaTypes}, as long as the request is a PUT or POST, and the path provided
     *     by webRequest matches. Otherwise returns an empty list (never null).
     */
    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
            throws HttpMediaTypeNotAcceptableException {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request != null
                && pathMatcher.getMatchingCondition(request) != null
                && ("PUT".equals(request.getMethod()) || "POST".equals(request.getMethod()))) {
            return mediaTypes;
        }
        return new ArrayList<>();
    }
}
