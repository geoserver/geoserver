/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.util.Map;

class AttributesTextRenderer {

    private Graphics2D graphics2d;
    private Map<String, String> attributes;
    private AttributesGlobeBounds attributesGlobeBounds;
    private AttributeGlobeFonts fonts;

    private double titleX;
    private double valueX;

    private double titleAscend;
    private double valueAscend;

    public AttributesTextRenderer(
            Graphics2D graphics2d,
            Map<String, String> attributes,
            AttributesGlobeBounds attributesGlobeBounds,
            AttributeGlobeFonts fonts) {
        this.graphics2d = graphics2d;
        this.attributes = attributes;
        this.attributesGlobeBounds = attributesGlobeBounds;
        this.fonts = fonts;
        titleX = attributesGlobeBounds.getConfiguration().getMargin();
        valueX = titleX + attributesGlobeBounds.getTextBounds().getTitleWidth();
        titleAscend = graphics2d.getFontMetrics(fonts.getTitleFont()).getAscent();
        valueAscend = graphics2d.getFontMetrics(fonts.getValueFont()).getAscent();
    }

    public void render() {
        double y = 0 + attributesGlobeBounds.getConfiguration().getMargin();
        for (String attrName : attributes.keySet()) {
            y = renderAttribute(attrName, attributes.get(attrName), y);
            y += attributesGlobeBounds.getConfiguration().getInterLineSpace();
        }
    }

    private double renderAttribute(String title, String value, double y) {
        // draw the title
        graphics2d.setFont(fonts.getTitleFont());
        graphics2d.setColor(fonts.getTitleColor());
        graphics2d.drawString(title + ":", (float) titleX, (float) (y + titleAscend));
        // draw the value
        graphics2d.setFont(fonts.getValueFont());
        graphics2d.setColor(fonts.getValueColor());
        graphics2d.drawString(value, (float) valueX, (float) (y + valueAscend));
        // get the text line height and add it to y
        return y + getLineHeight(title, value);
    }

    private double getLineHeight(String title, String value) {
        FontRenderContext context = graphics2d.getFontRenderContext();
        double titleHeight = fonts.getTitleFont().getStringBounds(title, context).getHeight();
        double valueHeight = fonts.getValueFont().getStringBounds(value, context).getHeight();
        return Math.max(titleHeight, valueHeight);
    }
}
