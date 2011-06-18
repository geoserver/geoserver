/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geotools.image.ImageWorker;
import org.geotools.image.palette.InverseColorMapOp;

/**
 * Handles a GetMap request that spects a map in GIF format.
 * 
 * @author Didier Richard
 * @author Simone Giannecchini - GeoSolutions
 * @version $Id
 */
public final class GIFMapResponse extends RenderedImageMapResponse {

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "image/gif";

    public GIFMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     * 
     * @param image
     *            The image to be formatted.
     * @param outStream
     *            The stream to write to.
     * 
     * @throws ServiceException
     *             not really.
     * @throws IOException
     *             if encoding to <code>outStream</code> fails.
     */
    public void formatImageOutputStream(RenderedImage originalImage, OutputStream outStream,
            WMSMapContext mapContext) throws ServiceException, IOException {
        // /////////////////////////////////////////////////////////////////
        //
        // Now the magic
        //
        // /////////////////////////////////////////////////////////////////
        InverseColorMapOp paletteInverter = mapContext.getPaletteInverter();
        RenderedImage renderedImage = super.forceIndexed8Bitmask(originalImage, paletteInverter);
        ImageWorker imageWorker = new ImageWorker(renderedImage);
        imageWorker.writeGIF(outStream, "LZW", 0.75f);
    }
}
