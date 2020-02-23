/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import it.geosolutions.jaiext.colorindexer.CachingColorIndexer;
import it.geosolutions.jaiext.colorindexer.ColorIndexer;
import it.geosolutions.jaiext.colorindexer.LRUColorIndexer;
import it.geosolutions.jaiext.colorindexer.Quantizer;
import java.awt.*;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.kvp.PaletteManager;
import org.geoserver.wms.map.PNGMapResponse.QuantizeMethod;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.ImageWorker;
import org.geotools.image.palette.InverseColorMapOp;
import org.springframework.util.Assert;

/**
 * Abstract base class for GetMapProducers that relies in LiteRenderer for creating the raster map
 * and then outputs it in the format they specializes in.
 *
 * <p>This class does the job of producing a BufferedImage using geotools LiteRenderer, so it should
 * be enough for a subclass to implement {@linkplain #formatImageOutputStream}
 *
 * <p>Generates a map using the geotools jai rendering classes. Uses the Lite renderer, loading the
 * data on the fly, which is quite nice. Thanks Andrea and Gabriel. The word is that we should
 * eventually switch over to StyledMapRenderer and do some fancy stuff with caching layers, but I
 * think we are a ways off with its maturity to try that yet. So Lite treats us quite well, as it is
 * stateless and therefore loads up nice and fast.
 *
 * <p>
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

    /** */
    public RenderedImageMapResponse(WMS wms) {
        this(DEFAULT_MAP_FORMAT, wms);
    }

    /**
     * @param mime the mime type to be written down as an HTTP header when a map of this format is
     *     generated
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
     * @param image The image to be formatted.
     * @param outStream The stream to write to.
     */
    public abstract void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException;

    /**
     * Writes the image to the given destination.
     *
     * @param value must be a {@link RenderedImageMap}
     * @see GetMapOutputFormat#write(org.geoserver.wms.WebMap, OutputStream)
     * @see #formatImageOutputStream(RenderedImage, OutputStream, WMSMapContent)
     */
    @Override
    public final void write(
            final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {

        Assert.isInstanceOf(RenderedImageMap.class, value);

        final RenderedImageMap imageMap = (RenderedImageMap) value;
        try {
            final RenderedImage image = imageMap.getImage();
            final List<GridCoverage2D> renderedCoverages = imageMap.getRenderedCoverages();
            final WMSMapContent mapContent = imageMap.getMapContext();
            try {
                formatImageOutputStream(image, output, mapContent);
                output.flush();
            } finally {
                if (image instanceof RenderedImageTimeDecorator) {
                    ((RenderedImageTimeDecorator) image).getStatistics().renderingComplete();
                }
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
     * Applies a transformation to 8 bits + palette in case the user requested a specific palette or
     * the palette format has been requested, applying a bitmask or translucent palette inverter
     * according to the user request and the image structure
     *
     * @param image The image to be eventually paletted
     * @param mapContent The {@link org.geotools.map.MapContent}
     * @param palettedFormatCheck The function checking if the format requested demands paletted
     *     output
     * @param supportsTranslucency If false the code will always apply the bitmask transformer
     */
    protected RenderedImage applyPalette(
            RenderedImage image,
            WMSMapContent mapContent,
            Function<String, Boolean> palettedFormatCheck,
            boolean supportsTranslucency) {
        // check to see if we have to see a translucent or bitmask quantizer
        GetMapRequest request = mapContent.getRequest();
        QuantizeMethod method =
                (QuantizeMethod) request.getFormatOptions().get(PaletteManager.QUANTIZER);
        boolean useBitmaskQuantizer =
                method == QuantizeMethod.Octree
                        || !supportsTranslucency
                        || (method == null
                                && image.getColorModel().getTransparency()
                                        != Transparency.TRANSLUCENT);

        // format: split on ';' to handle subtypes like 'image/gif;subtype=animated'
        final String format = request.getFormat().split(";")[0];
        // do we have to use the bitmask quantizer?
        IndexColorModel icm = mapContent.getPalette();
        if (useBitmaskQuantizer) {
            // user provided palette?
            if (icm != null) {
                image = forceIndexed8Bitmask(image, PaletteManager.getInverseColorMapOp(icm));
            } else if (palettedFormatCheck.apply(format)) {
                // or format that needs palette to be applied?
                image = forceIndexed8Bitmask(image, null);
            }
        } else {
            if (!(image.getColorModel() instanceof IndexColorModel)) {
                // try to force a RGBA setup
                image =
                        new ImageWorker(image)
                                .rescaleToBytes()
                                .forceComponentColorModel()
                                .getRenderedImage();
                ColorIndexer indexer = null;

                // user provided palette?
                if (mapContent.getPalette() != null) {
                    indexer = new CachingColorIndexer(new LRUColorIndexer(icm, 1024));
                } else if (palettedFormatCheck.apply(format)) {
                    // build the palette and grab the optimized color indexer
                    indexer = new Quantizer(256).subsample().buildColorIndexer(image);
                }

                // if we have an indexer transform the image
                if (indexer != null) {
                    image = new ImageWorker(image).colorIndex(indexer).getRenderedImage();
                }
            }
        }

        return image;
    }

    /**
     * Applies a transformation to 8 bits + palette in case the user requested a specific palette or
     * the palette format has been requested, applying a bitmask or translucent palette inverter
     * according to the user request and the image structure
     *
     * @param image The image to be eventually paletted
     * @param mapContent The {@link org.geotools.map.MapContent}
     * @param palettedFormatName The format name used to require paletted output
     * @param supportsTranslucency If false the code will always apply the bitmask transformer
     */
    protected RenderedImage applyPalette(
            RenderedImage image,
            WMSMapContent mapContent,
            String palettedFormatName,
            boolean supportsTranslucency) {
        return applyPalette(
                image,
                mapContent,
                format -> (palettedFormatName.equalsIgnoreCase(format)),
                supportsTranslucency);
    }

    /** @param originalImage */
    protected RenderedImage forceIndexed8Bitmask(
            RenderedImage originalImage, InverseColorMapOp paletteInverter) {
        return ImageUtils.forceIndexed8Bitmask(originalImage, paletteInverter);
    }

    /** Returns the capabilities for this output format */
    public abstract MapProducerCapabilities getCapabilities(String outputFormat);

    /** Returns a three letter extension for clients needing to name return files */
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        return "img";
    }
}
