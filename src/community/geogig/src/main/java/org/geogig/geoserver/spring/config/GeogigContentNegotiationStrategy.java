/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.locationtech.geogig.rest.Variants;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

/** Lists the output {@link MediaType}s that are supported by the various geogig endpoints. */
public class GeogigContentNegotiationStrategy implements ContentNegotiationStrategy {
    List<MediaType> mediaTypes;

    public GeogigContentNegotiationStrategy() {
        this.mediaTypes =
                Arrays.asList(
                        MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_XML,
                        Variants.CSV_MEDIA_TYPE,
                        Variants.GEOPKG_MEDIA_TYPE);
    }

    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
            throws HttpMediaTypeNotAcceptableException {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request.getServletPath().equals("/geogig")) {
            return mediaTypes;
        }
        return new ArrayList<>();
    }
}
