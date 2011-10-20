/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.media.jai.PlanarImage;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.resources.image.ImageUtilities;
import org.springframework.util.Assert;

/**
 * Abstract base class for GetMapProducers that relies in LiteRenderer for creating the raster map
 * and then outputs it in the format they specializes in.
 * 
 * <p>
 * This class does the job of producing a BufferedImage using geotools LiteRenderer, so it should be
 * enough for a subclass to implement {@linkplain #formatImageOutputStream}
 * </p>
 * 
 * <p>
 * Generates a map using the geotools jai rendering classes. Uses the Lite renderer, loading the
 * data on the fly, which is quite nice. Thanks Andrea and Gabriel. The word is that we should
 * eventually switch over to StyledMapRenderer and do some fancy stuff with caching layers, but I
 * think we are a ways off with its maturity to try that yet. So Lite treats us quite well, as it is
 * stateless and therefore loads up nice and fast.
 * </p>
 * 
 * <p>
 * </p>
 * 
 * @author Chris Holmes, TOPP
 * @author Simone Giannecchini, GeoSolutions
 * @version $Id$
 */
public abstract class RenderedImageMapResponse extends AbstractMapResponse {

    /** Which format to encode the image in if one is not supplied */
    private static final String DEFAULT_MAP_FORMAT = "image/png";

    /** WMS Service configuration * */
    protected final WMS wms;

    /**
     * 
     */
    public RenderedImageMapResponse(WMS wms) {
        this(DEFAULT_MAP_FORMAT, wms);
    }

    /**
     * @param the
     *            mime type to be written down as an HTTP header when a map of this format is
     *            generated
     */
    public RenderedImageMapResponse(String mime, WMS wms) {
        super(RenderedImageMap.class, mime);
        this.wms = wms;
    }

    public RenderedImageMapResponse(String[] outputFormats, WMS wms) {
        super(RenderedImageMap.class, outputFormats);
        this.wms = wms;
    }

    /**
     * Transforms a rendered image into the appropriate format, streaming to the output stream.
     * 
     * @param image
     *            The image to be formatted.
     * @param outStream
     *            The stream to write to.
     * 
     * @throws ServiceException
     * @throws IOException
     */
    public abstract void formatImageOutputStream(RenderedImage image, OutputStream outStream,
            WMSMapContext mapContext) throws ServiceException, IOException;

    /**
     * Writes the image to the given destination.
     * 
     * @param value
     *            must be a {@link RenderedImageMap}
     * @see GetMapOutputFormat#write(org.geoserver.wms.WebMap, OutputStream)
     * @see #formatImageOutputStream(RenderedImage, OutputStream, WMSMapContext)
     */
    @Override
    public final void write(final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {

        Assert.isInstanceOf(RenderedImageMap.class, value);

        final RenderedImageMap imageMap = (RenderedImageMap) value;
        try {
            final RenderedImage image = imageMap.getImage();
            final List<GridCoverage2D> renderedCoverages = imageMap.getRenderedCoverages();
            final WMSMapContext mapContext = imageMap.getMapContext();
            try {
                formatImageOutputStream(image, output, mapContext);
                output.flush();
            } finally {
                // let go of the coverages created for rendering
                for (GridCoverage2D coverage : renderedCoverages) {
                    RasterCleaner.addCoverage(coverage);
                }
                RasterCleaner.addImage(image);
            }
        } finally {
            imageMap.dispose();
        }
    }

    /**
     * @param originalImage
     * @return
     */
    protected RenderedImage forceIndexed8Bitmask(RenderedImage originalImage,
            InverseColorMapOp paletteInverter) {
        return ImageUtils.forceIndexed8Bitmask(originalImage, paletteInverter);
    }

    /**
     * Returns the capabilities for this output format 
     * @param outputFormat
     * @return
     */
    public abstract MapProducerCapabilities getCapabilities(String outputFormat);
}
