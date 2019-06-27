/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;

/**
 * Appends paging links as HTTP headers for GetFeature responses, mandatory for all formats that
 * cannot do their own link encoding, and useful for all in general
 */
public class HttpHeaderLinksAppender extends AbstractDispatcherCallback {

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        // is this a feature response we are about to encode?
        if (result instanceof FeaturesResponse) {
            HttpServletResponse httpResponse = request.getHttpResponse();
            FeatureCollectionResponse fcr = ((FeaturesResponse) result).getResponse();
            String contentType = response.getMimeType(result, operation);
            if (fcr.getPrevious() != null) {
                addLink(httpResponse, "prev", contentType, fcr.getPrevious());
            }
            if (fcr.getNext() != null) {
                addLink(httpResponse, "next", contentType, fcr.getNext());
            }
        }

        return response;
    }

    private void addLink(
            HttpServletResponse httpResponse, String rel, String contentType, String href) {
        String headerValue = String.format("<%s>; rel=\"%s\"; type=\"%s\"", href, rel, contentType);
        httpResponse.addHeader("Link", headerValue);
    }
}
