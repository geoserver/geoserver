/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.wms.WMSMapContent;
import org.geotools.renderer.lite.RendererUtilities;

public class ScaleRatioDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER = 
        org.geotools.util.logging.Logging.getLogger("org.geoserver.wms.responses");

    public void loadOptions(Map<String, String> options) {
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent){
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        return new Dimension(metrics.stringWidth(getScaleText(mapContent)), metrics.getHeight());
    }

    public String getScaleText(WMSMapContent mapContent) {
        return String.format(
            "1 : %0$1.0f", 
            RendererUtilities.calculateOGCScale(
                mapContent.getRenderingArea(),
                mapContent.getRequest().getWidth(),
                new HashMap()
            )
        );
    }

    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContent) 
    throws Exception {
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        Dimension d = 
            new Dimension(metrics.stringWidth(getScaleText(mapContent)), metrics.getHeight());
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();

        float x = (float)(paintArea.getMinX() + (paintArea.getWidth() - d.getWidth()) / 2.0); 
        float y = (float)(paintArea.getMaxY() - (paintArea.getHeight() - d.getHeight()) / 2.0);
        Rectangle2D bgRect = new Rectangle2D.Double(
            x - 3.0, y - d.getHeight(), 
            d.getWidth() + 6.0, d.getHeight() + 6.0
        );
        g2d.setColor(Color.WHITE);
        g2d.fill(bgRect);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));

        g2d.drawString(getScaleText(mapContent), x, y);
        g2d.draw(bgRect);

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }
}
