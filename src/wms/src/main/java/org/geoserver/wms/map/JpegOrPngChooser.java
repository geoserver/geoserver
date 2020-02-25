/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.RenderingHints;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ExtremaDescriptor;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.util.ImageUtilities;

/**
 * A support object attaching itself to the WebMapContent and deciding which format should be used
 * between jpeg and png when using the image/vnd.jpeg-png image format. This is not done in the
 * renderer because when meta-tiling we want to defer the decision about the format to the point in
 * which the single tiles are encoded
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JpegOrPngChooser {

    /** The key used to store the chooser in the map content metadata map */
    static final String JPEG_PNG_CHOOSER = "jpegOrPngChooser";

    public static JpegOrPngChooser getFromMap(RenderedImageMap map) {
        WMSMapContent ctx = map.getMapContext();
        return getFromMapContent(map.getImage(), ctx);
    }

    /** Returns the chooser from the map content, eventually creating it if missing */
    public static JpegOrPngChooser getFromMapContent(RenderedImage image, WMSMapContent ctx) {
        JpegOrPngChooser chooser = (JpegOrPngChooser) ctx.getMetadata().get(JPEG_PNG_CHOOSER);
        if (chooser == null) {
            chooser = new JpegOrPngChooser(image);
            ctx.getMetadata().put(JPEG_PNG_CHOOSER, chooser);
        }
        return chooser;
    }

    boolean jpegPreferred;

    public JpegOrPngChooser(RenderedImage image) {
        this.jpegPreferred = isBestFormatJpeg(image);
    }

    /**
     * Returns the full mime type of the chosen format (<code>image/jpeg</code> or <code>image/png
     * </code>)
     */
    public String getMime() {
        final String mime = jpegPreferred ? "image/jpeg" : "image/png";
        return mime;
    }

    /** Returns the extension of the chosen format (<code>jpeg</code> or <code>png</code>) */
    public String getExtension() {
        String extension = jpegPreferred ? "jpeg" : "png";
        return extension;
    }

    /**
     * Returns true if the best format to encode the image is jpeg (the image is rgb, or rgba
     * without any actual transparency use)
     */
    private boolean isBestFormatJpeg(RenderedImage renderedImage) {
        int numBands = renderedImage.getSampleModel().getNumBands();
        if (numBands == 4 || numBands == 2) {
            RenderingHints renderingHints = ImageUtilities.getRenderingHints(renderedImage);
            RenderedOp extremaOp =
                    ExtremaDescriptor.create(renderedImage, null, 1, 1, false, 1, renderingHints);
            double[][] extrema = (double[][]) extremaOp.getProperty("Extrema");
            double[] mins = extrema[0];

            return mins[mins.length - 1] == 255; // fully opaque
        } else if (renderedImage.getColorModel() instanceof IndexColorModel) {
            // JPEG would still compress a bit better, but in order to figure out
            // if the image has transparency we'd have to expand to RGB or roll
            // a new JAI image op that looks for the transparent pixels. Out of scope for the moment
            return false;
        } else {
            // otherwise support RGB or gray
            return (numBands == 3) || (numBands == 1);
        }
    }

    /** Returns true if the JPEG format was the preferred one */
    public boolean isJpegPreferred() {
        return jpegPreferred;
    }
}
