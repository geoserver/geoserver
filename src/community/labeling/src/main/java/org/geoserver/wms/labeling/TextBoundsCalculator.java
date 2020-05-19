/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import static java.util.Objects.requireNonNull;

import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Map.Entry;

/** Helper class, calculates text bounds before start the image rendering. */
class TextBoundsCalculator {

    private AttributeGlobeFonts fonts;
    private AttributesGlobeConfiguration configuration;
    private Map<String, String> attributes;

    protected final FontRenderContext context;

    /**
     * Main constructor.
     *
     * @param fonts the fonts to be used on titles and values
     * @param configuration the main configuration
     * @param attributes the attributes titles and values map
     */
    public TextBoundsCalculator(
            AttributeGlobeFonts fonts,
            AttributesGlobeConfiguration configuration,
            Map<String, String> attributes) {
        this(fonts, configuration, attributes, new FontRenderContext(null, true, true));
    }

    public TextBoundsCalculator(
            AttributeGlobeFonts fonts,
            AttributesGlobeConfiguration configuration,
            Map<String, String> attributes,
            FontRenderContext context) {
        this.fonts = requireNonNull(fonts);
        this.configuration = requireNonNull(configuration);
        this.attributes = requireNonNull(attributes);
        this.context = requireNonNull(context);
    }

    /**
     * Calculates the max bounds will be used by the text.
     *
     * @return the total computed bounds
     */
    public TextBounds calculateTextMaxBounds() {
        double maxTitleWidth = 0d;
        double maxValueWidth = 0d;
        double totalHeight = 0d;
        for (Entry<String, String> entry : attributes.entrySet()) {
            TextLineBounds textBounds = calculateTextLineBounds(entry);
            maxTitleWidth = Math.max(maxTitleWidth, textBounds.getTitleBounds().getWidth());
            maxValueWidth = Math.max(maxValueWidth, textBounds.getValueBounds().getWidth());
            totalHeight +=
                    configuration.getInterLineSpace()
                            + Math.max(
                                    textBounds.getTitleBounds().getHeight(),
                                    textBounds.getValueBounds().getHeight());
        }
        return new TextBounds(maxTitleWidth, maxValueWidth, totalHeight);
    }

    private TextLineBounds calculateTextLineBounds(Entry<String, String> entry) {
        Rectangle2D titleBounds =
                fonts.getTitleFont().getStringBounds(entry.getKey() + ": ", context);
        Rectangle2D valueBounds = fonts.getTitleFont().getStringBounds(entry.getValue(), context);
        return new TextLineBounds(titleBounds, valueBounds);
    }

    private static class TextLineBounds {
        private final Rectangle2D titleBounds;
        private final Rectangle2D valueBounds;

        public TextLineBounds(Rectangle2D titleBounds, Rectangle2D valueBounds) {
            super();
            this.titleBounds = titleBounds;
            this.valueBounds = valueBounds;
        }

        public Rectangle2D getTitleBounds() {
            return titleBounds;
        }

        public Rectangle2D getValueBounds() {
            return valueBounds;
        }
    }
}
