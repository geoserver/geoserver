package org.geoserver.map.png;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Helper painting a random buffered image, to be used for tests
 * 
 * @author Andrea Aime - GeoSolutions
 */
class SampleImagePainter {

    int lines = 200;
    
    int labels = 50;

    int strokeWidth = 30;

    public void paintImage(BufferedImage image) {
        Graphics2D g = image.createGraphics();

        // setup some basic rendering hints, mimicks the output we'd get from GeoServer
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hintsMap.put(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hintsMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHints(hintsMap);

        Random random = new Random(0);
        for (int i = 0; i < lines; i++) {
            int x1 = (int) (random.nextDouble() * image.getWidth());
            int y1 = (int) (random.nextDouble() * image.getHeight());
            int x2 = (int) (random.nextDouble() * image.getWidth());
            int y2 = (int) (random.nextDouble() * image.getHeight());
            int w = (int) (random.nextDouble() * (strokeWidth - 1) + 1);
            g.setStroke(new BasicStroke(w));
            g.setColor(new Color((int) (random.nextDouble() * Integer.MAX_VALUE)));
            g.drawLine(x1, y1, x2, y2);
        }
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        for(int i = 0; i < labels; i++) {
            int x1 = (int) (random.nextDouble() * image.getWidth());
            int y1 = (int) (random.nextDouble() * image.getHeight());
            g.drawString("TestLabel", x1, y1);
        }

        g.dispose();
    }

    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public int getLabels() {
        return labels;
    }

    public void setLabels(int labels) {
        this.labels = labels;
    }
}
