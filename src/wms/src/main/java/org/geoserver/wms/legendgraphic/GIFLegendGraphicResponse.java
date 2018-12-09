/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.image.ImageWorker;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes a {@link BufferedImageLegendGraphic} to the image/gif MIME Type
 *
 * @author groldan
 */
public class GIFLegendGraphicResponse extends AbstractGetLegendGraphicResponse {

    public GIFLegendGraphicResponse() {
        super(BufferedImageLegendGraphic.class, GIFLegendOutputFormat.MIME_TYPE);
    }

    /**
     * @return {@code image/gif}
     * @see Response#getMimeType(Object, Operation)
     */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, value);
        return GIFLegendOutputFormat.MIME_TYPE;
    }

    /**
     * @param legend a {@link BufferedImageLegendGraphic}
     * @param output image destination
     * @param operation Operation descriptor the {@code legend} was produced for
     * @see Response#write(Object, OutputStream, Operation)
     */
    @Override
    public void write(Object legend, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        Assert.isInstanceOf(BufferedImageLegendGraphic.class, legend);

        BufferedImage legendGraphic = (BufferedImage) ((LegendGraphic) legend).getLegend();

        RenderedImage forcedIndexed8Bitmask = ImageUtils.forceIndexed8Bitmask(legendGraphic, null);
        ImageWorker imageWorker = new ImageWorker(forcedIndexed8Bitmask);
        imageWorker.writeGIF(output, "LZW", 0.75f);
    }
}
