/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpHeaders;

/**
 * Adds proper caching headers to capabilites response clients paying attention to HTTP headers do
 * not think they are cacheable
 *
 * <p>The callback can be turned off by setting "CAPABILITIES_CACHE_CONTROL_ENABLED" to "false",
 * either as a system, environment or servlet context variable.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CapabilitiesCacheHeadersCallback extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(CapabilitiesCacheHeadersCallback.class);

    boolean capabilitiesCacheHeadersEnabled;

    GeoServer gs;

    public CapabilitiesCacheHeadersCallback(GeoServer gs) {
        this.gs = gs;

        // initialize headers processing by grabbing the default from a property
        final String value = GeoServerExtensions.getProperty("CAPABILITIES_CACHE_CONTROL_ENABLED");
        if (value != null) {
            capabilitiesCacheHeadersEnabled = Boolean.parseBoolean(value);
        } else {
            capabilitiesCacheHeadersEnabled = true;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Cache control for capabilities requests and 304 support is enabled: "
                            + capabilitiesCacheHeadersEnabled);
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        if (handleCachingHeaders(request)) {
            return new RevalidateTagResponse(response);
        }

        return response;
    }

    /** Returns true if the caching headers are enabled and the request is a GetCapabilities one */
    private boolean handleCachingHeaders(Request request) {
        return capabilitiesCacheHeadersEnabled
                && "GetCapabilities".equalsIgnoreCase(request.getRequest());
    }

    /**
     * Returns true if the callback will handle cache headers in GetCapabilities requests/responses
     */
    public boolean isCapabilitiesCacheHeadersEnabled() {
        return capabilitiesCacheHeadersEnabled;
    }

    /** Enables/disables the caching headers processing for this callback */
    public void setCapabilitiesCacheHeadersEnabled(boolean capabilitiesCacheHeadersEnabled) {
        this.capabilitiesCacheHeadersEnabled = capabilitiesCacheHeadersEnabled;
    }

    /**
     * A Response wrapper adding caching headers on demand
     *
     * @author aaime
     */
    private class RevalidateTagResponse extends Response {

        Response delegate;

        public RevalidateTagResponse(Response delegate) {
            super(delegate.getBinding());
            this.delegate = delegate;
        }

        public boolean canHandle(Operation operation) {
            return delegate.canHandle(operation);
        }

        public String getMimeType(Object value, Operation operation) throws ServiceException {
            return delegate.getMimeType(value, operation);
        }

        /**
         * See if we have to add cache control headers. Won't alter them if the response already set
         * them.
         */
        public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
            String[][] headers = delegate.getHeaders(value, operation);
            if (headers == null) {
                // if no headers at all, add and exit
                return new String[][] {{HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate"}};
            } else {
                // will add only if not already there
                Map<String, String> map = (Map) ArrayUtils.toMap(headers);
                map.putIfAbsent(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate");
                headers = new String[map.size()][2];
                int i = 0;
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    headers[i][0] = entry.getKey();
                    headers[i][1] = entry.getValue();
                    i++;
                }
            }

            return headers;
        }

        public void write(Object value, OutputStream output, Operation operation)
                throws IOException, ServiceException {
            delegate.write(value, output, operation);
        }

        public String getPreferredDisposition(Object value, Operation operation) {
            return delegate.getPreferredDisposition(value, operation);
        }

        public String getAttachmentFileName(Object value, Operation operation) {
            return delegate.getAttachmentFileName(value, operation);
        }

        public String getCharset(Operation operation) {
            return delegate.getCharset(operation);
        }
    }
}
