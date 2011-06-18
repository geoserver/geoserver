/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DescribeLayer;
import org.geoserver.wms.DescribeLayerRequest;
import org.springframework.util.Assert;

/**
 * Executes a <code>DescribeLayer</code> WMS request.
 * 
 * <p>
 * Receives a <code>DescribeLayerRequest</code> object holding the references to the requested
 * layers and utilizes a transformer based on the org.geotools.xml.transform framework to encode the
 * response.
 * </p>
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class DescribeLayerResponse extends Response {

    public static final String DESCLAYER_MIME_TYPE = "application/vnd.ogc.wms_xml";

    public DescribeLayerResponse() {
        super(DescribeLayerTransformer.class);
    }

    /**
     * @return {@code "application/vnd.ogc.wms_xml"}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return DESCLAYER_MIME_TYPE;
    }

    /**
     * @param value
     *            {@link DescribeLayerTransformer}
     * @param output
     *            where to write the response
     * @param operation
     *            {@link DescribeLayer} operation that originated the {@code value} response
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {

        Assert.isTrue(value instanceof DescribeLayerTransformer);
        Assert.notNull(operation.getParameters());
        Assert.isTrue(operation.getParameters()[0] instanceof DescribeLayerRequest);

        DescribeLayerTransformer transformer = (DescribeLayerTransformer) value;
        DescribeLayerRequest request = (DescribeLayerRequest) operation.getParameters()[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            transformer.transform(request, out);
            out.flush();
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
        output.write(out.toByteArray());
        output.flush();
    }

}
