/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;

/**
 * A Font that contains a preview image
 *
 * @author Miles Jordan, Australian Antarctic Division
 */
@SuppressWarnings("serial")
public class PreviewFont implements Serializable {

    /** The width of the preview image */
    public static final int PREVIEW_IMAGE_WIDTH = 450;

    /** The height of the preview image */
    public static final int PREVIEW_IMAGE_HEIGHT = 16;

    /** The preview image */
    private transient BufferedDynamicImageResource previewImage;

    /** The text for the preview image */
    private final String PREVIEW_TEXT =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** The font to be displayed */
    Font font;

    protected PreviewFont(Font font) {
        this.font = font;
    }

    /**
     * Gets the preview image
     *
     * @return a preview image of the font
     */
    public BufferedDynamicImageResource getPreviewImage() {
        if (previewImage == null) {
            previewImage = createPreviewImage();
        }
        return previewImage;
    }

    /**
     * Generates the preview image for this font
     *
     * @return an image resource
     */
    private BufferedDynamicImageResource createPreviewImage() {

        // convert into integer pixels, set the font and turn on antialiasing
        BufferedImage bi =
                new BufferedImage(
                        PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bi.createGraphics();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics2D.setFont(font);

        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int stringHeight = fontMetrics.getAscent();

        // background/foreground colours
        graphics2D.setBackground(Color.WHITE);
        graphics2D.setPaint(Color.BLACK);

        // write the name of the font to the graphic. Use the same rendering method used by the
        // WMS (more convoluted, but the only one that can be actually centered within a halo)
        GlyphVector gv =
                font.createGlyphVector(
                        graphics2D.getFontRenderContext(), PREVIEW_TEXT.toCharArray());
        final AffineTransform at =
                AffineTransform.getTranslateInstance(
                        2, PREVIEW_IMAGE_HEIGHT / 2 + stringHeight / 4);
        Shape sample = at.createTransformedShape(gv.getOutline());
        graphics2D.fill(sample);

        // create the image
        BufferedDynamicImageResource generatedImage = new BufferedDynamicImageResource("png");
        generatedImage.setImage(bi);
        // generatedImage.setCacheable(true);

        return generatedImage;
    }

    /** Returns the font name */
    public String getFontName() {
        return font.getFontName();
    }
}
