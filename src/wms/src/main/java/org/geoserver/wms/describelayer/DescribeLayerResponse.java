/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
import org.geoserver.wms.DescribeLayer;
import org.geoserver.wms.DescribeLayerRequest;
import org.springframework.util.Assert;

/**
 * Executes a <code>DescribeLayer</code> WMS request.
 *
 * <p>Receives a <code>DescribeLayerRequest</code> object holding the references to the requested
 * layers and utilizes a transformer based on the org.geotools.xml.transform framework to encode the
 * response.
 *
 * @author Gabriel Roldan
 * @author Carlo Cancellieri
 * @version $Id$
 */
public abstract class DescribeLayerResponse extends Response {

    private final String type;

    /** Creates a new GetMapResponse object. */
    public DescribeLayerResponse(String format) {
        super(DescribeLayerModel.class, format);
        this.type = format;
    }

    /**
     * Evaluates if this DescribeLayer producer can generate the format specified by <code>format
     * </code>, where <code>format</code> is the MIME type of the requested response.
     *
     * @param format the MIME type of the required output format, might be {@code null}
     * @return true if class can produce a DescribeLayer in the passed format
     */
    public boolean canProduce(String format) {
        return type.equalsIgnoreCase(format);
    }

    public String getContentType() {
        return type;
    }

    /**
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {

        Object op = operation.getParameters()[0];
        if (op instanceof DescribeLayerRequest) {
            DescribeLayerRequest dlr = (DescribeLayerRequest) op;
            return dlr.getOutputFormat();
        }
        throw new ServiceException("Unable to parse incoming operation");
    }

    /**
     * @param value {@link DescribeLayerTransformer}
     * @param output where to write the response
     * @param operation {@link DescribeLayer} operation that originated the {@code value} response
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        Assert.notNull(operation.getParameters(), "parameters");
        Assert.isTrue(
                operation.getParameters()[0] instanceof DescribeLayerRequest,
                "The first parameter must be a DescribeLayerRequest");
        final DescribeLayerRequest request = (DescribeLayerRequest) operation.getParameters()[0];

        Assert.isTrue(value instanceof DescribeLayerModel, "Value should be a DescribeLayerModel");
        final DescribeLayerModel results = (DescribeLayerModel) value;
        try {
            write(results, request, output);
        } finally {
            if (output != null) {
                try {
                    output.flush();
                } catch (IOException ioe) {
                }
                IOUtils.closeQuietly(output);
            }
        }
    }

    public abstract void write(
            DescribeLayerModel description, DescribeLayerRequest output, OutputStream operation)
            throws IOException, ServiceException;
}
