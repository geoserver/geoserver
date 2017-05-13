/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wms.WMSMapContent;
import org.geotools.util.logging.Logging;

public class ScaleLineDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER =
        Logging.getLogger("org.geoserver.wms.decoration");

    private static Map<String, Double> INCHES_PER_UNIT = new HashMap<String, Double> ();
    static {
        INCHES_PER_UNIT.put("inches", 1.0);
        INCHES_PER_UNIT.put("ft", 12.0);
        INCHES_PER_UNIT.put("mi", 63360.0);
        INCHES_PER_UNIT.put("nmi", 72913.4);
        INCHES_PER_UNIT.put("m", 39.3701);
        INCHES_PER_UNIT.put("km", 39370.1);
        INCHES_PER_UNIT.put("dd", 4374754.0);
        INCHES_PER_UNIT.put("yd", 36.0);
    }

    private String topOutUnit = "km";
    private String topInUnit = "m";
    private String bottomOutUnit = "mi";
    private String bottomInUnit = "ft";

    private float fontSize = 10;
    private float dpi = 25.4f / 0.28f; /// OGC Spec for SLD
    private float strokeWidth = 2;
    private float borderWidth = 1;
    private int suggestedWidth = 100;
    private int padding = 4;

    private Color bgcolor = Color.WHITE;
    private Color fgcolor = Color.BLACK;

    private Boolean transparent = Boolean.FALSE;

    private MeasurementSystem measurementSystem = MeasurementSystem.BOTH;

    private static enum MeasurementSystem {
        METRIC, IMPERIAL, BOTH;

        static MeasurementSystem mapToEnum(String type) throws Exception {
            switch(type){
                case "metric": return METRIC;
                case "imperial": return IMPERIAL;
                case "both": return BOTH;
                default: throw new Exception("Wrong input parameter");
            }
        }
    }

    public void loadOptions(Map<String, String> options) {
        String unit = options.get("top-out-unit");

        if (unit != null) {
            if (INCHES_PER_UNIT.containsKey(unit)) {
                this.topOutUnit = unit;
            } else {
                LOGGER.log(Level.WARNING, "'{0}' is an unknown unit for 'top-out-unit'.");
            }
        }

        unit = options.get("top-in-unit");

        if (unit != null) {
            if (INCHES_PER_UNIT.containsKey(unit)) {
                this.topInUnit = unit;
            } else {
                LOGGER.log(Level.WARNING, "'{0}' is an unknown unit for 'top-in-unit'.");
            }
        }

        unit = options.get("bottom-out-unit");

        if (unit != null) {
            if (INCHES_PER_UNIT.containsKey(unit)) {
                this.bottomOutUnit = unit;
            } else {
                LOGGER.log(Level.WARNING, "'{0}' is an unknown unit for 'bottom-out-unit'.");
            }
        }

        unit = options.get("bottom-in-unit");

        if (unit != null) {
            if (INCHES_PER_UNIT.containsKey(unit)) {
                this.bottomInUnit = unit;
            } else {
                LOGGER.log(Level.WARNING, "'{0}' is an unknown unit for 'bottom-in-unit'.");
            }
        }

        if (options.get("fontsize") != null) {
            try {
                this.fontSize = Float.parseFloat(options.get("fontsize"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'fontsize' must be a float.", e);
            }
        }

        if (options.get("dpi") != null) {
            try {
                this.dpi = Float.parseFloat(options.get("dpi"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'dpi' must be a float.", e);
            }
        }

        if (options.get("strokewidth") != null) {
            try {
                this.strokeWidth = Float.parseFloat(options.get("strokewidth"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'strokewidth' must be a float.", e);
            }
        }

        if (options.get("borderwidth") != null) {
            try {
                this.borderWidth = Float.parseFloat(options.get("borderwidth"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'borderwidth' must be a float.", e);
            }
        }

        if (options.get("suggestedwidth") != null) {
            try {
                this.suggestedWidth = Integer.parseInt(options.get("suggestedwidth"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'suggestedwidth' must be an integer.", e);
            }
        }

        if (options.get("padding") != null) {
            try {
                this.padding = Integer.parseInt(options.get("padding"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'padding' must be an integer.", e);
            }
        }

        Color tmp = MapDecorationLayout.parseColor(options.get("bgcolor"));
        if (tmp != null) bgcolor = tmp;

        tmp = MapDecorationLayout.parseColor(options.get("fgcolor"));
        if (tmp != null) fgcolor = tmp;

        // Creates a rectangle only if is defined, if not is "transparent" like Google Maps
        if (options.get("transparent") != null) {
            try {
                this.transparent = Boolean.parseBoolean(options.get("transparent"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'transparent' must be a boolean.", e);
            }
        }

        if(options.get("measurement-system") != null){
            try {
                this.measurementSystem = MeasurementSystem.mapToEnum(options.get("measurement-system"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'measurement-system' must be one of 'metric', 'imperial' or 'both'.", e);
            }
        }
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent){
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont().deriveFont(this.fontSize));
        return new Dimension(
            suggestedWidth, 2 * padding + (metrics.getHeight() + metrics.getDescent()) * 2
        );
    }

    private int getBarLength(double maxLength) {
        int digits = (int)(Math.log(maxLength) / Math.log(10));
        double pow10 = Math.pow(10, digits);

        // Find first character
        int firstCharacter = (int)(maxLength / pow10);

        int barLength;
        if (firstCharacter > 5) {
            barLength = 5;
        } else if (firstCharacter > 2) {
            barLength = 2;
        } else {
            barLength = 1;
        }

        return (int)(barLength * pow10);
    }

    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContent)
    throws Exception {
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();
        Font oldFont = g2d.getFont();
        Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        // Set the font size.
        g2d.setFont(oldFont.deriveFont(this.fontSize));

        double scaleDenominator = mapContent.getScaleDenominator(true);

        String curMapUnit = "m";

        double normalizedScale = (scaleDenominator > 1.0)
            ? (1.0 / scaleDenominator)
            : scaleDenominator;

        double resolution = 1 / (normalizedScale * INCHES_PER_UNIT.get(curMapUnit) * this.dpi);

        int maxWidth = suggestedWidth;

        if (maxWidth > paintArea.getWidth()) {
            maxWidth = (int)paintArea.getWidth();
        }

        maxWidth = maxWidth - 6;

        double maxSizeData = maxWidth * resolution * INCHES_PER_UNIT.get(curMapUnit);

        String topUnit;
        String bottomUnit;

        if (maxSizeData > 100000) {
            topUnit = topOutUnit;
            bottomUnit = bottomOutUnit;
        } else {
            topUnit = topInUnit;
            bottomUnit = bottomInUnit;
        }

        double topMax = maxSizeData / INCHES_PER_UNIT.get(topUnit);
        double bottomMax = maxSizeData / INCHES_PER_UNIT.get(bottomUnit);

        int topRounded = this.getBarLength(topMax);
        int bottomRounded = this.getBarLength(bottomMax);

        topMax = topRounded / INCHES_PER_UNIT.get(curMapUnit) * INCHES_PER_UNIT.get(topUnit);
        bottomMax = bottomRounded / INCHES_PER_UNIT.get(curMapUnit) * INCHES_PER_UNIT.get(bottomUnit);

        int topPx = (int)(topMax / resolution);
        int bottomPx = (int)(bottomMax / resolution);

        int centerY = (int)paintArea.getCenterY();
        int leftX = (int)paintArea.getMinX() + ((int)paintArea.getWidth() - Math.max(topPx, bottomPx)) / 2;

        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        int prongHeight = metrics.getHeight() + metrics.getDescent();

        //Do not antialias scaleline lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Creates a rectangle only if is defined, if not is "transparent" like Google Maps
        if (!this.transparent) {
            Rectangle frame = new Rectangle(
                leftX - padding, centerY - prongHeight - padding,
                Math.max(topPx, bottomPx) + padding * 2, padding * 2 + prongHeight * 2
            );
            // fill the rectangle
            g2d.setColor(bgcolor);
            g2d.fill(frame);

            // draw the border
            frame.height -= 1;
            frame.width -= 1;
            g2d.setColor(fgcolor);
            g2d.setStroke(new BasicStroke(this.borderWidth));
            g2d.draw(frame);
        } else {
            g2d.setColor(fgcolor);
        }

        g2d.setStroke(new BasicStroke(this.strokeWidth));

        if (measurementSystem == MeasurementSystem.METRIC || measurementSystem == MeasurementSystem.BOTH) {
            // Left vertical top bar
            g2d.drawLine(leftX, centerY, leftX, centerY - prongHeight);

            // Right vertical top bar
            g2d.drawLine(leftX + topPx, centerY, leftX + topPx, centerY - prongHeight);

            // Draw horizontal line for metric
            g2d.drawLine(leftX, centerY, leftX + topPx, centerY);

            //Antialias text if enabled
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);

            // Draw text metric
            String topText = topRounded + " " + topUnit;
            g2d.drawString(topText,
                    leftX + (int)((topPx - metrics.stringWidth(topText)) / 2),
                    centerY - prongHeight + metrics.getAscent()
            );
        }

        if (measurementSystem == MeasurementSystem.IMPERIAL || measurementSystem == MeasurementSystem.BOTH) {
            //Do not antialias scaleline lines
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            //Left vertical bottom bar
            g2d.drawLine(leftX, centerY + prongHeight, leftX, centerY);

            // Right vertical bottom bar
            g2d.drawLine(leftX + bottomPx, centerY, leftX + bottomPx, centerY + prongHeight);

            // Draw horizontal for imperial
            g2d.drawLine(leftX, centerY, leftX + bottomPx, centerY);

            //Antialias text if enabled
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);

            // Draw text imperial
            String bottomText = bottomRounded + " " + bottomUnit;
            g2d.drawString(bottomText,
                    leftX + (int) ((bottomPx - metrics.stringWidth(bottomText)) / 2),
                    centerY + metrics.getHeight()
            );
        }

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
        g2d.setFont(oldFont);
    }
}
