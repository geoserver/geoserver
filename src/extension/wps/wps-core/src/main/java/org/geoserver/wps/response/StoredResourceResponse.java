/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wps.GetExecutionResultType;
import org.geoserver.wps.GetExecutionStatusType;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.resource.WPSResourceManager;

/**
 * Returns a response already computed and stored in an output
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StoredResourceResponse extends Response {
    WPSResourceManager manager;

    public StoredResourceResponse(WPSResourceManager manager) {
        super(Resource.class);
        this.manager = manager;
    }

    @Override
    public boolean canHandle(Operation operation) {
        String operationId = operation.getId();
        return ("GetExecutionStatus".equalsIgnoreCase(operationId)
                        || "GetExecutionResult".equalsIgnoreCase(operationId))
                && operation.getService().getId().equals("wps");
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        Object request = operation.getParameters()[0];
        if (request instanceof GetExecutionStatusType) {
            return "text/xml";
        } else if (request instanceof GetExecutionResultType) {
            GetExecutionResultType ger = (GetExecutionResultType) request;
            Resource mimeResource =
                    manager.getOutputResource(ger.getExecutionId(), ger.getOutputId() + ".mime");
            if (mimeResource == null || mimeResource.getType() == Type.UNDEFINED) {
                throw new WPSException(
                        "Unknown output "
                                + ger.getOutputId()
                                + " for execution id "
                                + ger.getExecutionId()
                                + ", either the execution was never submitted or too much time "
                                + "elapsed since the process completed");
            }
            try (InputStream input = mimeResource.in()) {
                String mimeType = IOUtils.toString(input, StandardCharsets.UTF_8);
                if (ger.getMimeType() == null || ger.getMimeType().equals(mimeType)) {
                    return mimeType;
                }
                throw new WPSException(
                        "Requested mime type does not match the output resource mime type");
            } catch (IOException e) {
                throw new WPSException("Error validating the output resource mime type", e);
            }
        } else {
            throw new WPSException(
                    "Trying to get a mime type for a unknown operation, "
                            + "we should not have got here in the first place");
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        Object request = operation.getParameters()[0];
        if (request instanceof GetExecutionStatusType) {
            return "text/xml";
        } else if (request instanceof GetExecutionResultType) {
            GetExecutionResultType ger = (GetExecutionResultType) request;
            if (ger.getOutputId() != null) {
                return ger.getOutputId();
            } else {
                // we should really never get here, the request should fail before
                return "result.dat";
            }
        } else {
            throw new WPSException(
                    "Trying to get a file name for a unknown operation, "
                            + "we should not have got here in the first place");
        }
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        Resource resource = (Resource) value;
        try (InputStream is = resource.in()) {
            IOUtils.copy(is, output);
        }
    }
}
