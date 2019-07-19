/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A ContentNegotiationManager using the "f" query parameter as a way to request a few well known
 * formats in override to the HTTP Accept header
 */
public class APIContentNegotiationManager extends ContentNegotiationManager {

    public APIContentNegotiationManager() {
        List<ContentNegotiationStrategy> strategies = new ArrayList<>();
        // first use the f parameter
        strategies.add(new FormatContentNegotiationStrategy());
        strategies.add(new HeaderContentNegotiationStrategy());
        this.getStrategies().clear();
        this.getStrategies().addAll(strategies);
    }

    /** Uses the "f" parameter in the request */
    private static class FormatContentNegotiationStrategy implements ContentNegotiationStrategy {

        @Override
        public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
                throws HttpMediaTypeNotAcceptableException {
            String format = webRequest.getParameter("f");
            if ("json".equals(format)) {
                return Collections.singletonList(MediaType.APPLICATION_JSON);
            } else if ("xml".equals(format)) {
                return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
            } else if ("html".equals(format)) {
                return Collections.singletonList(MediaType.TEXT_HTML);
            } else if (format != null) {
                return Collections.singletonList(MediaType.parseMediaType(format));
            } else {
                return MEDIA_TYPE_ALL_LIST;
            }
        }
    }
}
