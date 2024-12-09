/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.http.MediaType.TEXT_XML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * A ContentNegotiationManager using the "f" query parameter as a way to request a few well known
 * formats in override to the HTTP Accept header
 */
public class APIContentNegotiationManager extends ContentNegotiationManager {

    public APIContentNegotiationManager() {
        List<ContentNegotiationStrategy> strategies = new ArrayList<>();
        // first use the f parameter
        strategies.add(new FormatContentNegotiationStrategy());
        strategies.add(new OpenAPIContentNegotiationStrategy());
        strategies.add(new HeaderContentNegotiationStrategy());
        this.getStrategies().clear();
        this.getStrategies().addAll(strategies);
    }

    /** Uses the "f" parameter in the request */
    private static class FormatContentNegotiationStrategy implements ContentNegotiationStrategy {

        @Override
        public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) {
            String format = webRequest.getParameter("f");
            if ("json".equals(format)) {
                return Arrays.asList(
                        MediaType.APPLICATION_JSON, MediaType.parseMediaType("application/*+json"));
            } else if ("xml".equals(format)) {
                return Arrays.asList(MediaType.APPLICATION_XML, TEXT_XML);
            } else if ("html".equals(format)) {
                return Collections.singletonList(TEXT_HTML);
            } else if ("yaml".equals(format)) {
                return Collections.singletonList(APPLICATION_YAML);
            } else if (format != null) {
                return Collections.singletonList(MediaType.parseMediaType(format));
            }
            return MEDIA_TYPE_ALL_LIST;
        }
    }

    private class OpenAPIContentNegotiationStrategy implements ContentNegotiationStrategy {
        @Override
        public List<MediaType> resolveMediaTypes(NativeWebRequest nativeWebRequest) {
            if (nativeWebRequest instanceof ServletWebRequest) {
                ServletWebRequest servletWebRequest = (ServletWebRequest) nativeWebRequest;
                if (servletWebRequest.getRequest().getRequestURI().endsWith("/openapi.json")) {
                    return Arrays.asList(OPEN_API_MEDIA_TYPE);
                } else if (servletWebRequest
                        .getRequest()
                        .getRequestURI()
                        .endsWith("/openapi.yaml")) {
                    return Arrays.asList(APPLICATION_YAML);
                }
            }
            return MEDIA_TYPE_ALL_LIST;
        }
    }
}
