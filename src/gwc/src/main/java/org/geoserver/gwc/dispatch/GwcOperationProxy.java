/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import java.util.Map;
import org.geoserver.ows.Dispatcher;

/**
 * A simple bean acting as the response for the {@link GwcServiceProxy#dispatch} operation, in order
 * to make a call to a GWC service fit into the GeoServer {@link Dispatcher} framework.
 *
 * <p>See the package documentation for more insights on how these all fit together.
 */
public class GwcOperationProxy {

    private String contentType;

    private byte[] responseContent;

    private Map<String, String> responseHeaders;

    public GwcOperationProxy(
            final String contentType,
            final Map<String, String> headers,
            final byte[] responseContent)
            throws Exception {
        this.contentType = contentType;
        this.responseContent = responseContent;
        this.responseHeaders = headers;
    }

    public String getMimeType() {
        return contentType;
    }

    public byte[] getContents() {
        return responseContent;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
}
