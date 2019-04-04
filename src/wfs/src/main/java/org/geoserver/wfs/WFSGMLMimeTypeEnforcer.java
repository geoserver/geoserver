/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

/**
 * Dispatcher callback that will enforce configured GML MIME type for WFS GML responses. If no GML
 * enforcing MIME type is configured nothing will be done.
 */
public final class WFSGMLMimeTypeEnforcer extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(WFSGMLMimeTypeEnforcer.class);

    private final GeoServer geoserver;

    public WFSGMLMimeTypeEnforcer(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        Service service = operation.getService();
        if (service == null
                || service.getId() == null
                || !service.getId().equalsIgnoreCase("wfs")) {
            // not a WFS service so we are not interested in it
            return response;
        }
        String responseMimeType = response.getMimeType(result, operation);
        if (!isGmlBased(responseMimeType)) {
            // no a GML based response
            return response;
        }
        WFSInfo wfs = geoserver.getService(WFSInfo.class);
        GMLInfo gmlInfo =
                wfs.getGML().get(WFSInfo.Version.negotiate(service.getVersion().toString()));
        if (gmlInfo == null || !gmlInfo.getMimeTypeToForce().isPresent()) {
            // we don't need to force any specific MIME type
            return response;
        }
        // enforce the configured MIME type
        String mimeType = gmlInfo.getMimeTypeToForce().get();
        LOGGER.info(
                String.format(
                        "Overriding MIME type '%s' with '%s' for WFS operation '%s'.",
                        responseMimeType, mimeType, operation.getId()));
        return new ResponseWrapper(response, mimeType);
    }

    /** Helper method that checks if a MIME type is GML based. */
    private boolean isGmlBased(String candidateMimeType) {
        if (candidateMimeType == null) {
            // unlikely situation but in this we don't consider this MIME type a GML one
            return false;
        }
        // check if the MIME type contains GML
        candidateMimeType = candidateMimeType.toLowerCase();
        return candidateMimeType.contains("gml");
    }

    /** Helper wrapper for responses to use the configured MIME type. */
    private static final class ResponseWrapper extends Response {

        private final Response response;
        private final String mimeType;

        public ResponseWrapper(Response response, String mimeType) {
            super(response.getBinding(), mimeType);
            this.response = response;
            this.mimeType = mimeType;
        }

        @Override
        public String getMimeType(Object value, Operation operation) throws ServiceException {
            return mimeType;
        }

        @Override
        public void write(Object value, OutputStream output, Operation operation)
                throws IOException, ServiceException {
            response.write(value, output, operation);
        }

        @Override
        public boolean canHandle(Operation operation) {
            return response.canHandle(operation);
        }

        @Override
        public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
            return response.getHeaders(value, operation);
        }

        @Override
        public String getPreferredDisposition(Object value, Operation operation) {
            return response.getPreferredDisposition(value, operation);
        }

        @Override
        public String getAttachmentFileName(Object value, Operation operation) {
            return response.getAttachmentFileName(value, operation);
        }

        @Override
        public String getCharset(Operation operation) {
            return response.getCharset(operation);
        }
    }
}
