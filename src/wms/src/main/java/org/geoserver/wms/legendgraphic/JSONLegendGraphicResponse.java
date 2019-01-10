/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import net.sf.json.JSONObject;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes a {@link BufferedImageLegendGraphic} to the image/png MIME Type
 *
 * @author Ian Turton
 * @version $Id$
 */
public class JSONLegendGraphicResponse extends AbstractGetLegendGraphicResponse {

    public JSONLegendGraphicResponse() {
        super(JSONLegendGraphic.class, JSONLegendOutputFormat.MIME_TYPE);
    }

    /**
     * @param legend a {@link JSONLegendGraphic}
     * @param output JSON destination
     * @see GetLegendGraphicProducer#writeTo(java.io.OutputStream)
     */
    @Override
    public void write(Object legend, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.isInstanceOf(JSONLegendGraphic.class, legend);

        JSONObject json = (JSONObject) ((LegendGraphic) legend).getLegend();
        PrintWriter writer = new PrintWriter(output);

        try {
            writer.write(json.toString(2));
        } finally {
            writer.close();
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(JSONLegendGraphic.class, value);
        return JSONLegendOutputFormat.MIME_TYPE;
    }
}
