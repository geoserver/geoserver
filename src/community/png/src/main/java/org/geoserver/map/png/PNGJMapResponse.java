package org.geoserver.map.png;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.map.png.providers.ScanlineProvider;
import org.geoserver.map.png.providers.ScanlineProviderFactory;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.PNGMapResponse;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.map.quantize.ColorIndexerDescriptor;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class PNGJMapResponse extends RenderedImageMapResponse {
    /** Logger */
    private static final Logger LOGGER = Logging.getLogger(PNGMapResponse.class);

    private static final String MIME_TYPE = "image/png";

    private static final String MIME_TYPE_8BIT = "image/png; mode=8bit";

    private static final String[] OUTPUT_FORMATS = { MIME_TYPE, MIME_TYPE_8BIT, "image/png8" };

    static {
        ColorIndexerDescriptor.register();
    }

    /**
     * Default capabilities for PNG format.
     * 
     * <p>
     * <ol>
     * <li>tiled = supported</li>
     * <li>multipleValues = unsupported</li>
     * <li>paletteSupported = supported</li>
     * <li>transparency = supported</li>
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(true, false,
            true, true, null);

    /**
     * @param format the format name as to be reported in the capabilities document
     * @param wms
     */
    public PNGJMapResponse(WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetMapRequest request = (GetMapRequest) operation.getParameters()[0];
        if (request.getFormat().contains("8")) {
            return MIME_TYPE_8BIT;
        } else {
            return MIME_TYPE;
        }
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     * 
     * @see RasterMapOutputFormat#formatImageOutputStream(RenderedImage, OutputStream)
     */
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream,
            WMSMapContent mapContent) throws ServiceException, IOException {
        // /////////////////////////////////////////////////////////////////
        //
        // Reformatting this image for png
        //
        // /////////////////////////////////////////////////////////////////
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing png image ...");
        }
        
        // we could have a 16 bit paletted image, that PNG cannot handle 
        if(image.getColorModel() instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) image.getColorModel();
            if(icm.getMapSize() > 256) {
                if(LOGGER.isLoggable(Level.FINER)){
                    LOGGER.fine("Forcing input image to be compatible with PNG: Palette with > 256 color is not supported.");
                }
                ImageWorker iw = new ImageWorker(image);
                iw.rescaleToBytes();
                image = iw.getRenderedImage();
            }
        }
        // check to see if we have to see a translucent or bitmask quantizer
        image = applyPalette(image, mapContent, "image/png8", true);

        // compute the compression level similarly to what the Clib code does
        float quality = (100 - wms.getPngCompression()) / 100.0f;
        int level = Math.round(9 * (1f - quality));
        
        writePNG(image, outStream, level);
    }

    public void writePNG(RenderedImage image, OutputStream outStream, int level) {
        ScanlineProvider scanlines = ScanlineProviderFactory.getProvider(image);
        
        // encode using the PNGJ library and the GeoServer own scaline providers
        ColorModel colorModel = image.getColorModel();
        boolean indexed = colorModel instanceof IndexColorModel;
        boolean hasAlpha = colorModel.hasAlpha();
        int numColorComponents = colorModel.getNumColorComponents();
        boolean grayscale = !indexed && numColorComponents < 3;
        byte bitDepth = scanlines.getBitDepth();
        ImageInfo ii = new ImageInfo(image.getWidth(), image.getHeight(), bitDepth, hasAlpha,
                grayscale, indexed);
        PngWriter pw = new PngWriter(outStream, ii);
        pw.setShouldCloseStream(false);
        try {
            pw.setCompLevel(level);
            // TODO: optimize based on the image type, raster or vector
            pw.setFilterType(ar.com.hjg.pngj.FilterType.FILTER_NONE);

            if (indexed) {
                IndexColorModel icm = (IndexColorModel) colorModel;
                PngChunkPLTE palette = pw.getMetadata().createPLTEChunk();
                int ncolors = icm.getMapSize();
                palette.setNentries(ncolors);
                for (int i = 0; i < ncolors; i++) {
                    final int red = icm.getRed(i);
                    final int green = icm.getGreen(i);
                    final int blue = icm.getBlue(i);
                    palette.setEntry(i, red, green, blue);
                }
                if (icm.hasAlpha()) {
                    PngChunkTRNS transparent = new PngChunkTRNS(ii);
                    int[] alpha = new int[ncolors];
                    for (int i = 0; i < ncolors; i++) {
                        final int a = icm.getAlpha(i);
                        alpha[i] = a;
                    }
                    transparent.setPalletteAlpha(alpha);
                    pw.getChunksList().queue(transparent);

                }
            }

            // write out the actual image lines
            for (int row = 0; row < image.getHeight(); row++) {
                pw.writeRow(scanlines);
            }
            pw.end();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to encode the PNG", e);
            throw new ServiceException(e);
        } finally {
            pw.close();
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }
}
