/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.api.ServiceException;
import org.geoserver.gsr.model.exception.ServiceError;
import org.geoserver.kml.KMZMapOutputFormat;
import org.geoserver.ogcapi.APIDispatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * GSR and OGC API are pretty similar, so it makes sense to base dispatching GSR on the OGC API
 * dispatcher. The benefits are major, including:
 *
 * <ul>
 *   <li>Proper integration with dispatcher subsystem (e.g., control flow, monitoring)
 *   <li>Proper integration with security subsystem
 *   <li>Shared machinery for generating HTML output, including shared look and feel
 * </ul>
 *
 * <p>However, the GRS behaves differently in terms of request format handling and error reporting,
 * so a subclass is needed
 */
public class GSRDispatcher extends APIDispatcher {

    public GSRDispatcher(GeoServer geoServer) {
        super(geoServer);
        this.contentNegotiationManager = new GSRContentNegotiationManager();
    }

    static class GSRContentNegotiationManager extends ContentNegotiationManager {

        public GSRContentNegotiationManager() {
            List<ContentNegotiationStrategy> strategies = new ArrayList<>();
            // first use the f parameter
            strategies.add(new FormatContentNegotiationStrategy());
            strategies.add(new HeaderContentNegotiationStrategy());
            this.getStrategies().clear();
            this.getStrategies().addAll(strategies);
        }

        @Override
        public List<MediaType> resolveMediaTypes(NativeWebRequest request)
                throws HttpMediaTypeNotAcceptableException {
            List<MediaType> mediaTypes = super.resolveMediaTypes(request);
            if (mediaTypes == MEDIA_TYPE_ALL_LIST) {
                return mediaTypes;
            }

            // this is a hack to make the code return JSON when no other requested output
            // format was found
            List<MediaType> result = new ArrayList<>(mediaTypes);
            result.add(MediaType.APPLICATION_JSON);
            return result;
        }

        /** Uses the "f" and "format" parameter in the request */
        private static class FormatContentNegotiationStrategy
                implements ContentNegotiationStrategy {

            @Override
            public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) {
                String f = webRequest.getParameter("f");
                if ("json".equals(f) || "pjson".equals(f)) {
                    return Collections.singletonList(MediaType.APPLICATION_JSON);
                } else if ("geojson".equals(f)) {
                    return Collections.singletonList(
                            MediaType.parseMediaType("application/geo+json"));
                } else if ("kmz".equals(f)) {
                    return Collections.singletonList(
                            MediaType.parseMediaType(KMZMapOutputFormat.MIME_TYPE));
                } else if ("xml".equals(f)) {
                    return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
                } else if ("html".equals(f)) {
                    return Collections.singletonList(MediaType.TEXT_HTML);
                } else if ("image".equals(f)) {
                    String format = webRequest.getParameter("format");
                    // if format not provided defaults to PNG
                    if (format == null) {
                        return Collections.singletonList(MediaType.IMAGE_PNG);
                    }
                    return Collections.singletonList(MediaType.parseMediaType("image/" + format));
                } else if (f != null) {
                    throw new ServiceException(
                            "Output format not supported",
                            new ServiceError(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Output format not " + "supported",
                                    Collections.singletonList(
                                            "Format " + f + " is " + "not supported")));
                } else {
                    return MEDIA_TYPE_ALL_LIST;
                }
            }
        }
    }
}
