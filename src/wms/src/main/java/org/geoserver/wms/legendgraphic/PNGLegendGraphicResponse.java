/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.media.jai.PlanarImage;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.image.ImageWorker;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes a {@link BufferedImageLegendGraphic} to the image/png MIME Type
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class PNGLegendGraphicResponse extends AbstractGetLegendGraphicResponse {

    public PNGLegendGraphicResponse() {
        super(BufferedImageLegendGraphic.class, PNGLegendOutputFormat.MIME_TYPE);
    }

    /**
     * @param legend a {@link BufferedImageLegendGraphic}
     * @param output png image destination
     * @see GetLegendGraphicProducer#writeTo(java.io.OutputStream)
     */
    @Override
    public void write(Object legend, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, legend);

        BufferedImage image = (BufferedImage) ((LegendGraphic) legend).getLegend();
        // /////////////////////////////////////////////////////////////////
        //
        // Reformatting this image for png
        //
        // /////////////////////////////////////////////////////////////////
        final MemoryCacheImageOutputStream memOutStream = new MemoryCacheImageOutputStream(output);
        final ImageWorker worker = new ImageWorker(image);
        final PlanarImage finalImage =
                (image.getColorModel() instanceof DirectColorModel)
                        ? worker.forceComponentColorModel().getPlanarImage()
                        : worker.getPlanarImage();

        // /////////////////////////////////////////////////////////////////
        //
        // Getting a writer
        //
        // /////////////////////////////////////////////////////////////////
        final Iterator<ImageWriter> it;
        it = ImageIO.getImageWritersByMIMEType(PNGLegendOutputFormat.MIME_TYPE);
        ImageWriter writer = null;

        if (!it.hasNext()) {
            throw new IllegalStateException("No PNG ImageWriter found");
        } else {
            writer = (ImageWriter) it.next();
        }

        // /////////////////////////////////////////////////////////////////
        //
        // Compression is available only on native lib
        //
        // /////////////////////////////////////////////////////////////////
        final ImageWriteParam iwp = writer.getDefaultWriteParam();

        if (writer.getClass()
                .getName()
                .equals("com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriter")) {
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            iwp.setCompressionQuality(0.75f); // we can control quality here
        }

        writer.setOutput(memOutStream);
        try {
            writer.write(null, new IIOImage(finalImage, null, null), iwp);
            memOutStream.flush();
            // this doesn't close the destination output stream
            memOutStream.close();
        } finally {
            writer.dispose();
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(BufferedImageLegendGraphic.class, value);
        return PNGLegendOutputFormat.MIME_TYPE;
    }
}
