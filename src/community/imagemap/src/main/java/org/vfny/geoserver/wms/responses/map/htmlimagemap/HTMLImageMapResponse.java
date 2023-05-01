/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.springframework.util.Assert;

/**
 * Handles a GetMap response that produces a map in HTMLImageMap format.
 *
 * @author Mauro Bartolomeoli
 */
public class HTMLImageMapResponse extends Response {

    public HTMLImageMapResponse() {
        super(EncodeHTMLImageMap.class, HTMLImageMapMapProducer.MIME_TYPE);
    }

    /**
     * Writes the generated map to an OutputStream.
     *
     * @param output final output stream
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.isInstanceOf(EncodeHTMLImageMap.class, value);
        EncodeHTMLImageMap htmlImageMapEncoder = (EncodeHTMLImageMap) value;
        try {
            htmlImageMapEncoder.encode(output);
        } finally {
            htmlImageMapEncoder.dispose();
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return HTMLImageMapMapProducer.MIME_TYPE;
    }
}
