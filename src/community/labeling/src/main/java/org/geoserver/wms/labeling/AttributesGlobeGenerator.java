/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import static java.util.Objects.requireNonNull;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;

/** Renders the attributes globes based on the provided attributes map (title, value). */
class AttributesGlobeGenerator {

    private AttributeGlobeFonts fonts;
    private AttributesGlobeConfiguration configuration;
    private Map<String, String> attributes;

    private AttributesGlobeBounds attributesGlobeBounds;
    private BufferedImage image;
    private Graphics2D graphics2d;

    public AttributesGlobeGenerator(
            AttributeGlobeFonts fonts,
            AttributesGlobeConfiguration configuration,
            Map<String, String> attributes) {
        super();
        this.fonts = requireNonNull(fonts);
        this.configuration = requireNonNull(configuration);
        this.attributes = requireNonNull(attributes);
    }

    /** Generates the attributes globe image using the provided configurations and attributes. */
    public BufferedImage generateImage() {
        // get the calculated bounds
        attributesGlobeBounds = buildAttributesBounds(attributes);
        // create the image instance
        image =
                new BufferedImage(
                        (int) Math.ceil(attributesGlobeBounds.getWidth()),
                        (int) Math.ceil(attributesGlobeBounds.getHeight()),
                        BufferedImage.TYPE_INT_ARGB);
        graphics2d = image.createGraphics();
        // add rendering hints
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // draw the background globe
        GlobeRender.renderGlobe(
                graphics2d,
                attributesGlobeBounds.getGlobeBounds(),
                configuration.getTailDimensions());
        // render the attributes text
        AttributesTextRenderer textRenderer =
                new AttributesTextRenderer(graphics2d, attributes, attributesGlobeBounds, fonts);
        textRenderer.render();
        return image;
    }

    public AttributesGlobeBounds getBounds() {
        return attributesGlobeBounds;
    }

    private AttributesGlobeBounds buildAttributesBounds(Map<String, String> attributes) {
        TextBounds textBounds = calculateTextMaxBounds(attributes);
        GlobeBounds globeBounds = calculateGlobeBounds(textBounds);

        return new AttributesGlobeBounds(globeBounds, textBounds, configuration);
    }

    private GlobeBounds calculateGlobeBounds(TextBounds textBounds) {
        double totalMargins = configuration.getMargin() * 2;
        double width = textBounds.getWidth() + totalMargins;
        double height = textBounds.getHeight() + totalMargins;

        return new GlobeBounds(width, height, configuration.getRoundCornerRadius());
    }

    /**
     * Calculates the max bounds will be used by the text.
     *
     * @param attributes the attributes titles and values Map
     * @return the total computed bounds
     */
    private TextBounds calculateTextMaxBounds(Map<String, String> attributes) {
        TextBoundsCalculator calculator =
                new TextBoundsCalculator(fonts, configuration, attributes);
        return calculator.calculateTextMaxBounds();
    }
}
