/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes a {@link BufferedImageLegendGraphic} to the image/jpeg MIME
 * Type
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class JPEGLegendGraphicResponse extends AbstractGetLegendGraphicResponse {

    public JPEGLegendGraphicResponse() {
        super(BufferedImageLegendGraphic.class, JPEGLegendOutputFormat.MIME_TYPE);
    }

    /**
     * @param legend a {@link BufferedImageLegendGraphic}
     * @param output destination for the image written by {@link ImageIO} in the {@link
     *     #getContentType() supported format}
     * @see Response#write(Object, OutputStream, Operation)
     */
    @Override
    public void write(Object legend, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, legend);

        BufferedImage legendImage = ((BufferedImageLegendGraphic) legend).getLegend();
        JAISupport.encode(JPEGLegendOutputFormat.MIME_TYPE, legendImage, output);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, value);
        return JPEGLegendOutputFormat.MIME_TYPE;
    }
}
