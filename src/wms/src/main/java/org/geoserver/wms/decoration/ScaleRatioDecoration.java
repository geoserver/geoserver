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

import org.geoserver.wms.WMSMapContext;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;


public class ScaleRatioDecoration implements MapDecoration {

    public void loadOptions(Map<String, String> options) {
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContext mapContext){
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        return new Dimension(metrics.stringWidth(getScaleText(mapContext)), metrics.getHeight());
    }

    public double getScale(WMSMapContext mapContext) {
        double dpi = RendererUtilities.getDpi(mapContext.getRequest().getFormatOptions());
        Map hints = new HashMap();        
        hints.put(StreamingRenderer.DPI_KEY, dpi);
        return RendererUtilities.calculateOGCScale(
                mapContext.getRenderingArea(),
                mapContext.getRequest().getWidth(),
                hints);
    }

    public String getScaleText(WMSMapContext mapContent) {
        return String.format("1 : %0$1.0f", getScale(mapContent));
    }
    
    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContext mapContext) 
    throws Exception {
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        Dimension d = 
            new Dimension(metrics.stringWidth(getScaleText(mapContext)), metrics.getHeight());
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

        g2d.drawString(getScaleText(mapContext), x, y);
        g2d.draw(bgRect);

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }
}
