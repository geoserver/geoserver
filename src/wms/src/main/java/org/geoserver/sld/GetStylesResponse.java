/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sld;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.StyledLayerDescriptor;

public class GetStylesResponse extends Response {

    public static final String SLD_MIME_TYPE = "application/vnd.ogc.sld+xml";

    public GetStylesResponse() {
        super(StyledLayerDescriptor.class);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return SLD_MIME_TYPE;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        StyledLayerDescriptor sld = (StyledLayerDescriptor) value;

        SLDTransformer tx = new SLDTransformer();
        try {
            tx.setIndentation(4);
            tx.transform(sld, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }

}
