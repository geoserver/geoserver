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

import org.apache.commons.lang.ArrayUtils;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Adds proper caching headers to capabilites response clients paying attention to HTTP headers do 
 * not they are cacheable, yet allowing these same clients to perform conditional requests and avoid
 * the caps document computation if nothing has changed, via ETag support.
 * 
 * The callback can be turned off by setting "CAPABILITIES_CACHE_CONTROL_ENABLED" to "false", either
 * as a system, environment or servlet context variable.
 *  
 * @author Andrea Aime - GeoSolutions
 */
public class CapabilitiesCacheHeadersCallback extends AbstractDispatcherCallback
        implements GeoServerLifecycleHandler {

    static final Logger LOGGER = Logging.getLogger(CapabilitiesCacheHeadersCallback.class);

    boolean capabilitiesCacheHeadersEnabled;

    GeoServer gs;

    private long lastLoaded;

    public CapabilitiesCacheHeadersCallback(GeoServer gs) {
        this.gs = gs;
        this.lastLoaded = System.currentTimeMillis();
        
        // initialize headers processing by grabbing the default from a property
        final String value = GeoServerExtensions.getProperty("CAPABILITIES_CACHE_CONTROL_ENABLED");
        if(value != null) {
            capabilitiesCacheHeadersEnabled = Boolean.parseBoolean(value);
        } else {
            capabilitiesCacheHeadersEnabled = true;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Cache control for capabilities requests and 304 support is enabled: "
                    + capabilitiesCacheHeadersEnabled);
        }
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        // check conditional requests on GetCapabilities
        if (handleCachingHeaders(request)) {
            String clientTag = request.getHttpRequest().getHeader(HttpHeaders.IF_NONE_MATCH);
            String currentTag = computeTag();
            if (clientTag != null && currentTag.equals(clientTag)) {
                throw new HttpErrorCodeException(HttpStatus.NOT_MODIFIED.value());
            }
        }

        return operation;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result,
            Response response) {
        if (handleCachingHeaders(request)) {
            return new RevalidateTagResponse(response, computeTag());
        }

        return response;
    }
    
    /**
     * Returns true if the caching headers are enabled and the request is a GetCapabilities one
     * @param request
     * @return
     */
    private boolean handleCachingHeaders(Request request) {
        return capabilitiesCacheHeadersEnabled && "GetCapabilities".equalsIgnoreCase(request.getRequest());
    }


    private String computeTag() {
        return lastLoaded + "-" + gs.getGlobal().getUpdateSequence();
    }

    @Override
    public void onReset() {
        // nothing to do
    }

    @Override
    public void onDispose() {
        // nothing to do
    }

    @Override
    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onReload() {
        this.lastLoaded = System.currentTimeMillis();
    }
    
    /**
     * Returns true if the callback will handle cache headers in GetCapabilities requests/responses
     * @return
     */
    public boolean isCapabilitiesCacheHeadersEnabled() {
        return capabilitiesCacheHeadersEnabled;
    }

    /**
     * Enables/disables the caching headers processing for this callback
     * 
     * @param capabilitiesCacheHeadersEnabled
     */
    public void setCapabilitiesCacheHeadersEnabled(boolean capabilitiesCacheHeadersEnabled) {
        this.capabilitiesCacheHeadersEnabled = capabilitiesCacheHeadersEnabled;
    }

    /**
     * A Response wrapper adding caching headers on demand
     * @author aaime
     */
    private class RevalidateTagResponse extends Response {

        Response delegate;

        String tag;

        public RevalidateTagResponse(Response delegate, String tag) {
            super(delegate.getBinding());
            this.delegate = delegate;
            this.tag = tag;
        }

        public boolean canHandle(Operation operation) {
            return delegate.canHandle(operation);
        }

        public String getMimeType(Object value, Operation operation) throws ServiceException {
            return delegate.getMimeType(value, operation);
        }

        /**
         * See if we have to add cache control headers. Won't alter them if the response already set them.
         */
        public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
            String[][] headers = delegate.getHeaders(value, operation);
            if (headers == null) {
                // if no headers at all, add and exit
                return new String[][] { { HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate" },
                        { HttpHeaders.ETAG, tag } };
            } else {
                // will add only if not already there
                Map<String, String> map = ArrayUtils.toMap(headers);
                map.putIfAbsent(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate");
                map.putIfAbsent(HttpHeaders.ETAG, tag);
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
